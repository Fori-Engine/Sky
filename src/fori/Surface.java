package fori;

import fori.graphics.DebugUtil;
import fori.graphics.Disposable;
import fori.graphics.Ref;
import fori.graphics.RenderAPI;
import fori.graphics.vulkan.VulkanRenderer;
import org.joml.Vector2f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

public abstract class Surface implements Disposable {
    protected String title;
    protected int width, height;
    protected boolean resizable;
    protected Ref ref;
    protected Vector2f cursorPos = new Vector2f();


    //Vulkan specific stuff
    protected long vkDebugMessenger;
    protected long vkSurface;
    protected VkInstance vkInstance;
    protected VkDebugUtilsMessengerCallbackEXT vkDebugUtilsMessengerCallbackEXT;

    public abstract void requestRenderAPI(RenderAPI api);


    public enum Cursor {
        Default,
        ResizeWE
    }

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
    public abstract void setCursor(Cursor cursor);


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
    public long getVulkanSurface() {
        return vkSurface;
    }
    public VkInstance getVulkanInstance() {
        return vkInstance;
    }

    protected abstract VkInstance createInstance(String appName, List<String> validationLayers);



    public long getVulkanDebugMessenger() {
        return vkDebugMessenger;
    }

    public abstract boolean supportsRenderAPI(RenderAPI api);
    public abstract void display();
    public abstract boolean update();
    public abstract boolean shouldClose();



    @Override
    public Ref getRef() {
        return ref;
    }



}



