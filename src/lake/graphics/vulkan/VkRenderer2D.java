package lake.graphics.vulkan;

import lake.FileReader;
import lake.graphics.*;
import lake.graphics.opengl.Texture2D;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VkRenderer2D extends Renderer2D implements Disposable {
    private int MAX_FRAMES_IN_FLIGHT = 2;

    long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
    private VkQueue graphicsQueue, presentQueue;
    private VkDeviceWithIndices deviceWithIndices;
    private VkPhysicalDevice physicalDevice;
    private VkInstance instance;
    private long surface;
    private Swapchain swapchain;
    private List<Long> swapchainImageViews;
    private VulkanPipeline pipeline;
    private long renderPass;
    private List<Long> swapchainFramebuffers;
    private long commandPool;
    private VulkanVertexBuffer vertexBuffer;
    private VulkanIndexBuffer indexBuffer;
    private List<VkCommandBuffer> commandBuffers;
    private VulkanRenderSyncInfo renderSyncInfo;
    private int currentFrame;


    public VkRenderer2D(StandaloneWindow window, int width, int height, boolean msaa) {
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

        VulkanShaderProgram shaderProgram = new VulkanShaderProgram(
                FileReader.readFile(VkRenderer2D.class.getClassLoader().getResourceAsStream("vulkan/VertexShader.glsl")),
                FileReader.readFile(VkRenderer2D.class.getClassLoader().getResourceAsStream("vulkan/FragmentShader.glsl"))
        );


        renderPass = FastVK.createRenderPass(deviceWithIndices.device, swapchain);

        shaderProgram.setDevice(deviceWithIndices.device);
        shaderProgram.prepare();

        pipeline = FastVK.createPipeline(deviceWithIndices.device, swapchain, shaderProgram.getShaderStages(), renderPass);
        shaderProgram.disposeShaderModules();

        currentShaderProgram = shaderProgram;
        defaultShaderProgram = shaderProgram;




        swapchainFramebuffers = FastVK.createFramebuffers(deviceWithIndices.device, swapchain, swapchainImageViews, renderPass);

        commandPool = FastVK.createCommandPool(deviceWithIndices);

        vertexBuffer = new VulkanVertexBuffer(1, 5 * Float.BYTES);
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


        indexBuffer = new VulkanIndexBuffer(Integer.BYTES);
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

            Frame thisFrame = renderSyncInfo.inFlightFrames.get(currentFrame);

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
