package lake.graphics.vulkan;

import lake.FlightRecorder;
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

public class VulkanRenderer2D extends Renderer2D {
    public static int MAX_FRAMES_IN_FLIGHT = 2;
    long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
    private VkQueue graphicsQueue, presentQueue;
    private static VkDeviceWithIndices deviceWithIndices;
    private static VkPhysicalDevice physicalDevice;
    private static VkPhysicalDeviceProperties physicalDeviceProperties;
    private VkInstance instance;
    private long surface;
    private VulkanSwapchain swapchain;
    private List<Long> swapchainImageViews;
    private long renderPass;
    private List<Long> swapchainFramebuffers;
    private long commandPool;
    private List<VkCommandBuffer> commandBuffers;
    private int currentFrame;
    private ByteBuffer vertexBufferData, indexBufferData;

    private Map<ShaderProgram, VulkanPipeline> pipelineCache = new HashMap<>();
    public List<VulkanSyncData> inFlightFrames;
    public Map<Integer, VulkanSyncData> imagesInFlight;

    public static int TOTAL_SIZE_BYTES = 3 * 16 * Float.BYTES;
    public static int MATRIX_SIZE_BYTES = 16 * Float.BYTES;
    private IntBuffer pImageIndex;



    public VulkanRenderer2D(VkInstance instance, long surface, int width, int height, RenderSettings settings) {
        super(width, height, settings);




        VulkanUtil.setupDebugMessenger(instance, settings.enableValidation);




        physicalDevice = VulkanUtil.pickPhysicalDevice(instance, surface);



        deviceWithIndices = VulkanUtil.createLogicalDevice(physicalDevice, settings.enableValidation, surface);
        graphicsQueue = VulkanUtil.getGraphicsQueue(deviceWithIndices);
        presentQueue = VulkanUtil.getPresentQueue(deviceWithIndices);
        swapchain = VulkanUtil.createSwapChain(physicalDevice, deviceWithIndices.device, surface, width, height);
        swapchainImageViews = VulkanUtil.createImageViews(deviceWithIndices.device, swapchain);




        physicalDeviceProperties = VulkanUtil.getPhysicalDeviceProperties(physicalDevice);
        FlightRecorder.info(VulkanRenderer2D.class,
                "Selected Physical Device: " +
                        physicalDeviceProperties.deviceNameString() +
                        "\nDriver Version: " +
                        physicalDeviceProperties.driverVersion()
                );



        FlightRecorder.info(VulkanRenderer2D.class, "Max Samplers allowed in Descriptor Set: " + physicalDeviceProperties.limits().maxDescriptorSetSamplers());



        renderPass = VulkanUtil.createRenderPass(deviceWithIndices.device, swapchain);
        swapchainFramebuffers = VulkanUtil.createFramebuffers(deviceWithIndices.device, swapchain, swapchainImageViews, renderPass);
        commandPool = VulkanUtil.createCommandPool(deviceWithIndices);
        VulkanUtil.setupUtilsCommandPool(deviceWithIndices, graphicsQueue);










        pImageIndex = MemoryUtil.memAllocInt(1);




        vertexBuffer = new VulkanVertexBuffer(
                settings.quadsPerBatch,
                10,
                deviceWithIndices.device,
                commandPool,
                graphicsQueue,
                physicalDevice);

        indexBuffer = new VulkanIndexBuffer(
                settings.quadsPerBatch,
                6,
                Integer.BYTES,
                deviceWithIndices.device,
                commandPool,
                graphicsQueue,
                physicalDevice);



        vertexBufferData = ((VulkanVertexBuffer) vertexBuffer).getMainBuffer().mapAndGet(deviceWithIndices.device, ((VulkanVertexBuffer) vertexBuffer).getMappingBuffer());
        indexBufferData = ((VulkanIndexBuffer) indexBuffer).getMainBuffer().mapAndGet(deviceWithIndices.device, ((VulkanIndexBuffer) indexBuffer).getMappingBuffer());



        int[] indices = generateIndices(200);

        for (int i : indices) {
            indexBufferData.putInt(i);
        }




        inFlightFrames = new ArrayList<>(MAX_FRAMES_IN_FLIGHT);
        imagesInFlight = new HashMap<>(MAX_FRAMES_IN_FLIGHT);



        try(MemoryStack stack = stackPush()) {

            VkSemaphoreCreateInfo imageAcquiredSemaphore = VkSemaphoreCreateInfo.calloc(stack);
            imageAcquiredSemaphore.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkSemaphoreCreateInfo renderFinishedSemaphore = VkSemaphoreCreateInfo.calloc(stack);
            renderFinishedSemaphore.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo submissionFence = VkFenceCreateInfo.calloc(stack);
            submissionFence.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            submissionFence.flags(VK_FENCE_CREATE_SIGNALED_BIT);


            LongBuffer pImageAcquiredSemaphore = stack.mallocLong(1);
            LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
            LongBuffer pSubmissionFence = stack.mallocLong(1);



            for(int i = 0;i < MAX_FRAMES_IN_FLIGHT;i++) {

                if(vkCreateSemaphore(deviceWithIndices.device, imageAcquiredSemaphore, null, pImageAcquiredSemaphore) != VK_SUCCESS
                        || vkCreateSemaphore(deviceWithIndices.device, renderFinishedSemaphore, null, pRenderFinishedSemaphore) != VK_SUCCESS
                        || vkCreateFence(deviceWithIndices.device, submissionFence, null, pSubmissionFence) != VK_SUCCESS) {

                    throw new RuntimeException("Failed to create synchronization objects for the frame " + i);
                }

                inFlightFrames.add(new VulkanSyncData(pImageAcquiredSemaphore.get(0), pRenderFinishedSemaphore.get(0), pSubmissionFence.get(0)));
            }


        }





        proj = new Matrix4f().ortho(0, getWidth(), 0, getHeight(), 0, 1, true);




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
    private VulkanPipeline createPipeline(VkDevice device, VulkanSwapchain swapchain, VkPipelineShaderStageCreateInfo.Buffer shaderStages, long renderPass, LongBuffer descriptorSetLayout){

        FlightRecorder.info(VulkanRenderer2D.class, "Creating new pipeline...");

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

        return new VulkanPipeline(pipelineLayout, graphicsPipeline);
    }



    @Override
    public void updateMatrices(ShaderProgram shaderProgram, ShaderResource modelViewProj) {

        ByteBuffer[] buffers = shaderProgram.mapUniformBuffer(modelViewProj);

        for(ByteBuffer buffer : buffers) {
            model.get(buffer);
            camera.getViewMatrix().get(MATRIX_SIZE_BYTES, buffer);
            proj.get(2 * MATRIX_SIZE_BYTES, buffer);
        }

        shaderProgram.unmapUniformBuffer(modelViewProj, buffers);
    }

    @Override
    public void drawTexture(float x, float y, float w, float h, Texture2D texture, Color color, Rect2D rect2D, boolean xFlip, boolean yFlip) {

        VulkanTexture2D vulkanTexture2D = (VulkanTexture2D) texture;

        int textureIndex = currentBatch.nextTextureIndex;
        boolean uniqueTexture = false;

        if(currentBatch.textureLookup.hasTexture(vulkanTexture2D)) {
            textureIndex = currentBatch.textureLookup.getTexture(texture);
        }
        else {

            currentBatch.textureLookup.registerTexture(vulkanTexture2D, currentBatch.nextTextureIndex);
            currentBatch.shaderProgram.updateSampler2DArray(
                    currentBatch.shaderProgram.getResourceByBinding(1),
                    currentBatch.nextTextureIndex,
                    texture
            );

            uniqueTexture = true;
        }

        //Maybe dispatch a batch when we run out of samplers in the shader?
        drawQuad(x, y, w, h, textureIndex, color, originX, originY, rect2D, -1, xFlip, yFlip);

        if(uniqueTexture) currentBatch.nextTextureIndex++;
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
        currentQuadCount++;
        batchQuadCount++;
    }


    @Override
    public void createResources(ShaderProgram... shaderPrograms) {
        for(ShaderProgram shaderProgram : shaderPrograms){
            VulkanShaderProgram vulkanShaderProgram = (VulkanShaderProgram) shaderProgram;


            vulkanShaderProgram.createDescriptors();
            VulkanPipeline newPipeline = createPipeline(
                    deviceWithIndices.device,
                    swapchain,
                    vulkanShaderProgram.getShaderStages(),
                    renderPass,
                    vulkanShaderProgram.getDescriptorSetLayout());

            pipelineCache.put(shaderProgram, newPipeline);
        }
    }


    @Override
    public void render() {


        VulkanSyncData thisFrame = inFlightFrames.get(currentFrame);


        vkAcquireNextImageKHR(deviceWithIndices.device, swapchain.swapChain, UINT64_MAX, thisFrame.imageAcquiredSemaphore, VK_NULL_HANDLE, pImageIndex);



        vkWaitForFences(deviceWithIndices.device, thisFrame.submissionFence, true, UINT64_MAX);
        vkResetFences(deviceWithIndices.device, thisFrame.submissionFence);



        try(MemoryStack stack = stackPush()) {

            int imageIndex = pImageIndex.get(0);


            //Command Buffer Recording
            {
                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
                beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

                VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
                {
                    renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
                    renderPassInfo.renderPass(renderPass);
                    VkRect2D renderArea = VkRect2D.calloc(stack);
                    renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
                    renderArea.extent(swapchain.swapChainExtent);
                    renderPassInfo.renderArea(renderArea);
                    VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
                    clearValues.color().float32(stack.floats(clearColor.r, clearColor.g, clearColor.b, clearColor.a));
                    renderPassInfo.pClearValues(clearValues);
                }


                VkCommandBuffer commandBuffer = commandBuffers.get(currentFrame);

                if (vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to begin recording command buffer");
                }

                renderPassInfo.framebuffer(swapchainFramebuffers.get(currentFrame));


                vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
                {

                    LongBuffer vertexBuffers = stack.longs(((VulkanVertexBuffer) vertexBuffer).getMainBuffer().handle);
                    LongBuffer offsets = stack.longs(0);

                    vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
                    vkCmdBindIndexBuffer(commandBuffer, ((VulkanIndexBuffer) indexBuffer).getMainBuffer().handle, 0, VK_INDEX_TYPE_UINT32);


                    for (RenderBatch batch : batches) {
                        VulkanPipeline vulkanPipeline = pipelineCache.get(batch.shaderProgram);


                        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, vulkanPipeline.pipeline);

                        vkCmdBindDescriptorSets(
                                commandBuffer,
                                VK_PIPELINE_BIND_POINT_GRAPHICS,
                                vulkanPipeline.pipelineLayout,
                                0,
                                stack.longs(((VulkanShaderProgram) batch.shaderProgram).getDescriptorSets().get(currentFrame)),
                                null
                        );

                        vkCmdDrawIndexed(
                                commandBuffer,
                                batch.indices,
                                1,
                                0,
                                batch.start * 4,
                                0
                        );
                    }
                }
                vkCmdEndRenderPass(commandBuffer);


                if (vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to record command buffer");
                }

            }

            //Submission
            {
                imagesInFlight.put(imageIndex, thisFrame);

                VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
                {
                    submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
                    submitInfo.pWaitSemaphores(stack.longs(thisFrame.imageAcquiredSemaphore));
                    submitInfo.pSignalSemaphores(stack.longs(thisFrame.renderFinishedSemaphore));
                    submitInfo.waitSemaphoreCount(1);
                    submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
                    submitInfo.pCommandBuffers(stack.pointers(commandBuffers.get(imageIndex)));
                }

                if(vkQueueSubmit(graphicsQueue, submitInfo, thisFrame.submissionFence) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to submit draw command buffer");
                }
            }



            //Present
            {
                VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
                {
                    presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
                    presentInfo.pWaitSemaphores(stack.longs(thisFrame.renderFinishedSemaphore));
                    presentInfo.swapchainCount(1);
                    presentInfo.pSwapchains(stack.longs(swapchain.swapChain));
                    presentInfo.pImageIndices(pImageIndex);
                }

                vkQueuePresentKHR(presentQueue, presentInfo);
            }





        }






        currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;

        batches.clear();
        vertexBufferData.clear();
        currentQuadCount = 0;


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
    public static VkPhysicalDeviceProperties getPhysicalDeviceProperties() {
        return physicalDeviceProperties;
    }
    private void destroyPipeline(VulkanPipeline pipeline){

        vkDestroyPipeline(deviceWithIndices.device, pipeline.pipeline, null);
        vkDestroyPipelineLayout(deviceWithIndices.device, pipeline.pipelineLayout, null);

    }
    @Override
    public void dispose() {



        vkDeviceWaitIdle(deviceWithIndices.device);

        VulkanUtil.cleanupUtilsCommandPool(deviceWithIndices.device);
        physicalDeviceProperties.free();




        for(ShaderProgram sp : pipelineCache.keySet()){
            VulkanShaderProgram shaderProgram = (VulkanShaderProgram) sp;
            vkDestroyDescriptorPool(deviceWithIndices.device, shaderProgram.getDescriptorPool(), null);
            destroyPipeline(pipelineCache.get(shaderProgram));
        }


        inFlightFrames.forEach(frame -> {
            vkDestroySemaphore(deviceWithIndices.device, frame.imageAcquiredSemaphore, null);
            vkDestroySemaphore(deviceWithIndices.device, frame.renderFinishedSemaphore, null);
            vkDestroyFence(deviceWithIndices.device, frame.submissionFence, null);
        });




        imagesInFlight.clear();

        vkDestroyRenderPass(deviceWithIndices.device, renderPass, null);
        vkDestroyCommandPool(deviceWithIndices.device, commandPool, null);

        swapchainFramebuffers.forEach(framebuffer -> vkDestroyFramebuffer(deviceWithIndices.device, framebuffer, null));
        swapchainImageViews.forEach(imageView -> vkDestroyImageView(deviceWithIndices.device, imageView, null));
        vkDestroySwapchainKHR(deviceWithIndices.device, swapchain.swapChain, null);

        for(ShaderProgram sp : pipelineCache.keySet()){
            VulkanShaderProgram shaderProgram = (VulkanShaderProgram) sp;
            vkDestroyDescriptorSetLayout(deviceWithIndices.device, shaderProgram.getDescriptorSetLayout().get(), null);
        }



        vkDestroyDevice(deviceWithIndices.device, null);
        VulkanUtil.cleanupDebugMessenger(instance, true);

        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
    }
}
