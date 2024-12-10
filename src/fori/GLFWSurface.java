package fori;
import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

import fori.graphics.DebugUtil;
import fori.graphics.Ref;
import fori.graphics.RenderAPI;
import fori.graphics.vulkan.VkRenderer;
import org.joml.Vector2f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.Set;


public class GLFWSurface extends Surface {

    private long handle;
    private Cursor cursor;

    @Override
    public void requestRenderAPI(RenderAPI api) {
        if(api == RenderAPI.Vulkan) {
            try (MemoryStack stack = stackPush()) {
                vkInstance = createInstance(title, List.of("VK_LAYER_KHRONOS_validation"));
                LongBuffer pSurface = stack.mallocLong(1);
                glfwCreateWindowSurface(vkInstance, handle,
                        null, pSurface);
                vkSurface = pSurface.get(0);
            }
        }
    }

    public GLFWSurface(Ref parent, String title, int width, int height, boolean resizable) {
        super(parent, title, width, height, resizable);


        if (!glfwInit()) {
            throw new RuntimeException(Logger.error(GLFWSurface.class, "Failed to initialize GLFW"));
        }

        GLFWErrorCallback.createPrint(System.err).set();
        glfwWindowHint(GLFW_RESIZABLE, glfwBool(resizable));
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        handle = glfwCreateWindow(width, height, title, NULL, NULL);
    }

    @Override
    public PointerBuffer getVulkanInstanceExtensions() {
        return glfwGetRequiredInstanceExtensions();
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
    public void setCursor(Cursor cursor) {
        if(this.cursor != cursor) {
            switch (cursor) {
                case Default -> glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_ARROW_CURSOR));
                case ResizeWE -> glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_RESIZE_EW_CURSOR));
            }
            this.cursor = cursor;
        }
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
