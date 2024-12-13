package fori;

import editor.awt.AWTVK;
import fori.graphics.Ref;
import fori.graphics.RenderAPI;
import fori.graphics.vulkan.VkContextManager;
import org.joml.Vector2f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;

import java.awt.*;
import java.util.List;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;

public class AWTSurface extends Surface {
    private Canvas canvas;
    private float dpiScaleFactor;

    public AWTSurface(Ref parent, String title, int width, int height, boolean resizable, Canvas canvas) {
        super(parent, title, width, height, resizable);
        this.canvas = canvas;
        float resolution = Toolkit.getDefaultToolkit().getScreenResolution();
        dpiScaleFactor = resolution != 100.0f ? (resolution / 96.0f) : resolution;
    }


    @Override
    public void requestRenderAPI(RenderAPI api) {
        if(api == RenderAPI.Vulkan) {
            vkInstance = createInstance(title, List.of("VK_LAYER_KHRONOS_validation"));

            try {
                vkSurface = AWTVK.create(canvas, getVulkanInstance());
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public int getWidth() {
        return (int) (canvas.getWidth() * dpiScaleFactor);
    }

    @Override
    public int getHeight() {
        return (int) (canvas.getHeight() * dpiScaleFactor);
    }

    @Override
    public boolean getKeyPressed(int key) {
        return false;
    }

    @Override
    public boolean getKeyReleased(int key) {
        return false;
    }

    @Override
    public boolean getMousePressed(int button) {
        return false;
    }

    @Override
    public boolean getMouseReleased(int button) {
        return false;
    }

    @Override
    public Vector2f getMousePos() {
        return null;
    }

    @Override
    public void setCursor(Cursor cursor) {

    }

    @Override
    public PointerBuffer getVulkanInstanceExtensions() {

        PointerBuffer pointerBuffer = MemoryUtil.memAllocPointer(2);
        pointerBuffer.put(0, MemoryUtil.memUTF8("VK_KHR_surface"));
        pointerBuffer.put(1, MemoryUtil.memUTF8("VK_KHR_win32_surface"));


        return pointerBuffer;
    }


    @Override
    public boolean supportsRenderAPI(RenderAPI api) {
        if(api == RenderAPI.Vulkan) return true;

        return false;
    }

    @Override
    public void display() {

    }

    @Override
    public boolean update() {

        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkSurfaceCapabilitiesKHR surfaceCapabilitiesKHR = VkSurfaceCapabilitiesKHR.calloc(stack);
            if(vkGetPhysicalDeviceSurfaceCapabilitiesKHR(VkContextManager.getCurrentPhysicalDevice(), vkSurface, surfaceCapabilitiesKHR) == VK_ERROR_SURFACE_LOST_KHR) {
                vkDeviceWaitIdle(VkContextManager.getCurrentDevice());
                vkSurface = AWTVK.create(canvas, getVulkanInstance());
                System.out.println("Recreated surface");

                return true;
            }




        } catch (AWTException e) {
            throw new RuntimeException(e);
        }


        canvas.repaint();

        return false;
    }

    @Override
    public boolean shouldClose() {
        return false;
    }

    @Override
    public void dispose() {

    }
}
