package fori.graphics.vulkan;

import fori.Logger;
import fori.graphics.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_fragment_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_vertex_shader;
import static org.lwjgl.util.vma.Vma.VMA_MEMORY_USAGE_CPU_TO_GPU;
import static org.lwjgl.vulkan.VK10.*;

public class VkShaderProgram extends ShaderProgram {

    private VkDevice device;
    private ShaderBinary vertexShaderBinary, fragmentShaderBinary;
    private long vertexShaderModule, fragmentShaderModule;
    private VkPipelineShaderStageCreateInfo.Buffer shaderStages;

    private ArrayList<ArrayList<Long>> descriptorSetLayouts = new ArrayList<>(VkSceneRenderer.FRAMES_IN_FLIGHT);
    private ArrayList<ArrayList<Long>> descriptorSets = new ArrayList<>(VkSceneRenderer.FRAMES_IN_FLIGHT);
    private ArrayList<Long> descriptorPools = new ArrayList<>(VkSceneRenderer.FRAMES_IN_FLIGHT);







































    public VkShaderProgram(SceneRenderer sceneRenderer, String vertexShaderSource, String fragmentShaderSource) {
        super(vertexShaderSource, fragmentShaderSource);
        this.device = ((VkSceneRenderer) sceneRenderer).getDevice();
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
                                VkShaderProgram.class,
                                "Failed to create shader module"
                        ));

            }

            return pShaderModule.get(0);
        }
    }




    @Override
    public void bind(ShaderResSet... resourceSets) {
        super.bind(resourceSets);

        try(MemoryStack stack = MemoryStack.stackPush()){
            ByteBuffer entryPoint = MemoryUtil.memUTF8("main");
            vertexShaderBinary = ShaderCompiler.compile(vertexShaderSource, shaderc_glsl_vertex_shader);
            fragmentShaderBinary = ShaderCompiler.compile(fragmentShaderSource, shaderc_glsl_fragment_shader);

            vertexShaderModule = createShaderModule(device, vertexShaderBinary.bytecode);
            fragmentShaderModule = createShaderModule(device, fragmentShaderBinary.bytecode);

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


            for (int i = 0; i < VkSceneRenderer.FRAMES_IN_FLIGHT; i++) {
                LongBuffer pDescriptorPool = MemoryUtil.memAllocLong(1);
                if (vkCreateDescriptorPool(device, descriptorPoolCreateInfo, null, pDescriptorPool) != VK_SUCCESS) {
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

                for (int i = 0; i < VkSceneRenderer.FRAMES_IN_FLIGHT; i++) {
                    descriptorSetLayouts.add(new ArrayList<>());
                    descriptorSets.add(new ArrayList<>());




                    LongBuffer pDescriptorSetLayout = MemoryUtil.memAllocLong(1);

                    VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.create();
                    descriptorSetLayoutCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
                    descriptorSetLayoutCreateInfo.pBindings(descriptorSetLayoutBindings);


                    if(vkCreateDescriptorSetLayout(device, descriptorSetLayoutCreateInfo, null, pDescriptorSetLayout) != VK_SUCCESS){
                        throw new RuntimeException("Failed to create descriptor set layout");
                    }

                    long descriptorSetLayout = pDescriptorSetLayout.get(0);

                    descriptorSetLayouts.get(i).add(descriptorSetLayout);



                    VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.create();
                    descriptorSetAllocateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
                    descriptorSetAllocateInfo.descriptorPool(descriptorPools.get(i));
                    descriptorSetAllocateInfo.pSetLayouts(pDescriptorSetLayout);

                    LongBuffer pDescriptorSet = MemoryUtil.memAllocLong(1);
                    if(vkAllocateDescriptorSets(device, descriptorSetAllocateInfo, pDescriptorSet) != VK_SUCCESS){
                        throw new RuntimeException("Failed to allocate descriptor set");
                    }
                    long descriptorSet = pDescriptorSet.get(0);

                    descriptorSets.get(i).add(descriptorSet);
                }
            }






        }




    }

    private int toVkDescriptorType(ShaderRes.Type type) {

        switch (type) {
            case UniformBuffer -> {
                return VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
            }
            case ShaderStorageBuffer -> {
                return VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
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

    }
}
