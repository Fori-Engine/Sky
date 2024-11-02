package fori;

import fori.graphics.RenderAPI;
import fori.graphics.Renderer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkInstance;

public abstract class Surface {
    protected String title;
    protected int width, height;
    protected boolean resizable;

    public Surface(String title, int width, int height, boolean resizable) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.resizable = resizable;
    }

    public String getTitle() {
        return title;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isResizable() {
        return resizable;
    }
    public abstract PointerBuffer getVulkanInstanceExtensions();
    public abstract long getVulkanSurface(VkInstance instance);
    public abstract boolean supportsRenderAPI(RenderAPI api);
    public abstract void init();
    public abstract void display();
    public abstract void update();
    public abstract boolean shouldClose();
    public abstract void dispose();

}



