package fori.graphics.vulkan;

import fori.Logger;
import fori.Surface;
import fori.GLFWSurface;
import fori.graphics.RenderAPI;
import fori.graphics.RenderContext;

import fori.graphics.DebugUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.jawt.JAWTFunctions;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

public class VkRenderContext extends RenderContext {

    private long vkSurface;
    private VkInstance instance;
    private Surface surface;


    public VkRenderContext() {
    }



    @Override
    public void readyDisplay(Surface surface) {
        this.surface = surface;
        if(!surface.supportsRenderAPI(RenderAPI.Vulkan)) {
            throw new RuntimeException(Logger.error(VkRenderContext.class, "The surface does not support Vulkan"));
        }

        instance = surface.getVulkanInstance();
        vkSurface = surface.getVulkanSurface(instance);
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
