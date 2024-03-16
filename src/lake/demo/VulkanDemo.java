package lake.demo;

import lake.graphics.StandaloneWindow;
import lake.graphics.Window;
import lake.vulkan.FastVK;
import lake.vulkan.VkDeviceConfig;
import static org.lwjgl.vulkan.KHRSurface.*;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanDemo {



    public static void main(String[] args) {
        StandaloneWindow window = new StandaloneWindow(640, 480, "Vulkan Demo", false, false);

        VkInstance instance = FastVK.createInstance("Vulkan Demo", "LakeEngine", true);
        FastVK.setupDebugMessenger(instance, true);

        long surface = FastVK.createSurface(instance, window);

        VkPhysicalDevice physicalDevice = FastVK.pickPhysicalDevice(instance);
        VkDeviceConfig logicalDeviceConfig = FastVK.createLogicalDevice(physicalDevice, true);
        VkQueue graphicsQueue = FastVK.getGraphicsQueue(logicalDeviceConfig);





        while(!window.shouldClose()){


            window.update();
        }

        vkDestroyDevice(logicalDeviceConfig.device, null);
        FastVK.cleanupDebugMessenger(instance);

        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);



        window.close();

    }
}
