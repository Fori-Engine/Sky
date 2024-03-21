package lake.graphics.vulkan;

import lake.FileReader;
import lake.graphics.*;
import lake.graphics.opengl.Texture2D;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
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

        LVKShaderProgram shaderProgram = new LVKShaderProgram(
                FileReader.readFile(LVKRenderer2D.class.getClassLoader().getResourceAsStream("vulkan/VertexShader.glsl")),
                FileReader.readFile(LVKRenderer2D.class.getClassLoader().getResourceAsStream("vulkan/FragmentShader.glsl"))
        );


        renderPass = FastVK.createRenderPass(deviceWithIndices.device, swapchain);

        shaderProgram.setDevice(deviceWithIndices.device);
        shaderProgram.prepare();

        pipeline = createPipeline(deviceWithIndices.device, swapchain, shaderProgram.getShaderStages(), renderPass);
        shaderProgram.disposeShaderModules();

        currentShaderProgram = shaderProgram;
        defaultShaderProgram = shaderProgram;




        swapchainFramebuffers = FastVK.createFramebuffers(deviceWithIndices.device, swapchain, swapchainImageViews, renderPass);

        commandPool = FastVK.createCommandPool(deviceWithIndices);

        vertexBuffer = new LVKVertexBuffer(1, 5 * Float.BYTES);
        vertexBuffer.setDeviceWithIndices(deviceWithIndices);
        vertexBuffer.setCommandPool(commandPool);
        vertexBuffer.setGraphicsQueue(graphicsQueue);
        vertexBuffer.setPhysicalDevice(physicalDevice);
        vertexBuffer.build();







        vkMapMemory(deviceWithIndices.device, vertexBuffer.getVertexBufferMemory(), 0, vertexBuffer.getBufferInfo().size(), 0, vertexBuffer.getData());
        {
            memcpy(vertexBuffer.getData().getByteBuffer(0, (int) vertexBuffer.getBufferInfo().size()), new float[]{
                    -0.5f, -0.5f, 1.0f, 0.0f, 0.0f,
                    0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
                    0.5f, 0.5f, 0.0f, 0.0f, 1.0f,
                    -0.5f, 0.5f, 1.0f, 1.0f, 1.0f
            });
        }
        vkUnmapMemory(deviceWithIndices.device, vertexBuffer.getVertexBufferMemory());


        indexBuffer = new LVKIndexBuffer(Integer.BYTES);
        indexBuffer.setDeviceWithIndices(deviceWithIndices);
        indexBuffer.setCommandPool(commandPool);
        indexBuffer.setGraphicsQueue(graphicsQueue);
        indexBuffer.setPhysicalDevice(physicalDevice);
        indexBuffer.build();



        vkMapMemory(deviceWithIndices.device, indexBuffer.getIndexBufferMemory(), 0, indexBuffer.getBufferInfo().size(), 0, indexBuffer.getData());
        {
            memcpy(indexBuffer.getData().getByteBuffer(0, (int) indexBuffer.getBufferInfo().size()), new int[]{
                    0, 1, 2, 2, 3, 0
            });
        }
        vkUnmapMemory(deviceWithIndices.device, indexBuffer.getIndexBufferMemory());



        commandBuffers = FastVK.createCommandBuffers(deviceWithIndices.device, commandPool, renderPass, swapchain, swapchainFramebuffers, vertexBuffer, indexBuffer, pipeline);
        renderSyncInfo = FastVK.createSyncObjects(deviceWithIndices.device, swapchain, MAX_FRAMES_IN_FLIGHT);

















    }

    private static void memcpy(ByteBuffer buffer, float[] vertices) {
        for(float f : vertices){
            buffer.putFloat(f);
        }
    }

    private static void memcpy(ByteBuffer buffer, int[] vertices) {
        for(int f : vertices){
            buffer.putInt(f);
        }
    }






    private LVKPipeline createPipeline(VkDevice device, LVKSwapchain swapchain, VkPipelineShaderStageCreateInfo.Buffer shaderStages, long renderPass){


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

    @Override
    public void render(String renderName) {
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
        renderSyncInfo.inFlightFrames.forEach(frame -> {

            vkDestroySemaphore(deviceWithIndices.device, frame.renderFinishedSemaphore(), null);
            vkDestroySemaphore(deviceWithIndices.device, frame.imageAvailableSemaphore(), null);
            vkDestroyFence(deviceWithIndices.device, frame.fence(), null);
        });
        renderSyncInfo.imagesInFlight.clear();


        vkDestroyRenderPass(deviceWithIndices.device, renderPass, null);
        vkDestroyCommandPool(deviceWithIndices.device, commandPool, null);

        cleanupSwapchain();

        vkDestroyDevice(deviceWithIndices.device, null);
        FastVK.cleanupDebugMessenger(instance, true);

        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
    }
}
