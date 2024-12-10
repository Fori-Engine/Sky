package fori.graphics;

import fori.Surface;
import org.joml.Vector2f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRDynamicRendering;
import org.lwjgl.vulkan.VkInstance;

import java.awt.*;
import java.util.List;

public class AWTSurface extends Surface {
    private long surface;
    private VkInstance vkInstance;
    private java.awt.Component component;
    private float dpiScaleFactor;

    public AWTSurface(Ref parent, String title, int width, int height, boolean resizable) {
        super(parent, title, width, height, resizable);

        vkInstance = createInstance(title, List.of("VK_LAYER_KHRONOS_validation"));

        float resolution = Toolkit.getDefaultToolkit().getScreenResolution();
        dpiScaleFactor = resolution != 100.0f ? (resolution / 96.0f) : resolution;
    }



    @Override
    public int getWidth() {
        return (int) (component.getWidth() * dpiScaleFactor);
    }

    @Override
    public int getHeight() {
        return (int) (component.getHeight() * dpiScaleFactor);
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
    public long getVulkanSurface(VkInstance instance) {
        return surface;
    }

    @Override
    public VkInstance getVulkanInstance() {
        return vkInstance;
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
    public void update() {

    }

    @Override
    public boolean shouldClose() {
        return false;
    }

    @Override
    public void dispose() {

    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public void setVulkanSurface(long surface) {
        this.surface = surface;
    }
}
