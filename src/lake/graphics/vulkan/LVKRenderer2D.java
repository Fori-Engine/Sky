package lake.graphics.vulkan;

import lake.FileReader;
import lake.FlightRecorder;
import lake.asset.AssetPacks;
import lake.asset.TextureData;
import lake.graphics.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK13.*;

public class LVKRenderer2D extends Renderer2D {
    private int MAX_FRAMES_IN_FLIGHT = 2;
    long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
    private VkQueue graphicsQueue, presentQueue;
    private static VkDeviceWithIndices deviceWithIndices;
    private static VkPhysicalDevice physicalDevice;
    private VkInstance instance;
    private long surface;
    private LVKSwapchain swapchain;
    private List<Long> swapchainImageViews;
    private LVKPipeline currentPipeline;
    private long renderPass;
    private List<Long> swapchainFramebuffers;
    private long commandPool;
    private LVKVertexBuffer vertexBuffer;
    private LVKIndexBuffer indexBuffer;
    private List<VkCommandBuffer> commandBuffers;
    private LVKRenderSync renderSyncInfo;
    private int currentFrame;
    private LongBuffer descriptorSetLayout;
    private long descriptorPool;
    private List<Long> descriptorSets;

    private ByteBuffer vertexBufferData, indexBufferData;
    private LVKShaderProgram currentShaderProgram, defaultShaderProgram;
    private int maxTextures = 32;

    private FastTextureLookup textureLookup;
    private int nextTextureSlot;
    private VkDescriptorImageInfo.Buffer imageInfos;
    private VkPhysicalDeviceProperties physicalDeviceProperties;
    private Map<ShaderProgram, LVKPipeline> pipelineCache = new HashMap<>();
    public LVKRenderer2D(Window window, int width, int height, RenderSettings settings) {
        super(width, height, settings);


        String appEngineInfoName = "LakeEngine";

        FlightRecorder.info(LVKRenderer2D.class, "Using appInfoEngineName of " + appEngineInfoName);
        instance = FastVK.createInstance(getClass().getName(), appEngineInfoName, settings.enableValidation);
        FastVK.setupDebugMessenger(instance, settings.enableValidation);
        surface = FastVK.createSurface(instance, window);
        physicalDevice = FastVK.pickPhysicalDevice(instance, surface);
        deviceWithIndices = FastVK.createLogicalDevice(physicalDevice, settings.enableValidation, surface);
        graphicsQueue = FastVK.getGraphicsQueue(deviceWithIndices);
        presentQueue = FastVK.getPresentQueue(deviceWithIndices);
        swapchain = FastVK.createSwapChain(physicalDevice, deviceWithIndices.device, surface, width, height);
        swapchainImageViews = FastVK.createImageViews(deviceWithIndices.device, swapchain);




        physicalDeviceProperties = FastVK.getPhysicalDeviceProperties(physicalDevice);



        renderPass = FastVK.createRenderPass(deviceWithIndices.device, swapchain);
        swapchainFramebuffers = FastVK.createFramebuffers(deviceWithIndices.device, swapchain, swapchainImageViews, renderPass);
        commandPool = FastVK.createCommandPool(deviceWithIndices);
        textureLookup = new FastTextureLookup(maxTextures);
        LVKCommandRunner.setup(deviceWithIndices, graphicsQueue);

















        vertexBuffer = new LVKVertexBuffer(
                settings.quadsPerBatch,
                10,
                deviceWithIndices.device,
                commandPool,
                graphicsQueue,
                physicalDevice);

        indexBuffer = new LVKIndexBuffer(
                settings.quadsPerBatch,
                6,
                Integer.BYTES,
                deviceWithIndices.device,
                commandPool,
                graphicsQueue,
                physicalDevice);


        vertexBufferData = vertexBuffer.getMainBuffer().mapAndGet(deviceWithIndices.device, vertexBuffer.getMappingBuffer());
        indexBufferData = indexBuffer.getMainBuffer().mapAndGet(deviceWithIndices.device, indexBuffer.getMappingBuffer());


        renderSyncInfo = new LVKRenderSync();
        renderSyncInfo.inFlightFrames = new ArrayList<>(MAX_FRAMES_IN_FLIGHT);
        renderSyncInfo.imagesInFlight = new HashMap<>(MAX_FRAMES_IN_FLIGHT);



        try(MemoryStack stack = stackPush()) {

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
            LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
            LongBuffer pFence = stack.mallocLong(1);

            for(int i = 0;i < MAX_FRAMES_IN_FLIGHT;i++) {

                if(vkCreateSemaphore(deviceWithIndices.device, semaphoreInfo, null, pImageAvailableSemaphore) != VK_SUCCESS
                        || vkCreateSemaphore(deviceWithIndices.device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS
                        || vkCreateFence(deviceWithIndices.device, fenceInfo, null, pFence) != VK_SUCCESS) {

                    throw new RuntimeException("Failed to create synchronization objects for the frame " + i);
                }

                renderSyncInfo.inFlightFrames.add(new LVKRenderFrame(pImageAvailableSemaphore.get(0), pRenderFinishedSemaphore.get(0), pFence.get(0)));
            }

            for(LVKRenderFrame frame : renderSyncInfo.inFlightFrames){
                LongBuffer pMemoryBuffer = stack.mallocLong(1);
                LVKGenericBuffer uniformsBuffer = FastVK.createBuffer(deviceWithIndices.device, physicalDevice, LVKRenderFrame.LVKFrameUniforms.TOTAL_SIZE_BYTES, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, pMemoryBuffer);
                LVKRenderFrame.LVKFrameUniforms uniforms = new LVKRenderFrame.LVKFrameUniforms(uniformsBuffer, pMemoryBuffer.get());
                frame.setUniforms(uniforms);
            }

        }




        //Descriptor Set Layout stuff
        {
            try(MemoryStack stack = stackPush()) {

                VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(2, stack);

                VkDescriptorSetLayoutBinding uboLayoutBinding = bindings.get(0);

                uboLayoutBinding.binding(0);
                uboLayoutBinding.descriptorCount(1);
                uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboLayoutBinding.pImmutableSamplers(null);
                uboLayoutBinding.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);


                VkDescriptorSetLayoutBinding samplerLayoutBinding = bindings.get(1);

                samplerLayoutBinding.binding(1);
                samplerLayoutBinding.descriptorCount(maxTextures);
                samplerLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                samplerLayoutBinding.pImmutableSamplers(null);
                samplerLayoutBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

                VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
                layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
                layoutInfo.pBindings(bindings);

                LongBuffer pDescriptorSetLayout = MemoryUtil.memAllocLong(1);

                if(vkCreateDescriptorSetLayout(deviceWithIndices.device, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create descriptor set layout");
                }
                descriptorSetLayout = pDescriptorSetLayout;
            }

        }

        //Descriptor Pool stuff
        {
            try(MemoryStack stack = stackPush()) {

                VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(2, stack);


                VkDescriptorPoolSize poolSize0 = poolSizes.get(0);
                poolSize0.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                poolSize0.descriptorCount(MAX_FRAMES_IN_FLIGHT);

                VkDescriptorPoolSize poolSize1 = poolSizes.get(1);
                poolSize1.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                poolSize1.descriptorCount(MAX_FRAMES_IN_FLIGHT * maxTextures);

                VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
                poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
                poolInfo.pPoolSizes(poolSizes);
                poolInfo.maxSets(MAX_FRAMES_IN_FLIGHT);

                LongBuffer pDescriptorPool = stack.mallocLong(1);

                if(vkCreateDescriptorPool(deviceWithIndices.device, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create descriptor pool");
                }

                descriptorPool = pDescriptorPool.get(0);
            }
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

                if(vkAllocateDescriptorSets(deviceWithIndices.device, allocInfo, pDescriptorSets) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate descriptor sets");
                }






                descriptorSets = new ArrayList<>(pDescriptorSets.capacity());








                imageInfos = VkDescriptorImageInfo.create(maxTextures);


                {
                    LVKTexture2D emptyTexture = (LVKTexture2D) Texture2D.newTexture2D(AssetPacks.getAsset("core:assets/empty.png"), Texture2D.Filter.Nearest);

                    for (int i = 0; i < maxTextures; i++) {

                        VkDescriptorImageInfo imageInfo = imageInfos.get(i);

                        imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                        imageInfo.imageView(emptyTexture.getTextureImageView());
                        imageInfo.sampler(emptyTexture.getSampler().getTextureSampler());

                    }

                }















                VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                bufferInfo.offset(0);

                bufferInfo.range(LVKRenderFrame.LVKFrameUniforms.TOTAL_SIZE_BYTES);

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
                descriptorWrite1.descriptorCount(maxTextures);
                descriptorWrite1.pImageInfo(imageInfos);




                List<LVKRenderFrame> inFlightFrames = renderSyncInfo.inFlightFrames;


                for (int i = 0; i < inFlightFrames.size(); i++) {
                    LVKRenderFrame frame = inFlightFrames.get(i);
                    long descriptorSet = pDescriptorSets.get(i);

                    for(VkWriteDescriptorSet descriptorWrite : descriptorWrites){
                        descriptorWrite.dstSet(descriptorSet);
                    }


                    bufferInfo.buffer(frame.getUniforms().getBuffer().handle);
                    vkUpdateDescriptorSets(deviceWithIndices.device, descriptorWrites, null);
                    descriptorSets.add(descriptorSet);
                }








            }


        }

        ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                AssetPacks.<String> getAsset("core:assets/shaders/vulkan/Default.glsl").asset
        );


        LVKShaderProgram shaderProgram = new LVKShaderProgram(
                shaderSources.vertexShader,
                shaderSources.fragmentShader
        );

        shaderProgram.setDevice(deviceWithIndices.device);
        shaderProgram.prepare();

        currentPipeline = createPipeline(deviceWithIndices.device, swapchain, shaderProgram.getShaderStages(), renderPass, descriptorSetLayout);
        pipelineCache.put(shaderProgram, currentPipeline);

        currentShaderProgram = shaderProgram;
        defaultShaderProgram = shaderProgram;

        proj = new Matrix4f().ortho(0, getWidth(), 0, getHeight(), 0, 1, true);
        updateMatrices();



        final int commandBuffersCount = swapchainFramebuffers.size();

        commandBuffers = new ArrayList<>(commandBuffersCount);

        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.create();
        allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
        allocInfo.commandPool(commandPool);
        allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
        allocInfo.commandBufferCount(commandBuffersCount);

        PointerBuffer pCommandBuffers = MemoryUtil.memAllocPointer(commandBuffersCount);

        if(vkAllocateCommandBuffers(deviceWithIndices.device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate command buffers");
        }

        for(int i = 0;i < commandBuffersCount;i++) {
            commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), deviceWithIndices.device));
        }







    }
    private void recordCmdBuffers(int indexCount) {

        final int commandBuffersCount = swapchainFramebuffers.size();



        try(MemoryStack stack = stackPush()) {



            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
            renderPassInfo.renderPass(renderPass);
            VkRect2D renderArea = VkRect2D.calloc(stack);
            renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
            renderArea.extent(swapchain.swapChainExtent);
            renderPassInfo.renderArea(renderArea);
            VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
            clearValues.color().float32(stack.floats(clearColor.r, clearColor.g, clearColor.b, clearColor.a));
            renderPassInfo.pClearValues(clearValues);


            for(int i = 0; i < commandBuffersCount;i++) {

                VkCommandBuffer commandBuffer = commandBuffers.get(i);

                if(vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to begin recording command buffer");
                }

                renderPassInfo.framebuffer(swapchainFramebuffers.get(i));


                vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
                {

                    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, currentPipeline.pipeline);



                    LongBuffer vertexBuffers = stack.longs(vertexBuffer.getMainBuffer().handle);


                    LongBuffer offsets = stack.longs(0);
                    vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
                    vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getMainBuffer().handle, 0, VK_INDEX_TYPE_UINT32);

                    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                            currentPipeline.pipelineLayout, 0, stack.longs(descriptorSets.get(i)), null);
                    vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, 0);



                }
                vkCmdEndRenderPass(commandBuffer);


                if(vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to record command buffer");
                }

            }

        }

    }
    private LVKPipeline createPipeline(VkDevice device, LVKSwapchain swapchain, VkPipelineShaderStageCreateInfo.Buffer shaderStages, long renderPass, LongBuffer descriptorSetLayout){

        FlightRecorder.info(LVKRenderer2D.class, "Creating new pipeline...");

        long pipelineLayout;
        long graphicsPipeline = 0;

        try(MemoryStack stack = stackPush()) {
            // ===> VERTEX STAGE <===

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);


            VkVertexInputBindingDescription.Buffer bindingDescription =
                    VkVertexInputBindingDescription.calloc(1, stack);

            bindingDescription.binding(0);
            bindingDescription.stride(vertexBuffer.getVertexDataSize() * Float.BYTES);
            bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
            vertexInputInfo.pVertexBindingDescriptions(bindingDescription);









            VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                    VkVertexInputAttributeDescription.calloc(5);

            // Position
            VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(0);
            {
                posDescription.binding(0);
                posDescription.location(0);
                posDescription.format(VK_FORMAT_R32G32_SFLOAT);
                posDescription.offset(0);
            }

            //Texture Coordinates
            VkVertexInputAttributeDescription texCoordDescription = attributeDescriptions.get(1);
            {
                texCoordDescription.binding(0);
                texCoordDescription.location(1);
                texCoordDescription.format(VK_FORMAT_R32G32_SFLOAT);
                texCoordDescription.offset(2 * Float.BYTES);
            }

            //Texture Index
            VkVertexInputAttributeDescription texIndexDescription = attributeDescriptions.get(2);
            {
                texIndexDescription.binding(0);
                texIndexDescription.location(2);
                texIndexDescription.format(VK_FORMAT_R32_SFLOAT);
                texIndexDescription.offset(4 * Float.BYTES);


            }

            // Color
            VkVertexInputAttributeDescription colorDescription = attributeDescriptions.get(3);
            {
                colorDescription.binding(0);
                colorDescription.location(3);
                colorDescription.format(VK_FORMAT_R32G32B32A32_SFLOAT);
                colorDescription.offset(5 * Float.BYTES);
            }

            // Thickness
            VkVertexInputAttributeDescription thicknessDescription = attributeDescriptions.get(4);
            {
                thicknessDescription.binding(0);
                thicknessDescription.location(4);
                thicknessDescription.format(VK_FORMAT_R32_SFLOAT);
                thicknessDescription.offset(9 * Float.BYTES);
            }





            vertexInputInfo.pVertexAttributeDescriptions(attributeDescriptions.rewind());

            // ===> ASSEMBLY STAGE <===

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            {
                inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
                inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
                inputAssembly.primitiveRestartEnable(false);
            }
            // ===> VIEWPORT & SCISSOR

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            {
                viewport.x(0.0f);
                viewport.y(0.0f);
                viewport.width(swapchain.swapChainExtent.width());
                viewport.height(swapchain.swapChainExtent.height());
                viewport.minDepth(0.0f);
                viewport.maxDepth(1.0f);
            }

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            {
                scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
                scissor.extent(swapchain.swapChainExtent);
            }
            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            {
                viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
                viewportState.pViewports(viewport);
                viewportState.pScissors(scissor);
            }
            // ===> RASTERIZATION STAGE <===

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            {
                rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
                rasterizer.depthClampEnable(false);
                rasterizer.rasterizerDiscardEnable(false);
                rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
                rasterizer.lineWidth(1.0f);
                rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
                rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
                rasterizer.depthBiasEnable(false);
            }
            // ===> MULTISAMPLING <===

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            {
                multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
                multisampling.sampleShadingEnable(false);
                multisampling.rasterizationSamples(1);
            }
            // ===> COLOR BLENDING <===

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            {
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

            // ===> PIPELINE LAYOUT CREATION <===

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            {
                pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
                pipelineLayoutInfo.setLayoutCount(1);
                pipelineLayoutInfo.pSetLayouts(descriptorSetLayout);
            }
            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            pipelineLayout = pPipelineLayout.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            {
                pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
                pipelineInfo.pStages(shaderStages);
                pipelineInfo.pVertexInputState(vertexInputInfo);
                pipelineInfo.pInputAssemblyState(inputAssembly);
                pipelineInfo.pViewportState(viewportState);
                pipelineInfo.pRasterizationState(rasterizer);
                pipelineInfo.pMultisampleState(multisampling);
                pipelineInfo.pColorBlendState(colorBlending);
                pipelineInfo.layout(pipelineLayout);
                pipelineInfo.renderPass(renderPass);
                pipelineInfo.subpass(0);
                pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
                pipelineInfo.basePipelineIndex(-1);
            }

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);


            if(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }


            graphicsPipeline = pGraphicsPipeline.get(0);


        }

        return new LVKPipeline(pipelineLayout, graphicsPipeline);
    }
    @Override
    public void setShaderProgram(ShaderProgram shaderProgram) {




        if(pipelineCache.containsKey(shaderProgram)) {
            currentPipeline = pipelineCache.get(shaderProgram);
        }
        else {

            LVKShaderProgram lvkShaderProgram = (LVKShaderProgram) shaderProgram;

            LVKPipeline newPipeline = createPipeline(
                    deviceWithIndices.device,
                    swapchain,
                    lvkShaderProgram.getShaderStages(),
                    renderPass,
                    descriptorSetLayout);

            lvkShaderProgram.setDevice(deviceWithIndices.device);
            pipelineCache.put(shaderProgram, newPipeline);
            currentPipeline = newPipeline;
        }




    }
    @Override
    public void updateMatrices() {

        for(LVKRenderFrame thisFrame : renderSyncInfo.inFlightFrames) {


            try (MemoryStack stack = stackPush()) {
                PointerBuffer data = stack.mallocPointer(1);

                ByteBuffer buffer = thisFrame.getUniforms().getBuffer().mapAndGet(deviceWithIndices.device, data);

                model.get(buffer);
                camera.getViewMatrix().get(LVKRenderFrame.LVKFrameUniforms.MATRIX_SIZE_BYTES, buffer);
                proj.get(2 * LVKRenderFrame.LVKFrameUniforms.MATRIX_SIZE_BYTES, buffer);


                thisFrame.getUniforms().getBuffer().unmap(deviceWithIndices.device);
            }
        }

    }

    @Override
    public ShaderProgram getDefaultShaderProgram() {
        return defaultShaderProgram;
    }
    @Override
    public ShaderProgram getCurrentShaderProgram() {
        return currentShaderProgram;
    }


    @Override
    public void drawTexture(float x, float y, float w, float h, Texture2D texture, Color color, Rect2D rect2D, boolean xFlip, boolean yFlip) {

        LVKTexture2D lvkTexture2D = (LVKTexture2D) texture;

        int slot = nextTextureSlot;
        boolean isUniqueTexture = false;



        //Existing texture
        if (textureLookup.hasTexture(lvkTexture2D)) {
            slot = textureLookup.getTexture(lvkTexture2D);
        }

        //Unique Texture
        else {


            try(MemoryStack stack = stackPush()){

                VkDescriptorImageInfo.Buffer newImageInfos = VkDescriptorImageInfo.calloc(1);

                VkDescriptorImageInfo imageInfo = newImageInfos.get(0);

                imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                imageInfo.imageView(lvkTexture2D.getTextureImageView());
                imageInfo.sampler(lvkTexture2D.getSampler().getTextureSampler());



                VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(1, stack);

                VkWriteDescriptorSet descriptorWrite1 = descriptorWrites.get(0);
                descriptorWrite1.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                descriptorWrite1.dstSet(descriptorSets.get(currentFrame));
                descriptorWrite1.dstBinding(1);
                descriptorWrite1.dstArrayElement(slot);
                descriptorWrite1.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                descriptorWrite1.descriptorCount(1);
                descriptorWrite1.pImageInfo(newImageInfos);




                vkUpdateDescriptorSets(deviceWithIndices.device, descriptorWrites, null);
            }


            textureLookup.registerTexture(lvkTexture2D, slot);
            isUniqueTexture = true;
        }


        drawQuad(x, y, w, h, slot, color, originX, originY, rect2D, -1, xFlip, yFlip);

        if(isUniqueTexture) nextTextureSlot++;



    }

    @Override
    public void drawQuad(float x,
                          float y,
                          float w,
                          float h,
                          int quadTypeOrTextureIndex,
                          Color color,
                          float originX,
                          float originY,
                          Rect2D region,
                          float thickness,
                          boolean xFlip,
                          boolean yFlip){




        Quad quad = applyTransformations(x, y, w, h, originX, originY, region, xFlip, yFlip);

        Vector4f[] transformedPoints = quad.transformedPoints;
        Rect2D copy = quad.textureCoords;

        Vector4f topLeft = transformedPoints[0];
        Vector4f topRight = transformedPoints[1];
        Vector4f bottomLeft = transformedPoints[2];
        Vector4f bottomRight = transformedPoints[3];

        {

            //int dataPerQuad = vertexBuffer.getVertexDataSize() * 4;




            vertexBufferData.putFloat(topLeft.x);
            vertexBufferData.putFloat(topLeft.y);
            vertexBufferData.putFloat(copy.x);
            vertexBufferData.putFloat(copy.y);
            vertexBufferData.putFloat(quadTypeOrTextureIndex);
            vertexBufferData.putFloat(color.r);
            vertexBufferData.putFloat(color.g);
            vertexBufferData.putFloat(color.b);
            vertexBufferData.putFloat(color.a);
            vertexBufferData.putFloat(thickness);


            vertexBufferData.putFloat(bottomLeft.x);
            vertexBufferData.putFloat(bottomLeft.y);
            vertexBufferData.putFloat(copy.x);
            vertexBufferData.putFloat(copy.h);
            vertexBufferData.putFloat(quadTypeOrTextureIndex);
            vertexBufferData.putFloat(color.r);
            vertexBufferData.putFloat(color.g);
            vertexBufferData.putFloat(color.b);
            vertexBufferData.putFloat(color.a);
            vertexBufferData.putFloat(thickness);


            vertexBufferData.putFloat(bottomRight.x);
            vertexBufferData.putFloat(bottomRight.y);
            vertexBufferData.putFloat(copy.w);
            vertexBufferData.putFloat(copy.h);
            vertexBufferData.putFloat(quadTypeOrTextureIndex);
            vertexBufferData.putFloat(color.r);
            vertexBufferData.putFloat(color.g);
            vertexBufferData.putFloat(color.b);
            vertexBufferData.putFloat(color.a);
            vertexBufferData.putFloat(thickness);


            vertexBufferData.putFloat(topRight.x);
            vertexBufferData.putFloat(topRight.y);
            vertexBufferData.putFloat(copy.w);
            vertexBufferData.putFloat(copy.y);
            vertexBufferData.putFloat(quadTypeOrTextureIndex);
            vertexBufferData.putFloat(color.r);
            vertexBufferData.putFloat(color.g);
            vertexBufferData.putFloat(color.b);
            vertexBufferData.putFloat(color.a);
            vertexBufferData.putFloat(thickness);

        }
        quadCount++;



        //if(quadIndex == vertexBuffer.maxQuads()) render("Next Batch Render");
    }
    @Override
    public void render() {



        render("Final Render");
    }
    @Override
    public void render(String renderName) {





        indexBufferData.clear();



        int[] indices = generateIndices(quadCount);

        for (int i : indices) {
            indexBufferData.putInt(i);
        }


        recordCmdBuffers(indices.length);

        try(MemoryStack stack = stackPush()) {

            LVKRenderFrame thisFrame = renderSyncInfo.inFlightFrames.get(currentFrame);
























            vkWaitForFences(deviceWithIndices.device, thisFrame.pFence(), true, UINT64_MAX);

            IntBuffer pImageIndex = stack.mallocInt(1);

            vkAcquireNextImageKHR(deviceWithIndices.device, swapchain.swapChain, UINT64_MAX, thisFrame.imageAvailableSemaphore(), VK_NULL_HANDLE, pImageIndex);
            final int imageIndex = pImageIndex.get(0);

            if(renderSyncInfo.imagesInFlight.containsKey(imageIndex)) {
                vkWaitForFences(deviceWithIndices.device, renderSyncInfo.imagesInFlight.get(imageIndex).fence(), true, UINT64_MAX);
            }

            renderSyncInfo.imagesInFlight.put(imageIndex, thisFrame);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            {
                submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
                submitInfo.waitSemaphoreCount(1);
                submitInfo.pWaitSemaphores(thisFrame.pImageAvailableSemaphore());
                submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
                submitInfo.pSignalSemaphores(thisFrame.pRenderFinishedSemaphore());
                submitInfo.pCommandBuffers(stack.pointers(commandBuffers.get(imageIndex)));
            }

            vkResetFences(deviceWithIndices.device, thisFrame.pFence());

            if(vkQueueSubmit(graphicsQueue, submitInfo, thisFrame.fence()) != VK_SUCCESS) {
                throw new RuntimeException("Failed to submit draw command buffer");
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            {
                presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
                presentInfo.pWaitSemaphores(thisFrame.pRenderFinishedSemaphore());
                presentInfo.swapchainCount(1);
                presentInfo.pSwapchains(stack.longs(swapchain.swapChain));
                presentInfo.pImageIndices(pImageIndex);
            }

            vkQueuePresentKHR(presentQueue, presentInfo);

            currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;

            vkDeviceWaitIdle(deviceWithIndices.device);
        }






        vertexBufferData.clear();
        quadCount = 0;
        nextTextureSlot = 0;
        textureLookup.clear();
    }
    @Override
    public void clear(Color color) {
        super.clear(color);
    }
    @Override
    public String getDeviceName() {
        return physicalDeviceProperties.deviceNameString();
    }
    public static VkDeviceWithIndices getDeviceWithIndices() {
        return deviceWithIndices;
    }
    public static VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }
    private void destroyPipeline(LVKPipeline pipeline){

        vkDestroyPipeline(deviceWithIndices.device, pipeline.pipeline, null);
        vkDestroyPipelineLayout(deviceWithIndices.device, pipeline.pipelineLayout, null);

    }
    @Override
    public void dispose() {

        LVKCommandRunner.cleanup(deviceWithIndices.device);
        physicalDeviceProperties.free();

        vkDestroyDescriptorPool(deviceWithIndices.device, descriptorPool, null);

        vkDeviceWaitIdle(deviceWithIndices.device);
        for(LVKPipeline pipeline : pipelineCache.values()){
            destroyPipeline(pipeline);
        }



        renderSyncInfo.inFlightFrames.forEach(frame -> {

            vkDestroySemaphore(deviceWithIndices.device, frame.renderFinishedSemaphore(), null);
            vkDestroySemaphore(deviceWithIndices.device, frame.imageAvailableSemaphore(), null);
            vkDestroyFence(deviceWithIndices.device, frame.fence(), null);

            vkDestroyBuffer(deviceWithIndices.device, frame.getUniforms().getBuffer().handle, null);
            vkFreeMemory(deviceWithIndices.device, frame.getUniforms().getpMemory(), null);




        });
        renderSyncInfo.imagesInFlight.clear();


        vkDestroyRenderPass(deviceWithIndices.device, renderPass, null);
        vkDestroyCommandPool(deviceWithIndices.device, commandPool, null);

        swapchainFramebuffers.forEach(framebuffer -> vkDestroyFramebuffer(deviceWithIndices.device, framebuffer, null));
        swapchainImageViews.forEach(imageView -> vkDestroyImageView(deviceWithIndices.device, imageView, null));
        vkDestroySwapchainKHR(deviceWithIndices.device, swapchain.swapChain, null);

        vkDestroyDescriptorSetLayout(deviceWithIndices.device, descriptorSetLayout.get(), null);

        vkDestroyDevice(deviceWithIndices.device, null);
        FastVK.cleanupDebugMessenger(instance, true);

        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
    }
}
