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


    public VulkanShaderProgram(Disposable parent, TextureFormatType colorTextureFormat, TextureFormatType depthTextureFormat) {
        super(parent, colorTextureFormat, depthTextureFormat);
        entryPoint = MemoryUtil.memUTF8("main");
    }

    public VulkanShaderProgram(Disposable parent) {
        super(parent);
        entryPoint = MemoryUtil.memUTF8("main");
    }

    @Override
    public void setShaders(Shader... shaders) {
        this.shaders = shaders;
        shaderStages = VkPipelineShaderStageCreateInfo.calloc(shaders.length);

        for (int i = 0; i < shaders.length; i++) {
            VulkanShader vulkanShader = (VulkanShader) shaders[i];

            VkPipelineShaderStageCreateInfo vertexShaderStageInfo = shaderStages.get(i);
            vertexShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertexShaderStageInfo.stage(vulkanShader.getStage());
            vertexShaderStageInfo.module(vulkanShader.getModule());
            vertexShaderStageInfo.pName(entryPoint);
        }

    }

    private int toVkTextureFormatEnum(TextureFormatType textureFormatType) {
        switch (textureFormatType) {
            case ColorR8G8B8A8StandardRGB -> {
                return VK_FORMAT_R8G8B8A8_SRGB;
            }
            case Depth32Float -> {
                return VK_FORMAT_D32_SFLOAT;
            }
        }
        return -1;
    }

    private VulkanPipeline createGraphicsPipeline(VkDevice device) {


        long pipelineLayout;
        long graphicsPipeline = 0;

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
                for(VertexAttributes.Type attribute : attributes){
                    vertexSize += attribute.size;
                }


                VkVertexInputBindingDescription.Buffer bindingDescription =
                        VkVertexInputBindingDescription.calloc(1, stack);


                bindingDescription.binding(0);
                bindingDescription.stride(vertexSize * Float.BYTES);
                bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
                vertexInputInfo.pVertexBindingDescriptions(bindingDescription);



                VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                        VkVertexInputAttributeDescription.calloc(attributes.length, stack);

                int offset = 0;

                for (int i = 0; i < attributes.length; i++) {
                    VkVertexInputAttributeDescription attribute = attributeDescriptions.get(i);
                    attribute.binding(0);
                    attribute.location(i);
                    attribute.format(attributeSizeToVulkanParam.apply(attributes[i].size));
                    attribute.offset(offset);

                    offset += attributes[i].size * Float.BYTES;
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

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            {
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


            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            {
                colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
                colorBlending.logicOpEnable(false);
                colorBlending.logicOp(VK_LOGIC_OP_COPY);
                colorBlending.pAttachments(colorBlendAttachment);
                colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));
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



            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            {
                pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
                pipelineLayoutInfo.setLayoutCount(getShaderResSets().length);
                pipelineLayoutInfo.pSetLayouts(stack.longs(getAllDescriptorSetLayouts()));


            }
            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            pipelineLayout = pPipelineLayout.get(0);

            VkPipelineRenderingCreateInfoKHR pipelineRenderingCreateInfoKHR = VkPipelineRenderingCreateInfoKHR.calloc(stack);
            pipelineRenderingCreateInfoKHR.colorAttachmentCount(1);
            pipelineRenderingCreateInfoKHR.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO_KHR);
            pipelineRenderingCreateInfoKHR.pColorAttachmentFormats(stack.ints(toVkTextureFormatEnum(colorTextureFormat)));
            pipelineRenderingCreateInfoKHR.depthAttachmentFormat(toVkTextureFormatEnum(depthTextureFormat));



            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            {
                pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
                pipelineInfo.layout(pipelineLayout);
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


            graphicsPipeline = pGraphicsPipeline.get(0);


        }

        return new VulkanPipeline(this, pipelineLayout, graphicsPipeline);
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
    public void bind(VertexAttributes.Type[] attributes, ShaderResSet... resourceSets) {
        super.bind(attributes, resourceSets);

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
                    if (vkCreateDescriptorPool(VulkanDeviceManager.getCurrentDevice(), descriptorPoolCreateInfo, null, pDescriptorPool) != VK_SUCCESS) {
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


                        if (vkCreateDescriptorSetLayout(VulkanDeviceManager.getCurrentDevice(), descriptorSetLayoutCreateInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                            throw new RuntimeException("Failed to create descriptor set layout");
                        }

                        long descriptorSetLayout = pDescriptorSetLayout.get(0);

                        descriptorSetLayouts.get(i).add(descriptorSetLayout);


                        VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.calloc(stack);
                        descriptorSetAllocateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
                        descriptorSetAllocateInfo.descriptorPool(descriptorPools.get(i));
                        descriptorSetAllocateInfo.pSetLayouts(pDescriptorSetLayout);

                        LongBuffer pDescriptorSet = stack.callocLong(1);
                        if (vkAllocateDescriptorSets(VulkanDeviceManager.getCurrentDevice(), descriptorSetAllocateInfo, pDescriptorSet) != VK_SUCCESS) {
                            throw new RuntimeException("Failed to allocate descriptor set");
                        }
                        long descriptorSet = pDescriptorSet.get(0);

                        descriptorSets.get(i).add(descriptorSet);
                    }
                }


            }
        }

        pipeline = createGraphicsPipeline(VulkanDeviceManager.getCurrentDevice());



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



        MemoryUtil.memFree(entryPoint);
        shaderStages.free();

        for(List<Long> frameDescriptorSetLayouts : descriptorSetLayouts){
            for(long descriptorSetLayouts : frameDescriptorSetLayouts)
                vkDestroyDescriptorSetLayout(VulkanDeviceManager.getCurrentDevice(), descriptorSetLayouts, null);
        }

        for(long descriptorPool : descriptorPools)
            vkDestroyDescriptorPool(VulkanDeviceManager.getCurrentDevice(), descriptorPool, null);


    }

}
