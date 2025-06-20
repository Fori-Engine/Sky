package fori.graphics.vulkan;

import fori.Logger;
import fori.Surface;
import fori.graphics.RenderAPI;
import fori.graphics.RenderContext;

import org.lwjgl.vulkan.*;

import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;

public class VulkanRenderContext extends RenderContext {

    private long vkSurface;
    private VkInstance instance;
    private Surface surface;


    public VulkanRenderContext() {
    }



    @Override
    public void readyDisplay(Surface surface) {
        this.surface = surface;
        if(!surface.supportsRenderAPI(RenderAPI.Vulkan)) {
            throw new RuntimeException(Logger.error(VulkanRenderContext.class, "The surface does not support Vulkan"));
        }
        surface.requestRenderAPI(RenderAPI.Vulkan);

        instance = surface.getVulkanInstance();
        vkSurface = surface.getVulkanSurface();
    }

    public long getDebugMessenger() {
        return surface.getVulkanDebugMessenger();
    }



    public long getVkSurface() {
        return vkSurface;
    }
    public VkInstance getInstance() {
        return instance;
    }
}
