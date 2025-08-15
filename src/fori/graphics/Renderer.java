package fori.graphics;

import fori.Logger;
import fori.Surface;
import fori.graphics.vulkan.VulkanRenderContext;
import fori.graphics.vulkan.VulkanRenderer;
import fori.graphics.vulkan.VulkanSemaphore;
import org.lwjgl.vulkan.VkInstance;

import java.util.ArrayList;
import java.util.List;

public abstract class Renderer extends Disposable {

    protected int width;
    protected int height;
    private static RenderAPI api;
    protected RendererSettings settings;
    protected int maxFramesInFlight;
    protected Surface surface;
    protected RenderTarget swapchainRenderTarget;
    protected Semaphore[] frameStartSemaphores;
    protected int frameIndex;




    public Renderer(Disposable parent, int width, int height, int maxFramesInFlight, RendererSettings settings, Surface surface){
        super(parent);
        this.width = width;
        this.height = height;
        this.settings = settings;
        this.maxFramesInFlight = maxFramesInFlight;
        this.surface = surface;

    }

    public RendererSettings getSettings() {
        return settings;
    }

    public abstract StaticMeshBatch newStaticMeshBatch(int maxVertices, int maxIndices, int maxTransforms, ShaderProgram shaderProgram);
    public abstract void destroyStaticMeshBatch(StaticMeshBatch staticMeshBatch);

    public abstract DynamicMesh newDynamicMesh(int maxVertexCount, int maxIndexCount, ShaderProgram shaderProgram);
    public abstract void destroyDynamicMesh(DynamicMesh dynamicMesh);

    public abstract void updateRenderer(boolean surfaceInvalidated);
    public abstract void render(RenderGraph renderGraph);
    public Semaphore[] getRenderStartSemaphores() {
        return frameStartSemaphores;
    }


    public int getFrameIndex() { return frameIndex; }
    public int getMaxFramesInFlight() { return maxFramesInFlight; }
    public abstract String getDeviceName();

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
    public abstract void waitForDevice();
    public static Renderer newRenderer(Surface surface, int width, int height, RendererSettings settings){
        api = settings.backend;


        if(settings.backend == RenderAPI.Vulkan){
            VulkanRenderContext vkContext = new VulkanRenderContext();
            vkContext.readyDisplay(surface);

            long vkSurface = vkContext.getVkSurface();
            VkInstance instance = vkContext.getInstance();

            return new VulkanRenderer(surface, instance, vkSurface, width, height, settings, vkContext.getDebugMessenger(), surface);
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
    public RenderTarget getSwapchainRenderTarget() { return swapchainRenderTarget; }

}
