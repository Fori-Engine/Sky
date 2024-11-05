package fori;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

import fori.graphics.Ref;
import fori.graphics.RenderAPI;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstance;

import java.nio.LongBuffer;


public class GLFWSurface extends Surface {

    private long handle;
    public GLFWSurface(Ref parent, String title, int width, int height, boolean resizable) {
        super(parent, title, width, height, resizable);


        if (!glfwInit())
            throw new RuntimeException(Logger.error(GLFWSurface.class, "Failed to initialize GLFW"));

        glfwWindowHint(GLFW_RESIZABLE, glfwBool(resizable));
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    }

    @Override
    public PointerBuffer getVulkanInstanceExtensions() {
        return glfwGetRequiredInstanceExtensions();
    }

    @Override
    public long getVulkanSurface(VkInstance instance) {
        long surface;

        try(MemoryStack stack = stackPush()) {

            LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);

            if(glfwCreateWindowSurface(instance, handle, null, pSurface) != VK_SUCCESS) {
                throw new RuntimeException(Logger.error(GLFWSurface.class, "Failed to create a Vulkan surface"));
            }

            surface = pSurface.get(0);
        }

        return surface;
    }

    @Override
    public boolean supportsRenderAPI(RenderAPI api) {
        if(api == RenderAPI.Vulkan) return glfwVulkanSupported();
        return false;
    }



    private int glfwBool(boolean b){
        return b ? GLFW_TRUE : GLFW_FALSE;
    }

    @Override
    public void display() {
        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL)
            throw new RuntimeException(Logger.error(GLFWSurface.class, "Failed to create GLFW window"));
        glfwShowWindow(handle);
    }

    @Override
    public void update() {
        glfwPollEvents();
    }

    @Override
    public int getWidth() {
        int[] width = new int[1], height = new int[1];
        glfwGetWindowSize(handle, width, height);
        return width[0];
    }

    @Override
    public int getHeight() {
        int[] width = new int[1], height = new int[1];
        glfwGetWindowSize(handle, width, height);
        return height[0];
    }

    @Override
    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    @Override
    public void dispose() {
        glfwDestroyWindow(handle);
        glfwTerminate();
    }


}
