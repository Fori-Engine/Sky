package lake.demo;

import lake.graphics.StandaloneWindow;
import lake.graphics.Window;
import lake.vulkan.FastVK;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;

public class VulkanDemo {



    public static void main(String[] args) {
        Window window = new StandaloneWindow(640, 480, "Vulkan Demo", false, false);

        VkInstance instance = FastVK.createInstance("Vulkan Demo", "LakeEngine", true);
        FastVK.setupDebugMessenger(instance, true);
        VkPhysicalDevice physicalDevice = FastVK.pickPhysicalDevice(instance);



        while(!window.shouldClose()){


            window.update();
        }



        FastVK.cleanup(instance);
        window.close();

    }
}
