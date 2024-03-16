package lake.demo;

import lake.graphics.StandaloneWindow;
import lake.vulkan.FastVK;
import lake.vulkan.Swapchain;
import lake.vulkan.VkDeviceWithIndices;
import static org.lwjgl.vulkan.KHRSurface.*;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;

public class VulkanDemo {



    public static void main(String[] args) {
        StandaloneWindow window = new StandaloneWindow(640, 480, "Vulkan Demo", false, false);

        VkInstance instance = FastVK.createInstance("Vulkan Demo", "LakeEngine", true);
        FastVK.setupDebugMessenger(instance, true);

        long surface = FastVK.createSurface(instance, window);

        VkPhysicalDevice physicalDevice = FastVK.pickPhysicalDevice(instance, surface);
        VkDeviceWithIndices deviceWithIndices = FastVK.createLogicalDevice(physicalDevice, true, surface);


        VkQueue graphicsQueue = FastVK.getGraphicsQueue(deviceWithIndices);
        VkQueue presentQueue = FastVK.getPresentQueue(deviceWithIndices);

        Swapchain swapchain = FastVK.createSwapChain(physicalDevice, deviceWithIndices.device, surface, window.getWidth(), window.getHeight());




        while(!window.shouldClose()){


            window.update();
        }

        vkDestroySwapchainKHR(deviceWithIndices.device, swapchain.swapChain, null);
        vkDestroyDevice(deviceWithIndices.device, null);
        FastVK.cleanupDebugMessenger(instance, true);

        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);



        window.close();

    }
}
