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
import java.util.Optional;
import java.util.function.Function;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanShaderProgram extends ShaderProgram {

    private VkPipelineShaderStageCreateInfo.Buffer shaderStages;

    private ArrayList<ArrayList<Long>> descriptorSetLayouts = new ArrayList<>(VulkanRenderer.FRAMES_IN_FLIGHT);
    private ArrayList<ArrayList<Long>> descriptorSets = new ArrayList<>(VulkanRenderer.FRAMES_IN_FLIGHT);
    private ArrayList<Long> descriptorPools = new ArrayList<>(VulkanRenderer.FRAMES_IN_FLIGHT);
    private ByteBuffer entryPoint;
    private VulkanPipeline pipeline;


    public VulkanShaderProgram(Disposable parent, ShaderProgramType type) {
        super(parent, type);
        entryPoint = MemoryUtil.memUTF8("main");
    }

    @Override
    public void addShader(ShaderType shaderType, Shader shader) {
        shaderMap.put(shaderType, shader);
    }




    private VulkanPipeline createComputePipeline(VkDevice device) {
        createStages();

        Shader computeShader = shaderMap.get(ShaderType.Compute);

        long pipelineLayoutHandle;
        long computePipelineHandle = 0;

        try(MemoryStack stack = stackPush()) {

            VkPushConstantRange.Buffer pushConstantRanges = VkPushConstantRange.calloc(1, stack);
            {
                VkPushConstantRange shaderModePushConstantRange = pushConstantRanges.get(0);
                {
                    shaderModePushConstantRange.offset(0);
                    shaderModePushConstantRange.size(Integer.BYTES);
                    shaderModePushConstantRange.stageFlags(VK_SHADER_STAGE_ALL);
                }
            }


            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            {
                pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
                pipelineLayoutInfo.setLayoutCount(getShaderResSets().length);
                pipelineLayoutInfo.pSetLayouts(stack.longs(getAllDescriptorSetLayouts()));
                pipelineLayoutInfo.pPushConstantRanges(pushConstantRanges);
            }
            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);
            if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }
            pipelineLayoutHandle = pPipelineLayout.get(0);


            VkComputePipelineCreateInfo.Buffer pipelineInfo = VkComputePipelineCreateInfo.calloc(1, stack);
            {
                pipelineInfo.sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO);
                pipelineInfo.layout(pipelineLayoutHandle);
                pipelineInfo.stage(shaderStages.get(0));


            }


            LongBuffer pComputePipeline = stack.mallocLong(1);


            if(vkCreateComputePipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pComputePipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }


            computePipelineHandle = pComputePipeline.get(0);


        }

        return new VulkanPipeline(this, pipelineLayoutHandle, computePipelineHandle);
    }
    private VulkanPipeline createGraphicsPipeline(VkDevice device) {
        createStages();

        Shader vertexShader = shaderMap.get(ShaderType.Vertex);
        Shader fragmentShader = shaderMap.get(ShaderType.Fragment);




        long pipelineLayoutHandle;
        long graphicsPipelineHandle = 0;

        try(MemoryStack stack = stackPush()) {

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            {

                Function<Integer, Integer> attributeSizeToVulkanParam = attributeSize -> {
                    switch(attributeSize){
                        case 1: return VK_FORMAT_R32_SFLOAT;
                        case 2: return VK_FORMAT_R32G32_SFLOAT;
                        case 3: return VK_FORMAT_R32G32B32_SFLOAT;
                        case 4: return VK_FORMAT_R32G32B32A32_SFLOAT;
                    }
                    return 0;
                };

                int vertexSize = 0;
                for(VertexAttributes.Type attribute : vertexShader.getVertexAttributes()){
                    vertexSize += attribute.size;
                }


                VkVertexInputBindingDescription.Buffer bindingDescription =
                        VkVertexInputBindingDescription.calloc(1, stack);


                bindingDescription.binding(0);
                bindingDescription.stride(vertexSize * Float.BYTES);
                bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
                vertexInputInfo.pVertexBindingDescriptions(bindingDescription);



                VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                        VkVertexInputAttributeDescription.calloc(vertexShader.getVertexAttributes().length, stack);

                int offset = 0;

                for (int i = 0; i < vertexShader.getVertexAttributes().length; i++) {
                    VkVertexInputAttributeDescription attribute = attributeDescriptions.get(i);
                    attribute.binding(0);
                    attribute.location(i);
                    attribute.format(attributeSizeToVulkanParam.apply(vertexShader.getVertexAttributes()[i].size));
                    attribute.offset(offset);

                    offset += vertexShader.getVertexAttributes()[i].size * Float.BYTES;
                }




                vertexInputInfo.pVertexAttributeDescriptions(attributeDescriptions.rewind());



            }

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            {
                inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
                inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
                inputAssembly.primitiveRestartEnable(false);
            }

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            {
                viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
                viewportState.viewportCount(1);
                viewportState.scissorCount(1);
            }

            VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack);
            {
                dynamicState.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
                dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));
            }

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            {
                rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
                rasterizer.depthClampEnable(false);
                rasterizer.rasterizerDiscardEnable(false);

                rasterizer.lineWidth(1);
                rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
                rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
                rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
                rasterizer.depthBiasEnable(false);
            }

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            {
                multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
                multisampling.sampleShadingEnable(false);
                multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);
            }

            int attachmentCount = fragmentShader.getAttachmentTextureFormatTypes().length;

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachments = VkPipelineColorBlendAttachmentState.calloc(attachmentCount, stack);
            {
                for(VkPipelineColorBlendAttachmentState colorBlendAttachment : colorBlendAttachments) {

                    colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
                    colorBlendAttachment.blendEnable(false);
                    colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
                    colorBlendAttachment.blendEnable(true);
                    colorBlendAttachment.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
                    colorBlendAttachment.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
                    colorBlendAttachment.colorBlendOp(VK_BLEND_OP_ADD);
                    colorBlendAttachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
                    colorBlendAttachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
                    colorBlendAttachment.alphaBlendOp(VK_BLEND_OP_ADD);
                }

            }


            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            {
                colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
                colorBlending.logicOpEnable(false);
                colorBlending.logicOp(VK_LOGIC_OP_COPY);
                colorBlending.pAttachments(colorBlendAttachments);
                colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));
                colorBlending.attachmentCount(attachmentCount);
            }

            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            {
                depthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
                depthStencil.depthTestEnable(true);
                depthStencil.depthWriteEnable(true);
                depthStencil.depthCompareOp(toVkDepthCompareOpEnum(DepthTestType.LessThan));
                depthStencil.minDepthBounds(0f);
                depthStencil.maxDepthBounds(1f);


                depthStencil.stencilTestEnable(false);


            }

            VkPushConstantRange.Buffer pushConstantRanges = VkPushConstantRange.calloc(1, stack);
            {
                VkPushConstantRange shaderModePushConstantRange = pushConstantRanges.get(0);
                {
                    shaderModePushConstantRange.offset(0);
                    shaderModePushConstantRange.size(Integer.BYTES);
                    shaderModePushConstantRange.stageFlags(VK_SHADER_STAGE_ALL);
                }
            }



            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            {
                pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
                pipelineLayoutInfo.setLayoutCount(getShaderResSets().length);
                pipelineLayoutInfo.pSetLayouts(stack.longs(getAllDescriptorSetLayouts()));
                pipelineLayoutInfo.pPushConstantRanges(pushConstantRanges);


            }
            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            pipelineLayoutHandle = pPipelineLayout.get(0);

            VkPipelineRenderingCreateInfoKHR pipelineRenderingCreateInfoKHR = VkPipelineRenderingCreateInfoKHR.calloc(stack);
            pipelineRenderingCreateInfoKHR.colorAttachmentCount(fragmentShader.getAttachmentTextureFormatTypes().length);
            pipelineRenderingCreateInfoKHR.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO_KHR);

            int[] vkImageFormatEnums = new int[fragmentShader.getAttachmentTextureFormatTypes().length];
            for (int i = 0; i < vkImageFormatEnums.length; i++) {
                vkImageFormatEnums[i] = VulkanUtil.toVkImageFormatEnum(fragmentShader.getAttachmentTextureFormatTypes()[i]);
            }

            pipelineRenderingCreateInfoKHR.pColorAttachmentFormats(stack.ints(vkImageFormatEnums));



            pipelineRenderingCreateInfoKHR.depthAttachmentFormat(VulkanUtil.toVkImageFormatEnum(fragmentShader.getDepthAttachmentTextureFormatType()));



            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            {
                pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
                pipelineInfo.layout(pipelineLayoutHandle);
                pipelineInfo.pInputAssemblyState(inputAssembly);
                pipelineInfo.pRasterizationState(rasterizer);
                pipelineInfo.pColorBlendState(colorBlending);
                pipelineInfo.pMultisampleState(multisampling);
                pipelineInfo.pViewportState(viewportState);
                pipelineInfo.pDepthStencilState(depthStencil);
                pipelineInfo.pDynamicState(dynamicState);
                pipelineInfo.stageCount(getShaderStages().capacity());
                pipelineInfo.pStages(getShaderStages());
                pipelineInfo.pVertexInputState(vertexInputInfo);
                pipelineInfo.pNext(pipelineRenderingCreateInfoKHR);
            }


            LongBuffer pGraphicsPipeline = stack.mallocLong(1);


            if(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }


            graphicsPipelineHandle = pGraphicsPipeline.get(0);


        }

        return new VulkanPipeline(this, pipelineLayoutHandle, graphicsPipelineHandle);
    }

    private void createStages() {
        shaderStages = VkPipelineShaderStageCreateInfo.calloc(shaderMap.size());

        int shaderIndex = 0;
        for(Shader shader : shaderMap.values()) {
            VulkanShader vulkanShader = (VulkanShader) shader;

            VkPipelineShaderStageCreateInfo stageInfo = shaderStages.get(shaderIndex);
            stageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            stageInfo.stage(vulkanShader.getStage());
            stageInfo.module(vulkanShader.getModule());
            stageInfo.pName(entryPoint);

            shaderIndex++;
        }
    }


    private int toVkDepthCompareOpEnum(DepthTestType depthTestType) {
        if(depthTestType == null) return -1;
        switch(depthTestType) {
            case LessThan -> {
                return VK_COMPARE_OP_LESS;
            }
            case GreaterThan -> {
                return VK_COMPARE_OP_GREATER;
            }
            case LessOrEqualTo -> {
                return VK_COMPARE_OP_LESS_OR_EQUAL;
            }
            case GreaterOrEqualTo -> {
                return VK_COMPARE_OP_GREATER_OR_EQUAL;
            }
            case Always -> {
                return VK_COMPARE_OP_ALWAYS;
            }
            case Never -> {
                return VK_COMPARE_OP_NEVER;
            }
        }

        throw new RuntimeException(Logger.error(VulkanRenderer.class, "The depth operation for this pipeline is an invalid value [" + depthTestType + "]"));
    }


    public VulkanPipeline getPipeline() {
        return pipeline;
    }

    @Override
    public void bind(ShaderResSet... resourceSets) {
        this.resourcesSets = resourceSets;

        try(MemoryStack stack = stackPush()) {

            int frameDescriptorCount = 0;

            for (ShaderResSet set : resourceSets) {
                frameDescriptorCount += set.getShaderResources().size();
            }

            //Create the pool
            {

                //Create a Descriptor Pool Layout for a frame
                VkDescriptorPoolSize.Buffer descriptorPoolSizes = VkDescriptorPoolSize.calloc(frameDescriptorCount, stack);

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

                VkDescriptorPoolCreateInfo descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.calloc(stack);
                descriptorPoolCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
                descriptorPoolCreateInfo.pPoolSizes(descriptorPoolSizes);
                descriptorPoolCreateInfo.maxSets(resourceSets.length);


                for (int i = 0; i < VulkanRenderer.FRAMES_IN_FLIGHT; i++) {
                    LongBuffer pDescriptorPool = stack.callocLong(1);
                    if (vkCreateDescriptorPool(VulkanRuntime.getCurrentDevice(), descriptorPoolCreateInfo, null, pDescriptorPool) != VK_SUCCESS) {
                        throw new RuntimeException("Failed to create descriptor pool");
                    }
                    descriptorPools.add(i, pDescriptorPool.get(0));
                }

            }

            //Create the sets
            {


                for (ShaderResSet set : resourceSets) {
                    VkDescriptorSetLayoutBinding.Buffer descriptorSetLayoutBindings = VkDescriptorSetLayoutBinding.calloc(set.getShaderResources().size(), stack);

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


                        LongBuffer pDescriptorSetLayout = stack.callocLong(1);

                        VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
                        descriptorSetLayoutCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
                        descriptorSetLayoutCreateInfo.pBindings(descriptorSetLayoutBindings);


                        if (vkCreateDescriptorSetLayout(VulkanRuntime.getCurrentDevice(), descriptorSetLayoutCreateInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                            throw new RuntimeException("Failed to create descriptor set layout");
                        }

                        long descriptorSetLayout = pDescriptorSetLayout.get(0);

                        descriptorSetLayouts.get(i).add(descriptorSetLayout);


                        VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.calloc(stack);
                        descriptorSetAllocateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
                        descriptorSetAllocateInfo.descriptorPool(descriptorPools.get(i));
                        descriptorSetAllocateInfo.pSetLayouts(pDescriptorSetLayout);

                        LongBuffer pDescriptorSet = stack.callocLong(1);
                        if (vkAllocateDescriptorSets(VulkanRuntime.getCurrentDevice(), descriptorSetAllocateInfo, pDescriptorSet) != VK_SUCCESS) {
                            throw new RuntimeException("Failed to allocate descriptor set");
                        }
                        long descriptorSet = pDescriptorSet.get(0);

                        descriptorSets.get(i).add(descriptorSet);
                    }
                }


            }
        }

        if(type == ShaderProgramType.Graphics) pipeline = createGraphicsPipeline(VulkanRuntime.getCurrentDevice());
        else if(type == ShaderProgramType.Compute) pipeline = createComputePipeline(VulkanRuntime.getCurrentDevice());
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

            vkUpdateDescriptorSets(VulkanRuntime.getCurrentDevice(), descriptorSetsWrites, null);
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

                descriptorImageInfo.imageLayout(textureUpdate.update.isStorageTexture() ? VK_IMAGE_LAYOUT_GENERAL : VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                descriptorImageInfo.imageView(((VulkanTexture) textureUpdate.update).getImageView().getHandle());
                descriptorImageInfo.sampler(((VulkanTexture) textureUpdate.update).getSampler().getHandle());


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

            vkUpdateDescriptorSets(VulkanRuntime.getCurrentDevice(), descriptorSetsWrites, null);

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
            case StorageImage -> {
                return VK_DESCRIPTOR_TYPE_STORAGE_IMAGE;
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
            case ComputeStage -> {
                return VK_SHADER_STAGE_COMPUTE_BIT;
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
        vkDeviceWaitIdle(VulkanRuntime.getCurrentDevice());



        MemoryUtil.memFree(entryPoint);
        shaderStages.free();

        for(List<Long> frameDescriptorSetLayouts : descriptorSetLayouts){
            for(long descriptorSetLayouts : frameDescriptorSetLayouts)
                vkDestroyDescriptorSetLayout(VulkanRuntime.getCurrentDevice(), descriptorSetLayouts, null);
        }

        for(long descriptorPool : descriptorPools)
            vkDestroyDescriptorPool(VulkanRuntime.getCurrentDevice(), descriptorPool, null);


    }

}
