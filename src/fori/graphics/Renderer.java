package fori.graphics;

import fori.Logger;
import fori.Surface;
import fori.graphics.vulkan.VkRenderContext;
import fori.graphics.vulkan.VkRenderer;
import org.lwjgl.vulkan.VkInstance;

import java.util.ArrayList;
import java.util.List;

public abstract class Renderer implements Disposable {

    protected int width;
    protected int height;
    private static RenderAPI api;
    protected RendererSettings settings;
    protected List<RenderQueue> renderQueues = new ArrayList<>();
    protected Ref ref;
    protected int maxFramesInFlight;



    public Renderer(Ref parent, int width, int height, int maxFramesInFlight, RendererSettings settings){
        this.ref = parent.add(this);
        this.width = width;
        this.height = height;
        this.settings = settings;
        this.maxFramesInFlight = maxFramesInFlight;
    }
    public abstract void onSurfaceResized(int width, int height);


    public abstract RenderQueue newRenderQueue(RenderQueueFlags renderQueueFlags);
    public RenderQueue getRenderQueueByShaderProgram(ShaderProgram shaderProgram){
        for(RenderQueue renderQueue : renderQueues){
            if(renderQueue.getShaderProgram() == shaderProgram) return renderQueue;
        }
        return null;
    }
    public abstract void removeQueue(RenderQueue renderQueue);
    public abstract void update();
    public abstract int getFrameIndex();
    public int getMaxFramesInFlight() { return maxFramesInFlight; }
    public abstract String getDeviceName();

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
    public abstract void waitForDevice();
    public abstract int getMaxRenderQueueCount();
    public static Renderer newRenderer(Ref parent, Surface surface, int width, int height, RendererSettings settings){
        api = settings.backend;


        if(settings.backend == RenderAPI.Vulkan){
            VkRenderContext vkContext = new VkRenderContext();
            vkContext.readyDisplay(surface);

            long vkSurface = vkContext.getVkSurface();
            VkInstance instance = vkContext.getInstance();

            return new VkRenderer(parent, instance, vkSurface, width, height, settings, vkContext.getDebugMessenger());
        }
        else if(settings.backend == null){
            Logger.meltdown(Renderer.class, "The target graphics API was not specified in RenderSettings!");
        }
        else {
            Logger.meltdown(Renderer.class, "User requested renderer backend " + settings.backend + " but no backend could be initialized!");
        }



        return null;
    }

    public Ref getRef() { return ref; }

    public static RenderAPI getRenderAPI() {
        return api;
    }

}
