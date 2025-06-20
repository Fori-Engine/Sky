package fori.graphics.vulkan;

import fori.Logger;
import fori.graphics.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_fragment_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_vertex_shader;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanShaderProgram extends ShaderProgram {

    private ShaderBinary vertexShaderBinary, fragmentShaderBinary;
    private long vertexShaderModule, fragmentShaderModule;
    private VkPipelineShaderStageCreateInfo.Buffer shaderStages;

    private ArrayList<ArrayList<Long>> descriptorSetLayouts = new ArrayList<>(VulkanRenderer.FRAMES_IN_FLIGHT);
    private ArrayList<ArrayList<Long>> descriptorSets = new ArrayList<>(VulkanRenderer.FRAMES_IN_FLIGHT);
    private ArrayList<Long> descriptorPools = new ArrayList<>(VulkanRenderer.FRAMES_IN_FLIGHT);


    public VulkanShaderProgram(Ref parent, String vertexShaderSource, String fragmentShaderSource) {
        super(parent, vertexShaderSource, fragmentShaderSource);
        try(MemoryStack stack = MemoryStack.stackPush()){
            ByteBuffer entryPoint = MemoryUtil.memUTF8("main");
            vertexShaderBinary = ShaderCompiler.compile(vertexShaderSource, shaderc_glsl_vertex_shader);
            fragmentShaderBinary = ShaderCompiler.compile(fragmentShaderSource, shaderc_glsl_fragment_shader);

            vertexShaderModule = createShaderModule(VulkanDeviceManager.getCurrentDevice(), vertexShaderBinary.bytecode);
            fragmentShaderModule = createShaderModule(VulkanDeviceManager.getCurrentDevice(), fragmentShaderBinary.bytecode);

            shaderStages = VkPipelineShaderStageCreateInfo.create(2);


            VkPipelineShaderStageCreateInfo vertexShaderStageInfo = shaderStages.get(0);
            vertexShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertexShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
            vertexShaderStageInfo.module(vertexShaderModule);
            vertexShaderStageInfo.pName(entryPoint);

            VkPipelineShaderStageCreateInfo fragmentShaderStageInfo = shaderStages.get(1);
            fragmentShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragmentShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragmentShaderStageInfo.module(fragmentShaderModule);
            fragmentShaderStageInfo.pName(entryPoint);

        }
    }

    public static long createShaderModule(VkDevice device, ByteBuffer spirvCode) {

        try(MemoryStack stack = stackPush()) {

            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode);

            LongBuffer pShaderModule = stack.mallocLong(1);

            if(vkCreateShaderModule(device, createInfo, null, pShaderModule) != VK_SUCCESS) {

                throw new RuntimeException(
                        Logger.error(
                                VulkanShaderProgram.class,
                                "Failed to create shader module"
                        ));

            }

            return pShaderModule.get(0);
        }
    }
    @Override
    public void bind(Attributes.Type[] attributes, ShaderResSet... resourceSets) {
        super.bind(attributes, resourceSets);

        int frameDescriptorCount = 0;

        for(ShaderResSet set : resourceSets){
            frameDescriptorCount += set.getShaderResources().size();
        }

        //Create the pool
        {

            //Create a Descriptor Pool Layout for a frame
            VkDescriptorPoolSize.Buffer descriptorPoolSizes = VkDescriptorPoolSize.create(frameDescriptorCount);

            int poolSizeIndex = 0;
            for (ShaderResSet set : resourceSets) {

                for (int shaderResIndex = 0; shaderResIndex < set.getShaderResources().size(); shaderResIndex++) {
                    ShaderRes res = set.getShaderResources().get(shaderResIndex);

                    VkDescriptorPoolSize poolSize = descriptorPoolSizes.get(poolSizeIndex);

                    int vkDescriptorType = toVkDescriptorType(res.type);

                    poolSize.type(vkDescriptorType);
                    poolSize.descriptorCount(res.count);

                    poolSizeIndex++;
                }
            }

            VkDescriptorPoolCreateInfo descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.create();
            descriptorPoolCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            descriptorPoolCreateInfo.pPoolSizes(descriptorPoolSizes);
            descriptorPoolCreateInfo.maxSets(resourceSets.length);


            for (int i = 0; i < VulkanRenderer.FRAMES_IN_FLIGHT; i++) {
                LongBuffer pDescriptorPool = MemoryUtil.memAllocLong(1);
                if (vkCreateDescriptorPool(VulkanDeviceManager.getCurrentDevice(), descriptorPoolCreateInfo, null, pDescriptorPool) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create descriptor pool");
                }
                descriptorPools.add(i, pDescriptorPool.get(0));
            }

        }

        //Create the sets
        {


            for (ShaderResSet set : resourceSets) {
                VkDescriptorSetLayoutBinding.Buffer descriptorSetLayoutBindings = VkDescriptorSetLayoutBinding.create(set.getShaderResources().size());

                ArrayList<ShaderRes> shaderResources = set.getShaderResources();
                for (int i = 0; i < shaderResources.size(); i++) {
                    ShaderRes res = shaderResources.get(i);

                    VkDescriptorSetLayoutBinding binding = descriptorSetLayoutBindings.get(i);
                    binding.descriptorType(toVkDescriptorType(res.type));
                    binding.stageFlags(toVkShaderStage(res.shaderStage));
                    binding.binding(res.binding);
                    binding.descriptorCount(res.count);
                }

                for (int i = 0; i < VulkanRenderer.FRAMES_IN_FLIGHT; i++) {
                    descriptorSetLayouts.add(new ArrayList<>());
                    descriptorSets.add(new ArrayList<>());




                    LongBuffer pDescriptorSetLayout = MemoryUtil.memAllocLong(1);

                    VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.create();
                    descriptorSetLayoutCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
                    descriptorSetLayoutCreateInfo.pBindings(descriptorSetLayoutBindings);


                    if(vkCreateDescriptorSetLayout(VulkanDeviceManager.getCurrentDevice(), descriptorSetLayoutCreateInfo, null, pDescriptorSetLayout) != VK_SUCCESS){
                        throw new RuntimeException("Failed to create descriptor set layout");
                    }

                    long descriptorSetLayout = pDescriptorSetLayout.get(0);

                    descriptorSetLayouts.get(i).add(descriptorSetLayout);



                    VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.create();
                    descriptorSetAllocateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
                    descriptorSetAllocateInfo.descriptorPool(descriptorPools.get(i));
                    descriptorSetAllocateInfo.pSetLayouts(pDescriptorSetLayout);

                    LongBuffer pDescriptorSet = MemoryUtil.memAllocLong(1);
                    if(vkAllocateDescriptorSets(VulkanDeviceManager.getCurrentDevice(), descriptorSetAllocateInfo, pDescriptorSet) != VK_SUCCESS){
                        throw new RuntimeException("Failed to allocate descriptor set");
                    }
                    long descriptorSet = pDescriptorSet.get(0);

                    descriptorSets.get(i).add(descriptorSet);
                }
            }






        }




    }

    public void updateBuffers(int frameIndex, ShaderUpdate<Buffer>... bufferUpdates){
        try(MemoryStack stack = stackPush()) {

            VkWriteDescriptorSet.Buffer descriptorSetsWrites = VkWriteDescriptorSet.calloc(bufferUpdates.length, stack);


            for (int i = 0; i < bufferUpdates.length; i++) {
                ShaderUpdate<Buffer> bufferUpdate = bufferUpdates[i];
                VulkanBuffer buffer = (VulkanBuffer) bufferUpdate.update;
                VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                bufferInfo.buffer(buffer.getHandle());
                bufferInfo.offset(0);
                bufferInfo.range(buffer.getSizeBytes());

                ShaderRes targetRes = null;

                for(ShaderResSet set : resourcesSets){
                    for(ShaderRes res : set.getShaderResources()){
                        if(set.set == bufferUpdate.set && res.binding == bufferUpdate.binding)
                            targetRes = res;
                    }
                }


                VkWriteDescriptorSet descriptorSetsWrite = descriptorSetsWrites.get(i);

                descriptorSetsWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                int descriptorSetIndex = findShaderResSetFromIndex(bufferUpdate.set);
                long descriptorSet = descriptorSets.get(frameIndex).get(descriptorSetIndex);

                descriptorSetsWrite.dstSet(descriptorSet);
                descriptorSetsWrite.dstBinding(bufferUpdate.binding);
                descriptorSetsWrite.dstArrayElement(0);
                descriptorSetsWrite.descriptorType(toVkDescriptorType(targetRes.type));
                descriptorSetsWrite.pBufferInfo(bufferInfo);
                descriptorSetsWrite.descriptorCount(bufferUpdate.updateCount);
            }

            vkUpdateDescriptorSets(VulkanDeviceManager.getCurrentDevice(), descriptorSetsWrites, null);
        }
    }
    public void updateTextures(int frameIndex, ShaderUpdate<Texture>... textureUpdates){
        try(MemoryStack stack = stackPush()) {


            VkWriteDescriptorSet.Buffer descriptorSetsWrites = VkWriteDescriptorSet.calloc(textureUpdates.length, stack);


            for (int i = 0; i < textureUpdates.length; i++) {
                ShaderUpdate<Texture> textureUpdate = textureUpdates[i];


                ShaderRes targetRes = null;

                for(ShaderResSet set : resourcesSets){
                    for(ShaderRes res : set.getShaderResources()){
                        if(set.set == textureUpdate.set && res.binding == textureUpdate.binding)
                            targetRes = res;
                    }
                }

                VkDescriptorImageInfo.Buffer descriptorImageInfo = VkDescriptorImageInfo.calloc(1, stack);
                descriptorImageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                descriptorImageInfo.imageView(((VulkanTexture) textureUpdate.update).getImageView());
                descriptorImageInfo.sampler(((VulkanTexture) textureUpdate.update).getSampler());


                VkWriteDescriptorSet descriptorSetsWrite = descriptorSetsWrites.get(i);

                descriptorSetsWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                int descriptorSetIndex = findShaderResSetFromIndex(textureUpdate.set);
                long descriptorSet = descriptorSets.get(frameIndex).get(descriptorSetIndex);

                descriptorSetsWrite.dstSet(descriptorSet);
                descriptorSetsWrite.dstBinding(textureUpdate.binding);
                descriptorSetsWrite.dstArrayElement(textureUpdate.arrayIndex);
                descriptorSetsWrite.descriptorType(toVkDescriptorType(targetRes.type));
                descriptorSetsWrite.pImageInfo(descriptorImageInfo);
                descriptorSetsWrite.descriptorCount(textureUpdate.updateCount);
            }

            vkUpdateDescriptorSets(VulkanDeviceManager.getCurrentDevice(), descriptorSetsWrites, null);


        }
    }


    private int findShaderResSetFromIndex(int id) {
        for (int i = 0; i < resourcesSets.length; i++) {
            ShaderResSet resourceSet = resourcesSets[i];
            if (resourceSet.set == id) return i;
        }

        return 0;
    }

    private int toVkDescriptorType(ShaderRes.Type type) {

        switch (type) {
            case UniformBuffer -> {
                return VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
            }
            case ShaderStorageBuffer -> {
                return VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
            }
            case CombinedSampler -> {
                return VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
            }
        }

        return 0;
    }


    private int toVkShaderStage(ShaderRes.ShaderStage stage) {

        switch (stage) {
            case VertexStage -> {
                return VK_SHADER_STAGE_VERTEX_BIT;
            }
            case FragmentStage -> {
                return VK_SHADER_STAGE_FRAGMENT_BIT;
            }
        }

        return 0;
    }

    public long[] getDescriptorSets(int frameIndex) {
        return descriptorSets.get(frameIndex).stream().mapToLong(i -> i).toArray();
    }

    public long[] getDescriptorSetLayouts(int frameIndex){
        return descriptorSetLayouts.get(frameIndex).stream().mapToLong(i -> i).toArray();
    }

    public long[] getAllDescriptorSetLayouts(){
        ArrayList<Long> allDescriptorSetLayouts = new ArrayList<>();

        for(ArrayList<Long> descriptorSetLayouts : descriptorSetLayouts){
            allDescriptorSetLayouts.addAll(descriptorSetLayouts);
        }

        return allDescriptorSetLayouts.stream().mapToLong(i -> i).toArray();
    }

    public VkPipelineShaderStageCreateInfo.Buffer getShaderStages() {
        return shaderStages;
    }

    @Override
    public void dispose() {
        vkDeviceWaitIdle(VulkanDeviceManager.getCurrentDevice());

        vkDestroyShaderModule(VulkanDeviceManager.getCurrentDevice(), vertexShaderModule, null);
        vkDestroyShaderModule(VulkanDeviceManager.getCurrentDevice(), fragmentShaderModule, null);

        for(List<Long> frameDescriptorSetLayouts : descriptorSetLayouts){
            for(long descriptorSetLayouts : frameDescriptorSetLayouts)
                vkDestroyDescriptorSetLayout(VulkanDeviceManager.getCurrentDevice(), descriptorSetLayouts, null);
        }

        for(long descriptorPool : descriptorPools)
            vkDestroyDescriptorPool(VulkanDeviceManager.getCurrentDevice(), descriptorPool, null);




    }

}
