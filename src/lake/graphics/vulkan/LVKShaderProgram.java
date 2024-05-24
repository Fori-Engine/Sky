package lake.graphics.vulkan;

import lake.asset.AssetPacks;
import lake.graphics.ShaderProgram;
import lake.graphics.Disposer;
import lake.graphics.ShaderResource;
import lake.graphics.Texture2D;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import static lake.graphics.vulkan.LVKRenderer2D.MAX_FRAMES_IN_FLIGHT;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.shaderc.Shaderc.*;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;

public class LVKShaderProgram extends ShaderProgram {

    private VkPipelineShaderStageCreateInfo.Buffer shaderStages;
    private VkDevice device;
    private LVKSpir5Binary vertShaderSPIRV;
    private LVKSpir5Binary fragShaderSPIRV;

    private ByteBuffer entryPoint;

    private long vertShaderModule;
    private long fragShaderModule;
    private VkDescriptorImageInfo.Buffer imageInfos;

    private LongBuffer descriptorSetLayout;
    private long descriptorPool;
    private List<Long> descriptorSets;


    private HashMap<ShaderResource, LVKGenericBuffer>[] frameUniformBuffers = new HashMap[MAX_FRAMES_IN_FLIGHT];





    public LVKShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        super(vertexShaderSource, fragmentShaderSource);
        Disposer.add("managedResources", this);

        for (int i = 0; i < frameUniformBuffers.length; i++) {
            frameUniformBuffers[i] = new HashMap<>();
        }

    }

    @Override
    public void addResource(ShaderResource resource) {
        super.addResource(resource);

        for(HashMap<ShaderResource, LVKGenericBuffer> buffer : frameUniformBuffers) {


            if (resource.type == ShaderResource.Type.UniformBuffer) {

                try (MemoryStack stack = stackPush()) {

                    LongBuffer pMemoryBuffer = stack.mallocLong(1);
                    LVKGenericBuffer uniformsBuffer = FastVK.createBuffer(
                            LVKRenderer2D.getDeviceWithIndices().device,
                            LVKRenderer2D.getPhysicalDevice(),
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
            byteBuffers[i] = frameUniformBuffers[i].get(resource).mapAndGet(LVKRenderer2D.getDeviceWithIndices().device, data);
        }

        return byteBuffers;
    }

    @Override
    public void unmapUniformBuffer(ShaderResource resource, ByteBuffer[] byteBuffers) {
        for(int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++){
            frameUniformBuffers[i].get(resource).unmap(LVKRenderer2D.getDeviceWithIndices().device);
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



    public void createDescriptors(LVKRenderSync renderSyncInfo){



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

            if(vkCreateDescriptorSetLayout(LVKRenderer2D.getDeviceWithIndices().device, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
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

            if(vkCreateDescriptorPool(LVKRenderer2D.getDeviceWithIndices().device, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
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


                LongBuffer pDescriptorSets = stack.mallocLong(MAX_FRAMES_IN_FLIGHT);

                if(vkAllocateDescriptorSets(LVKRenderer2D.getDeviceWithIndices().device, allocInfo, pDescriptorSets) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate descriptor sets");
                }


                descriptorSets = new ArrayList<>(pDescriptorSets.capacity());


                imageInfos = VkDescriptorImageInfo.create(32);


                {
                    LVKTexture2D emptyTexture = (LVKTexture2D) Texture2D.newTexture2D(AssetPacks.getAsset("core:assets/empty.png"), Texture2D.Filter.Nearest);

                    for (int i = 0; i < 32; i++) {

                        VkDescriptorImageInfo imageInfo = imageInfos.get(i);

                        imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                        imageInfo.imageView(emptyTexture.getTextureImageView());
                        imageInfo.sampler(emptyTexture.getSampler().getTextureSampler());

                    }

                }

                List<LVKRenderFrame> inFlightFrames = renderSyncInfo.inFlightFrames;

                for (int i = 0; i < inFlightFrames.size(); i++) {

                    long descriptorSet = pDescriptorSets.get(i);


                    VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                    bufferInfo.offset(0);
                    bufferInfo.range(LVKRenderFrame.LVKFrameUniforms.TOTAL_SIZE_BYTES);
                    bufferInfo.buffer(frameUniformBuffers[i].get(LVKRenderer2D.modelViewProj).handle);

                    VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(2, stack);

                    VkWriteDescriptorSet descriptorWrite0 = descriptorWrites.get(0);
                    descriptorWrite0.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                    descriptorWrite0.dstBinding(0);
                    descriptorWrite0.dstArrayElement(0);
                    descriptorWrite0.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                    descriptorWrite0.descriptorCount(1);
                    descriptorWrite0.pBufferInfo(bufferInfo);



                    VkWriteDescriptorSet descriptorWrite1 = descriptorWrites.get(1);
                    descriptorWrite1.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                    descriptorWrite1.dstBinding(1);
                    descriptorWrite1.dstArrayElement(0);
                    descriptorWrite1.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                    descriptorWrite1.descriptorCount(32);
                    descriptorWrite1.pImageInfo(imageInfos);


                    for(VkWriteDescriptorSet descriptorWrite : descriptorWrites){
                        descriptorWrite.dstSet(descriptorSet);
                    }



                    vkUpdateDescriptorSets(LVKRenderer2D.getDeviceWithIndices().device, descriptorWrites, null);
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
        setDevice(LVKRenderer2D.getDeviceWithIndices().device);
        entryPoint = MemoryUtil.memUTF8("main");


        vertShaderSPIRV = compileShaderToSPIRV(getVertexShaderSource(), shaderc_glsl_vertex_shader);
        fragShaderSPIRV = compileShaderToSPIRV(getFragmentShaderSource(), shaderc_glsl_fragment_shader);

        vertShaderModule = FastVK.createShaderModule(device, vertShaderSPIRV.bytecode);
        fragShaderModule = FastVK.createShaderModule(device, fragShaderSPIRV.bytecode);



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
        MemoryUtil.memFree(entryPoint);
    }
}
