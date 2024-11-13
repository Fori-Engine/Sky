package fori;
import static java.awt.SystemColor.window;
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
import org.joml.Vector2f;
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

        double[] x = new double[1], y = new double[1];
        glfwGetCursorPos(handle, x, y);

        cursorPos.set(x[0], y[0]);
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
    public boolean getKeyPressed(int key) {
        return glfwGetKey(handle, key) == GLFW_PRESS;
    }

    @Override
    public boolean getKeyReleased(int key) {
        return glfwGetKey(handle, key) == GLFW_RELEASE;
    }

    @Override
    public boolean getMousePressed(int button) {
        return glfwGetMouseButton(handle, button) == GLFW_PRESS;
    }

    @Override
    public boolean getMouseReleased(int button) {
        return glfwGetMouseButton(handle, button) == GLFW_RELEASE;
    }

    @Override
    public Vector2f getMousePos() {
        return cursorPos;
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
