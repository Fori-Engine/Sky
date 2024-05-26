package lake.graphics.vulkan;

import lake.graphics.ShaderProgram;
import lake.graphics.Disposer;
import lake.graphics.ShaderResource;
import lake.graphics.Texture2D;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import static lake.graphics.vulkan.VulkanRenderer2D.MAX_FRAMES_IN_FLIGHT;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.shaderc.Shaderc.*;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;

public class VulkanShaderProgram extends ShaderProgram {

    private VkPipelineShaderStageCreateInfo.Buffer shaderStages;
    private VkDevice device;
    private LVKSpir5Binary vertShaderSPIRV;
    private LVKSpir5Binary fragShaderSPIRV;
    private ByteBuffer entryPoint;
    private long vertShaderModule;
    private long fragShaderModule;
    private LongBuffer descriptorSetLayout;
    private long descriptorPool;
    private List<Long> descriptorSets;
    private LongBuffer pDescriptorSets;
    private HashMap<ShaderResource, VulkanBuffer>[] frameUniformBuffers = new HashMap[MAX_FRAMES_IN_FLIGHT];


    public VulkanShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        super(vertexShaderSource, fragmentShaderSource);
        Disposer.add("managedResources", this);

        for (int i = 0; i < frameUniformBuffers.length; i++) {
            frameUniformBuffers[i] = new HashMap<>();
        }
    }

    @Override
    public void addResource(ShaderResource resource) {
        super.addResource(resource);

        for(HashMap<ShaderResource, VulkanBuffer> buffer : frameUniformBuffers) {


            if (resource.type == ShaderResource.Type.UniformBuffer) {

                try (MemoryStack stack = stackPush()) {

                    LongBuffer pMemoryBuffer = stack.mallocLong(1);
                    VulkanBuffer uniformsBuffer = VulkanUtil.createBuffer(
                            VulkanRenderer2D.getDeviceWithIndices().device,
                            VulkanRenderer2D.getPhysicalDevice(),
                            resource.sizeBytes,
                            VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                            pMemoryBuffer
                    );

                    buffer.put(resource, uniformsBuffer);
                }
            }




        }

    }

    @Override
    public ByteBuffer[] mapUniformBuffer(ShaderResource resource) {

        ByteBuffer[] byteBuffers = new ByteBuffer[MAX_FRAMES_IN_FLIGHT];
        for (int i = 0; i < byteBuffers.length; i++) {
            PointerBuffer data = MemoryUtil.memAllocPointer(1);
            byteBuffers[i] = frameUniformBuffers[i].get(resource).mapAndGet(VulkanRenderer2D.getDeviceWithIndices().device, data);
        }

        return byteBuffers;
    }

    @Override
    public void unmapUniformBuffer(ShaderResource resource, ByteBuffer[] byteBuffers) {
        for(int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++){
            frameUniformBuffers[i].get(resource).unmap(VulkanRenderer2D.getDeviceWithIndices().device);
        }
    }

    @Override
    public void updateEntireSampler2DArrayWithOnly(ShaderResource resource, Texture2D tex) {
        VulkanTexture2D texture = (VulkanTexture2D) tex;

        try(MemoryStack stack = stackPush()) {

            VkDescriptorImageInfo.Buffer imageInfos = VkDescriptorImageInfo.calloc(resource.count);

            for (int i = 0; i < resource.count; i++) {

                VkDescriptorImageInfo imageInfo = imageInfos.get(i);

                imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                imageInfo.imageView(texture.getTextureImageView());
                imageInfo.sampler(texture.getSampler().getTextureSampler());

            }

            //Set Textures, this should go into another method
            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {

                long descriptorSet = pDescriptorSets.get(i);


                VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(1, stack);

                VkWriteDescriptorSet descriptorWrite1 = descriptorWrites.get(0);
                descriptorWrite1.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                descriptorWrite1.dstBinding(resource.binding);
                descriptorWrite1.dstArrayElement(0);
                descriptorWrite1.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                descriptorWrite1.descriptorCount(resource.count);
                descriptorWrite1.pImageInfo(imageInfos);


                for (VkWriteDescriptorSet descriptorWrite : descriptorWrites) {
                    descriptorWrite.dstSet(descriptorSet);
                }

                vkUpdateDescriptorSets(VulkanRenderer2D.getDeviceWithIndices().device, descriptorWrites, null);
            }
        }
    }

    @Override
    public void updateSampler2DArray(ShaderResource resource, int index, Texture2D tex) {
        VulkanTexture2D vulkanTexture2D = (VulkanTexture2D) tex;

        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {

            long descriptorSet = pDescriptorSets.get(i);

            try (MemoryStack stack = stackPush()) {

                VkDescriptorImageInfo.Buffer newImageInfos = VkDescriptorImageInfo.calloc(1);

                VkDescriptorImageInfo imageInfo = newImageInfos.get(0);

                imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                imageInfo.imageView(vulkanTexture2D.getTextureImageView());
                imageInfo.sampler(vulkanTexture2D.getSampler().getTextureSampler());


                VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(1, stack);

                VkWriteDescriptorSet descriptorWrite1 = descriptorWrites.get(0);
                descriptorWrite1.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                descriptorWrite1.dstSet(descriptorSet);
                descriptorWrite1.dstBinding(resource.binding);
                descriptorWrite1.dstArrayElement(index);
                descriptorWrite1.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                descriptorWrite1.descriptorCount(1);
                descriptorWrite1.pImageInfo(newImageInfos);


                vkUpdateDescriptorSets(VulkanRenderer2D.getDeviceWithIndices().device, descriptorWrites, null);
            }
        }
    }

    private int toStageFlags(ShaderResource.ShaderStage shaderStage){
        if(shaderStage == ShaderResource.ShaderStage.VertexStage) return VK_SHADER_STAGE_VERTEX_BIT;
        if(shaderStage == ShaderResource.ShaderStage.FragmentStage) return VK_SHADER_STAGE_FRAGMENT_BIT;

        return 0;
    }

    private int toDescriptorType(ShaderResource shaderResource){
        if(shaderResource.type == ShaderResource.Type.UniformBuffer) return VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
        if(shaderResource.type == ShaderResource.Type.CombinedSampler) return VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;

        return 0;
    }



    public void createDescriptors(){




        try(MemoryStack stack = stackPush()){
            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(resources.size(), stack);
            for (int i = 0; i < resources.size(); i++) {
                ShaderResource shaderResource = resources.get(i);

                VkDescriptorSetLayoutBinding binding = bindings.get(i);

                binding.binding(shaderResource.binding);
                binding.descriptorCount(shaderResource.count);
                binding.descriptorType(toDescriptorType(shaderResource));
                binding.pImmutableSamplers(null);
                binding.stageFlags(toStageFlags(shaderResource.shaderStage));
            }

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(bindings);

            LongBuffer pDescriptorSetLayout = MemoryUtil.memAllocLong(1);

            if(vkCreateDescriptorSetLayout(VulkanRenderer2D.getDeviceWithIndices().device, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }
            descriptorSetLayout = pDescriptorSetLayout;

            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(resources.size(), stack);

            for (int i = 0; i < resources.size(); i++) {
                ShaderResource shaderResource = resources.get(i);

                VkDescriptorPoolSize poolSize = poolSizes.get(i);
                poolSize.type(toDescriptorType(shaderResource));
                poolSize.descriptorCount(MAX_FRAMES_IN_FLIGHT * shaderResource.count);
            }


            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(MAX_FRAMES_IN_FLIGHT);

            LongBuffer pDescriptorPool = stack.mallocLong(1);

            if(vkCreateDescriptorPool(VulkanRenderer2D.getDeviceWithIndices().device, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }

            descriptorPool = pDescriptorPool.get(0);
        }












        //Descriptor Set stuff
        {


            try(MemoryStack stack = stackPush()) {

                LongBuffer layouts = stack.mallocLong(MAX_FRAMES_IN_FLIGHT);
                for(int i = 0; i < layouts.capacity();i++) {
                    layouts.put(i, descriptorSetLayout.get(0));
                }

                VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
                allocInfo.descriptorPool(descriptorPool);
                allocInfo.pSetLayouts(layouts);


                pDescriptorSets = MemoryUtil.memAllocLong(MAX_FRAMES_IN_FLIGHT);

                if(vkAllocateDescriptorSets(VulkanRenderer2D.getDeviceWithIndices().device, allocInfo, pDescriptorSets) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate descriptor sets");
                }
                descriptorSets = new ArrayList<>(pDescriptorSets.capacity());

                //Bind Memory
                for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                    HashMap<ShaderResource, VulkanBuffer> buffers = frameUniformBuffers[i];
                    long descriptorSet = pDescriptorSets.get(i);

                    for(ShaderResource shaderResource : buffers.keySet()) {

                        if(shaderResource.type == ShaderResource.Type.UniformBuffer) {

                            VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                            bufferInfo.offset(0);
                            bufferInfo.range(shaderResource.sizeBytes);
                            bufferInfo.buffer(buffers.get(shaderResource).handle/*frameUniformBuffers[i].get(LVKRenderer2D.modelViewProj).handle*/);

                            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(1, stack);

                            VkWriteDescriptorSet descriptorWrite0 = descriptorWrites.get(0);
                            descriptorWrite0.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                            descriptorWrite0.dstBinding(shaderResource.binding);
                            descriptorWrite0.dstArrayElement(0);
                            descriptorWrite0.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                            descriptorWrite0.descriptorCount(shaderResource.count);
                            descriptorWrite0.pBufferInfo(bufferInfo);


                            for (VkWriteDescriptorSet descriptorWrite : descriptorWrites) {
                                descriptorWrite.dstSet(descriptorSet);
                            }

                            vkUpdateDescriptorSets(VulkanRenderer2D.getDeviceWithIndices().device, descriptorWrites, null);
                        }
                    }




                    descriptorSets.add(descriptorSet);
                }
            }
        }
    }

    public VkDevice getDevice() {
        return device;
    }

    public void setDevice(VkDevice device) {
        this.device = device;
    }

    public VkPipelineShaderStageCreateInfo.Buffer getShaderStages() {
        return shaderStages;
    }

    @Override
    public void prepare() {
        setDevice(VulkanRenderer2D.getDeviceWithIndices().device);
        entryPoint = MemoryUtil.memUTF8("main");


        vertShaderSPIRV = compileShaderToSPIRV(getVertexShaderSource(), shaderc_glsl_vertex_shader);
        fragShaderSPIRV = compileShaderToSPIRV(getFragmentShaderSource(), shaderc_glsl_fragment_shader);

        vertShaderModule = VulkanUtil.createShaderModule(device, vertShaderSPIRV.bytecode);
        fragShaderModule = VulkanUtil.createShaderModule(device, fragShaderSPIRV.bytecode);



        shaderStages = VkPipelineShaderStageCreateInfo.create(2);

        VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);

        vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
        vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
        vertShaderStageInfo.module(vertShaderModule);
        vertShaderStageInfo.pName(entryPoint);

        VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);

        fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
        fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
        fragShaderStageInfo.module(fragShaderModule);
        fragShaderStageInfo.pName(entryPoint);
    }

    public LongBuffer getDescriptorSetLayout() {
        return descriptorSetLayout;
    }

    public long getDescriptorPool() {
        return descriptorPool;
    }

    public List<Long> getDescriptorSets() {
        return descriptorSets;
    }

    public static LVKSpir5Binary compileShaderToSPIRV(String source, int kind){
        long compiler = shaderc_compiler_initialize();
        if(compiler == MemoryUtil.NULL){
            throw new RuntimeException("Failed to init shaderc compiler");
        }

        long result = shaderc_compile_into_spv(compiler, source, kind, "", "main", MemoryUtil.NULL);
        if(result == MemoryUtil.NULL){
            throw new RuntimeException("Shader compilation failed for " + kind);
        }
        if(shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success){

            String shaderType = getShaderType(kind);



            throw new RuntimeException(shaderType + ": " + shaderc_result_get_error_message(result));
        }

        shaderc_compiler_release(compiler);


        return new LVKSpir5Binary(result, shaderc_result_get_bytes(result));
    }

    private static String getShaderType(int kind) {
        if(kind == shaderc_glsl_vertex_shader) return "Vertex Shader";
        if(kind == shaderc_glsl_fragment_shader) return "Fragment Shader";


        return null;
    }

    public static class LVKSpir5Binary {
        public long handle;
        public ByteBuffer bytecode;

        public LVKSpir5Binary(long handle, ByteBuffer bytecode) {
            this.handle = handle;
            this.bytecode = bytecode;
        }

        public void cleanup() {
            shaderc_result_release(handle);
        }
    }

















    @Override
    public void bind() {

    }

    @Override
    public void setFloat(String name, float value) {

    }

    @Override
    public void setInt(String name, int value) {

    }

    @Override
    public void setMatrix4f(String name, Matrix4f proj) {

    }

    @Override
    public void setIntArray(String name, int[] array) {

    }

    @Override
    public void setVector2fArray(String name, float[] array) {

    }

    public void disposeShaderModules(){

        vkDestroyShaderModule(device, vertShaderModule, null);
        vkDestroyShaderModule(device, fragShaderModule, null);

        vertShaderSPIRV.cleanup();
        fragShaderSPIRV.cleanup();
    }

    @Override
    public void dispose() {
        disposeShaderModules();
        for(HashMap<ShaderResource, VulkanBuffer> frameUniformBuffer : frameUniformBuffers){
            for(VulkanBuffer buffer : frameUniformBuffer.values()){
                vkDestroyBuffer(device, buffer.handle, null);
                vkFreeMemory(device, buffer.pMemory, null);
            }
        }



        MemoryUtil.memFree(entryPoint);
    }
}
