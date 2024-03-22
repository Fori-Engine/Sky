package lake.graphics.vulkan;

import lake.FileReader;
import lake.Time;
import lake.graphics.*;
import lake.graphics.opengl.Texture2D;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class LVKRenderer2D extends Renderer2D implements Disposable {
    private int MAX_FRAMES_IN_FLIGHT = 2;

    long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
    private VkQueue graphicsQueue, presentQueue;
    private VkDeviceWithIndices deviceWithIndices;
    private VkPhysicalDevice physicalDevice;
    private VkInstance instance;
    private long surface;
    private LVKSwapchain swapchain;
    private List<Long> swapchainImageViews;
    private LVKPipeline pipeline;
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

    public LVKRenderer2D(StandaloneWindow window, int width, int height, boolean msaa) {
        super(width, height, msaa);

        Disposer.add("renderer", this);

        instance = FastVK.createInstance(getClass().getName(), "LakeEngine", true);
        FastVK.setupDebugMessenger(instance, true);
        surface = FastVK.createSurface(instance, window);
        physicalDevice = FastVK.pickPhysicalDevice(instance, surface);
        deviceWithIndices = FastVK.createLogicalDevice(physicalDevice, true, surface);
        graphicsQueue = FastVK.getGraphicsQueue(deviceWithIndices);
        presentQueue = FastVK.getPresentQueue(deviceWithIndices);
        swapchain = FastVK.createSwapChain(physicalDevice, deviceWithIndices.device, surface, width, height);
        swapchainImageViews = FastVK.createImageViews(deviceWithIndices.device, swapchain);
        renderPass = FastVK.createRenderPass(deviceWithIndices.device, swapchain);
        swapchainFramebuffers = FastVK.createFramebuffers(deviceWithIndices.device, swapchain, swapchainImageViews, renderPass);
        commandPool = FastVK.createCommandPool(deviceWithIndices);













        vertexBuffer = new LVKVertexBuffer(1, 5 * Float.BYTES);
        {
            vertexBuffer.setDeviceWithIndices(deviceWithIndices);
            vertexBuffer.setCommandPool(commandPool);
            vertexBuffer.setGraphicsQueue(graphicsQueue);
            vertexBuffer.setPhysicalDevice(physicalDevice);
            vertexBuffer.build();
        }




        vertexBuffer.getGenericBuffer().mapAndUpload(deviceWithIndices.device, vertexBuffer.getMappingBuffer(), new float[]{
                -0.5f, -0.5f, 1.0f, 0.0f, 0.0f,
                0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
                0.5f, 0.5f, 0.0f, 0.0f, 1.0f,
                -0.5f, 0.5f, 1.0f, 1.0f, 1.0f
        });


        indexBuffer = new LVKIndexBuffer(1, 6, Integer.BYTES);
        {
            indexBuffer.setDeviceWithIndices(deviceWithIndices);
            indexBuffer.setCommandPool(commandPool);
            indexBuffer.setGraphicsQueue(graphicsQueue);
            indexBuffer.setPhysicalDevice(physicalDevice);
            indexBuffer.build();
        }

        indexBuffer.getMainBuffer().mapAndUpload(deviceWithIndices.device, indexBuffer.getMappingBuffer(), new int[]{
                0, 1, 2, 2, 3, 0
        });




        renderSyncInfo = new LVKRenderSync();
        renderSyncInfo.inFlightFrames = new ArrayList<>(MAX_FRAMES_IN_FLIGHT);
        renderSyncInfo.imagesInFlight = new HashMap<>(swapchain.swapChainImages.size());



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
                LVKGenericBuffer uniformsBuffer = FastVK.createBuffer(deviceWithIndices.device, physicalDevice, LVKRenderFrame.LVKFrameUniforms.SIZE, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, pMemoryBuffer);
                LVKRenderFrame.LVKFrameUniforms uniforms = new LVKRenderFrame.LVKFrameUniforms(uniformsBuffer, pMemoryBuffer.get());
                frame.uniformBuffers().add(uniforms);
            }

        }




        //Descriptor Set Layout stuff
        {
            try(MemoryStack stack = stackPush()) {

                VkDescriptorSetLayoutBinding.Buffer uboLayoutBinding = VkDescriptorSetLayoutBinding.calloc(1, stack);
                uboLayoutBinding.binding(0);
                uboLayoutBinding.descriptorCount(1);
                uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboLayoutBinding.pImmutableSamplers(null);
                uboLayoutBinding.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

                VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
                layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
                layoutInfo.pBindings(uboLayoutBinding);

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

                VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.calloc(1, stack);
                poolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                poolSize.descriptorCount(swapchain.swapChainImages.size());

                VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
                poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
                poolInfo.pPoolSizes(poolSize);
                poolInfo.maxSets(swapchain.swapChainImages.size());

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
                for(int i = 0;i < layouts.capacity();i++) {
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

                VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
                bufferInfo.offset(0);

                bufferInfo.range(LVKRenderFrame.LVKFrameUniforms.SIZE);

                VkWriteDescriptorSet.Buffer descriptorWrite = VkWriteDescriptorSet.calloc(1, stack);
                descriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                descriptorWrite.dstBinding(0);
                descriptorWrite.dstArrayElement(0);
                descriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                descriptorWrite.descriptorCount(1);
                descriptorWrite.pBufferInfo(bufferInfo);

                List<LVKRenderFrame> inFlightFrames = renderSyncInfo.inFlightFrames;


                for (int i = 0; i < inFlightFrames.size(); i++) {
                    LVKRenderFrame frame = inFlightFrames.get(i);
                    for (LVKRenderFrame.LVKFrameUniforms uniforms : frame.uniformBuffers()) {
                        long descriptorSet = pDescriptorSets.get(i);
                        bufferInfo.buffer(uniforms.getBuffer().handle);
                        descriptorWrite.dstSet(descriptorSet);
                        vkUpdateDescriptorSets(deviceWithIndices.device, descriptorWrite, null);
                        descriptorSets.add(descriptorSet);
                    }
                }






            }


        }































        LVKShaderProgram shaderProgram = new LVKShaderProgram(
                FileReader.readFile(LVKRenderer2D.class.getClassLoader().getResourceAsStream("vulkan/VertexShader.glsl")),
                FileReader.readFile(LVKRenderer2D.class.getClassLoader().getResourceAsStream("vulkan/FragmentShader.glsl"))
        );




        shaderProgram.setDevice(deviceWithIndices.device);
        shaderProgram.prepare();

        pipeline = createPipeline(deviceWithIndices.device, swapchain, shaderProgram.getShaderStages(), renderPass, descriptorSetLayout);
        shaderProgram.disposeShaderModules();

        currentShaderProgram = shaderProgram;
        defaultShaderProgram = shaderProgram;




        commandBuffers = createCommandBuffers(
                deviceWithIndices.device,
                commandPool,
                renderPass,
                swapchain,
                swapchainFramebuffers,
                vertexBuffer,
                indexBuffer,
                pipeline,
                descriptorSets);























    }


    private List<VkCommandBuffer> createCommandBuffers(VkDevice device, long commandPool, long renderPass, LVKSwapchain swapchain, List<Long> swapChainFramebuffers, LVKVertexBuffer vertexBuffer, LVKIndexBuffer indexBuffer, LVKPipeline pipeline, List<Long> descriptorSets) {

        final int commandBuffersCount = swapChainFramebuffers.size();

        List<VkCommandBuffer> commandBuffers = new ArrayList<>(commandBuffersCount);

        try(MemoryStack stack = stackPush()) {

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(commandBuffersCount);

            PointerBuffer pCommandBuffers = stack.mallocPointer(commandBuffersCount);

            if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers");
            }

            for(int i = 0;i < commandBuffersCount;i++) {
                commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), device));
            }

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
            clearValues.color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
            renderPassInfo.pClearValues(clearValues);


            for(int i = 0; i < commandBuffersCount;i++) {

                VkCommandBuffer commandBuffer = commandBuffers.get(i);

                if(vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to begin recording command buffer");
                }

                renderPassInfo.framebuffer(swapChainFramebuffers.get(i));


                vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
                {

                    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.pipeline);


                    LongBuffer vertexBuffers = stack.longs(vertexBuffer.getGenericBuffer().handle);


                    LongBuffer offsets = stack.longs(0);
                    vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
                    vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getMainBuffer().handle, 0, VK_INDEX_TYPE_UINT32);

                    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipeline.pipelineLayout, 0, stack.longs(descriptorSets.get(i)), null);
                    vkCmdDrawIndexed(commandBuffer, 6, 1, 0, 0, 0);



                }
                vkCmdEndRenderPass(commandBuffer);


                if(vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to record command buffer");
                }

            }

        }

        return commandBuffers;
    }






    private LVKPipeline createPipeline(VkDevice device, LVKSwapchain swapchain, VkPipelineShaderStageCreateInfo.Buffer shaderStages, long renderPass, LongBuffer descriptorSetLayout){


        long pipelineLayout;
        long graphicsPipeline = 0;

        try(MemoryStack stack = stackPush()) {
            // ===> VERTEX STAGE <===

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);


            VkVertexInputBindingDescription.Buffer bindingDescription =
                    VkVertexInputBindingDescription.calloc(1, stack);

            bindingDescription.binding(0);
            bindingDescription.stride(5 * Float.BYTES);
            bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
            vertexInputInfo.pVertexBindingDescriptions(bindingDescription);









            VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                    VkVertexInputAttributeDescription.calloc(2);

            // Position
            VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(0);
            {
                posDescription.binding(0);
                posDescription.location(0);
                posDescription.format(VK_FORMAT_R32G32_SFLOAT);
                posDescription.offset(0);
            }

            // Color
            VkVertexInputAttributeDescription colorDescription = attributeDescriptions.get(1);
            {
                colorDescription.binding(0);
                colorDescription.location(1);
                colorDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
                colorDescription.offset(2 * Float.BYTES);
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
                rasterizer.frontFace(VK_FRONT_FACE_CLOCKWISE);
                rasterizer.depthBiasEnable(false);
            }
            // ===> MULTISAMPLING <===

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            {
                multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
                multisampling.sampleShadingEnable(false);
                multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);
            }
            // ===> COLOR BLENDING <===

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            {
                colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
                colorBlendAttachment.blendEnable(false);
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
    public void updateCamera2D() {

    }

    @Override
    public void drawTexture(float x, float y, float w, float h, Texture2D texture) {

    }

    @Override
    public void drawRect(float x, float y, float w, float h, Color color, int thickness) {

    }

    @Override
    public void drawLine(float x1, float y1, float x2, float y2, Color color, int thickness, boolean round) {

    }

    @Override
    public void drawTexture(float x, float y, float w, float h, Texture2D texture, Color color) {

    }

    @Override
    public void drawTexture(float x, float y, float w, float h, Texture2D texture, Color color, Rect2D rect2D, boolean xFlip, boolean yFlip) {

    }

    @Override
    public void drawFilledRect(float x, float y, float w, float h, Color color) {

    }

    @Override
    public void drawFilledEllipse(float x, float y, float w, float h, Color color) {

    }

    @Override
    public void drawEllipse(float x, float y, float w, float h, Color color, float thickness) {

    }

    @Override
    public void render() {
        render("Final Render");
    }


    float t = 0;

    @Override
    public void render(String renderName) {
        try(MemoryStack stack = stackPush()) {

            LVKRenderFrame thisFrame = renderSyncInfo.inFlightFrames.get(currentFrame);


            //Uniforms
            {
                for(LVKRenderFrame.LVKFrameUniforms uniforms : thisFrame.uniformBuffers()){

                    PointerBuffer data = stack.mallocPointer(1);

                    uniforms.getBuffer().mapAndUpload(deviceWithIndices.device, data, new float[]{
                            (float) (Math.pow(Math.sin(t), 2) + 1) / 2
                    });





















                }

                t += Time.deltaTime * 3;
            }





















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
    }

    @Override
    public void clear(Color color) {

    }

    @Override
    public void drawText(float x, float y, String text, Color color, Font2D font) {

    }

    private void cleanupSwapchain(){
        swapchainFramebuffers.forEach(framebuffer -> vkDestroyFramebuffer(deviceWithIndices.device, framebuffer, null));
        vkDestroyPipeline(deviceWithIndices.device, pipeline.pipeline, null);
        vkDestroyPipelineLayout(deviceWithIndices.device, pipeline.pipelineLayout, null);
        swapchainImageViews.forEach(imageView -> vkDestroyImageView(deviceWithIndices.device, imageView, null));
        vkDestroySwapchainKHR(deviceWithIndices.device, swapchain.swapChain, null);
    }

    @Override
    public void dispose() {

        vkDestroyDescriptorPool(deviceWithIndices.device, descriptorPool, null);


        renderSyncInfo.inFlightFrames.forEach(frame -> {

            vkDestroySemaphore(deviceWithIndices.device, frame.renderFinishedSemaphore(), null);
            vkDestroySemaphore(deviceWithIndices.device, frame.imageAvailableSemaphore(), null);
            vkDestroyFence(deviceWithIndices.device, frame.fence(), null);

            for(LVKRenderFrame.LVKFrameUniforms uniforms : frame.uniformBuffers()){
                vkDestroyBuffer(deviceWithIndices.device, uniforms.getBuffer().handle, null);
                vkFreeMemory(deviceWithIndices.device, uniforms.getpMemory(), null);
            }




        });
        renderSyncInfo.imagesInFlight.clear();


        vkDestroyRenderPass(deviceWithIndices.device, renderPass, null);
        vkDestroyCommandPool(deviceWithIndices.device, commandPool, null);

        cleanupSwapchain();

        vkDestroyDescriptorSetLayout(deviceWithIndices.device, descriptorSetLayout.get(), null);

        vkDestroyDevice(deviceWithIndices.device, null);
        FastVK.cleanupDebugMessenger(instance, true);

        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
    }
}
