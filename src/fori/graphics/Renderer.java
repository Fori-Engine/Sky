package fori.graphics;

import fori.Logger;
import fori.Surface;
import fori.ecs.Scene;
import fori.graphics.vulkan.VulkanRenderContext;
import fori.graphics.vulkan.VulkanRenderer;
import org.lwjgl.vulkan.VkInstance;

public abstract class Renderer extends Disposable {

    protected int width;
    protected int height;
    private static RenderAPI api;
    protected RendererSettings settings;
    protected int maxFramesInFlight;
    protected Surface surface;
    protected RenderTarget swapchainRenderTarget;



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

    public abstract SpriteBatch newSpriteBatch(int maxVertexCount, int maxIndexCount, ShaderProgram shaderProgram, Camera camera);


    public abstract void dispatch(Scene scene, SpriteBatch spriteBatche, boolean recreateRenderer);
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
