package lake.graphics.vulkan;

import lake.graphics.RenderContext;
import lake.graphics.PlatformWindow;
import lake.graphics.RenderSettings;
import lake.graphics.Renderer2D;
import org.lwjgl.vulkan.VkInstance;

import static org.lwjgl.glfw.GLFW.*;

public class VulkanRenderContext extends RenderContext {

    private long platformWindowSurface, canvasSurface;
    private VkInstance platformWindowInstance, canvasInstance;
    private RenderSettings renderSettings;

    public VulkanRenderContext(RenderSettings renderSettings) {
        this.renderSettings = renderSettings;
    }

    @Override
    public void enableHints() {
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    }

    @Override
    public void setup(PlatformWindow window) {

    }

    @Override
    public void swapBuffers(PlatformWindow window) {

    }

    @Override
    public void readyDisplay(PlatformWindow window) {
        platformWindowInstance = VulkanUtil.createInstance(getClass().getName(), "Fori", renderSettings.enableValidation, SurfaceType.PlatformWindow);
        platformWindowSurface = VulkanUtil.createSurface(platformWindowInstance, window);
    }



    public long getPlatformWindowSurface() {
        return platformWindowSurface;
    }
    public VkInstance getPlatformWindowInstance() {
        return platformWindowInstance;
    }
}
