package fori.graphics;

import fori.Logger;
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



    public Renderer(int width, int height, RendererSettings settings){
        this.width = width;
        this.height = height;
        this.settings = settings;

        Disposer.add("renderer", this);
    }
    public abstract void onSurfaceResized(int width, int height);
    public RenderQueue newRenderQueue(ShaderProgram shaderProgram){
        return newRenderQueue(shaderProgram, RenderQueue.MAX_VERTEX_COUNT, RenderQueue.MAX_INDEX_COUNT);
    }
    public abstract RenderQueue newRenderQueue(ShaderProgram shaderProgram, int maxVertices, int maxIndices);
    public RenderQueue getRenderQueueByShaderProgram(ShaderProgram shaderProgram){
        for(RenderQueue renderQueue : renderQueues){
            if(renderQueue.shaderProgram == shaderProgram) return renderQueue;
        }
        return null;
    }
    public abstract void removeQueue(RenderQueue renderQueue);
    public abstract void update();
    public abstract int getFrameIndex();

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static Renderer newRenderer(PlatformWindow window, int width, int height, RendererSettings settings){
        api = settings.backend;


        if(settings.backend == RenderAPI.Vulkan){

            VkRenderContext vulkanContext = new VkRenderContext(settings);
            window.configureAndCreateWindow(vulkanContext);
            vulkanContext.readyDisplay(window);

            long surface = vulkanContext.getPlatformWindowSurface();
            VkInstance instance = vulkanContext.getPlatformWindowInstance();

            VkRenderer vulkanRenderer2D = new VkRenderer(instance, surface, width, height, settings, vulkanContext.getDebugMessenger());
            window.onRenderContextReady(vulkanContext, vulkanRenderer2D);

            return vulkanRenderer2D;
        }
        else if(settings.backend == null){
            Logger.meltdown(Renderer.class, "The target graphics API was not specified in RenderSettings!");
        }
        else {
            Logger.meltdown(Renderer.class, "User requested renderer backend " + settings.backend + " but no backend could be initialized!");
        }



        return null;
    }
    public static RenderAPI getRenderAPI() {
        return api;
    }

}
