package lake.demo;

import lake.FileReader;
import lake.graphics.Renderer2D;
import lake.graphics.StandaloneWindow;
import lake.vulkan.*;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;

public class VulkanDemo {



    public static void main(String[] args) {



        int MAX_FRAMES_IN_FLIGHT = 2;





        StandaloneWindow window = new StandaloneWindow(640, 480, "Vulkan Demo", false, false);

        VkInstance instance = FastVK.createInstance("Vulkan Demo", "LakeEngine", true);
        FastVK.setupDebugMessenger(instance, true);

        long surface = FastVK.createSurface(instance, window);

        VkPhysicalDevice physicalDevice = FastVK.pickPhysicalDevice(instance, surface);
        VkDeviceWithIndices deviceWithIndices = FastVK.createLogicalDevice(physicalDevice, true, surface);


        VkQueue graphicsQueue = FastVK.getGraphicsQueue(deviceWithIndices);
        VkQueue presentQueue = FastVK.getPresentQueue(deviceWithIndices);

        Swapchain swapchain = FastVK.createSwapChain(physicalDevice, deviceWithIndices.device, surface, window.getWidth(), window.getHeight());
        List<Long> swapchainImageViews = FastVK.createImageViews(deviceWithIndices.device, swapchain);

        VulkanShaderProgram shaderProgram = new VulkanShaderProgram(
                FileReader.readFile(Renderer2D.class.getClassLoader().getResourceAsStream("vulkan/VertexShader.glsl")),
                FileReader.readFile(Renderer2D.class.getClassLoader().getResourceAsStream("vulkan/FragmentShader.glsl"))
        );


        long renderPass = FastVK.createRenderPass(deviceWithIndices.device, swapchain);

        shaderProgram.setDevice(deviceWithIndices.device);
        shaderProgram.prepare();

        VulkanPipeline pipeline = FastVK.createPipeline(deviceWithIndices.device, swapchain, shaderProgram.getShaderStages(), renderPass);
        shaderProgram.disposeShaderModules();

        List<Long> swapchainFramebuffers = FastVK.createFramebuffers(deviceWithIndices.device, swapchain, swapchainImageViews, renderPass);

        long commandPool = FastVK.createCommandPool(deviceWithIndices);
        List<VkCommandBuffer> commandBuffers = FastVK.createCommandBuffers(deviceWithIndices.device, commandPool, renderPass, swapchain, swapchainFramebuffers, pipeline);
        VulkanRenderSyncInfo renderSyncInfo = FastVK.createSyncObjects(deviceWithIndices.device, swapchain, MAX_FRAMES_IN_FLIGHT);


        int currentFrame = 0;

        long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;


        while(!window.shouldClose()){






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
                submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

                submitInfo.waitSemaphoreCount(1);
                submitInfo.pWaitSemaphores(thisFrame.pImageAvailableSemaphore());
                submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));

                submitInfo.pSignalSemaphores(thisFrame.pRenderFinishedSemaphore());

                submitInfo.pCommandBuffers(stack.pointers(commandBuffers.get(imageIndex)));

                vkResetFences(deviceWithIndices.device, thisFrame.pFence());

                if(vkQueueSubmit(graphicsQueue, submitInfo, thisFrame.fence()) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to submit draw command buffer");
                }

                VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
                presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);

                presentInfo.pWaitSemaphores(thisFrame.pRenderFinishedSemaphore());

                presentInfo.swapchainCount(1);
                presentInfo.pSwapchains(stack.longs(swapchain.swapChain));

                presentInfo.pImageIndices(pImageIndex);

                vkQueuePresentKHR(presentQueue, presentInfo);

                currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
            }





















            window.update();
        }


        renderSyncInfo.inFlightFrames.forEach(frame -> {

            vkDestroySemaphore(deviceWithIndices.device, frame.renderFinishedSemaphore(), null);
            vkDestroySemaphore(deviceWithIndices.device, frame.imageAvailableSemaphore(), null);
            vkDestroyFence(deviceWithIndices.device, frame.fence(), null);
        });
        renderSyncInfo.imagesInFlight.clear();


        vkDestroyRenderPass(deviceWithIndices.device, renderPass, null);
        vkDestroyCommandPool(deviceWithIndices.device, commandPool, null);
        swapchainFramebuffers.forEach(framebuffer -> vkDestroyFramebuffer(deviceWithIndices.device, framebuffer, null));
        vkDestroyPipeline(deviceWithIndices.device, pipeline.pipeline, null);
        vkDestroyPipelineLayout(deviceWithIndices.device, pipeline.pipelineLayout, null);
        swapchainImageViews.forEach(imageView -> vkDestroyImageView(deviceWithIndices.device, imageView, null));
        vkDestroySwapchainKHR(deviceWithIndices.device, swapchain.swapChain, null);
        vkDestroyDevice(deviceWithIndices.device, null);
        FastVK.cleanupDebugMessenger(instance, true);

        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);



        window.close();

    }
}
