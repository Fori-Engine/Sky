package fori;

import fori.graphics.Disposable;
import fori.graphics.Ref;
import fori.graphics.RenderAPI;
import fori.graphics.Renderer;
import org.joml.Vector2f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkInstance;

public abstract class Surface implements Disposable {
    protected String title;
    protected int width, height;
    protected boolean resizable;
    protected Ref ref;
    protected Vector2f cursorPos = new Vector2f();

    public Surface(Ref parent, String title, int width, int height, boolean resizable) {
        ref = parent.add(this);
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


    public abstract boolean getKeyPressed(int key);
    public abstract boolean getKeyReleased(int key);
    public abstract boolean getMousePressed(int button);
    public abstract boolean getMouseReleased(int button);
    public abstract Vector2f getMousePos();


    public static Surface newSurface(Ref parent, String title, int width, int height) {
        return newSurface(parent, title, width, height, true);
    }
    public static Surface newSurface(Ref parent, String title, int width, int height, boolean resizable) {
        return new GLFWSurface(parent, title, width, height, resizable);
    }



    public boolean isResizable() {
        return resizable;
    }
    public abstract PointerBuffer getVulkanInstanceExtensions();
    public abstract long getVulkanSurface(VkInstance instance);
    public abstract boolean supportsRenderAPI(RenderAPI api);
    public abstract void display();
    public abstract void update();
    public abstract boolean shouldClose();


    @Override
    public Ref getRef() {
        return ref;
    }



}



