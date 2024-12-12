package editor.awt;

import org.lwjgl.system.Platform;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;

import java.awt.*;

import static org.lwjgl.vulkan.KHRWin32Surface.VK_KHR_WIN32_SURFACE_EXTENSION_NAME;

/**
 * Vulkan API. To use the surface, {@link org.lwjgl.vulkan.KHRSurface#VK_KHR_SURFACE_EXTENSION_NAME VK_KHR_SURFACE_EXTENSION_NAME}
 * and {@link #getSurfaceExtensionName()} must be enabled extensions.
 */
public class AWTVK {





    /**
     * Uses the provided canvas to create a Vulkan surface to draw on.
     * @param canvas canvas to render onto
     * @param instance vulkan instance
     * @return handle of the surface
     * @throws AWTException if the surface creation fails
     */
    public static long create(Canvas canvas, VkInstance instance) throws AWTException {
        switch (Platform.get()) {
            case WINDOWS: return PlatformWin32VKCanvas.create(canvas, instance);
            default: throw new RuntimeException("Platform " + Platform.get() + " not supported in lwjgl3-awt.");
        }
    }

    public static String getSurfaceExtensionName() {
        return VK_KHR_WIN32_SURFACE_EXTENSION_NAME;
    }
}