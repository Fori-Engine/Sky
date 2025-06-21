package fori.graphics;

import fori.Logger;
import fori.Surface;
import fori.graphics.aurora.DynamicMesh;
import fori.graphics.aurora.StaticMeshBatch;
import fori.graphics.vulkan.VulkanRenderContext;
import fori.graphics.vulkan.VulkanRenderer;
import org.lwjgl.vulkan.VkInstance;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class Renderer implements Disposable {

    protected int width;
    protected int height;
    private static RenderAPI api;
    protected RendererSettings settings;
    protected Ref ref;
    protected int maxFramesInFlight;
    protected Surface surface;
    protected Map<ShaderProgram, StaticMeshBatch> staticMeshBatches;
    protected Map<ShaderProgram, DynamicMesh> dynamicMeshes;


    public Renderer(Ref parent, int width, int height, int maxFramesInFlight, RendererSettings settings, Surface surface){
        this.ref = parent.add(this);
        this.width = width;
        this.height = height;
        this.settings = settings;
        this.maxFramesInFlight = maxFramesInFlight;
        this.surface = surface;

        staticMeshBatches = new HashMap<>();
        dynamicMeshes = new HashMap<>();
    }

    public abstract StaticMeshBatch newStaticMeshBatch(int maxVertices, int maxIndices, int maxTransforms, ShaderProgram shaderProgram);
    public void submitStaticMesh(StaticMeshBatch staticMeshBatch, Mesh mesh, int transformIndex) {
        if(mesh.getType() != MeshType.Static) {
            throw new RuntimeException("Mesh of type " + mesh.getType() + " passed to submitStaticMesh()");
        }

        ByteBuffer vertexBufferData = staticMeshBatch.getDefaultVertexBuffer().get();
        vertexBufferData.clear();

        ByteBuffer indexBufferData = staticMeshBatch.getDefaultIndexBuffer().get();
        indexBufferData.clear();

        mesh.put(
                staticMeshBatch.getVertexCount(),
                staticMeshBatch.getShaderProgram(),
                transformIndex,
                staticMeshBatch.getDefaultVertexBuffer().get(),
                staticMeshBatch.getDefaultIndexBuffer().get()
        );

        staticMeshBatch.updateMeshBatch(
                mesh.getVertexCount(),
                mesh.getIndexCount()
        );
    }
    public abstract DynamicMesh submitDynamicMesh(Mesh mesh, int maxVertexCount, int maxIndexCount, ShaderProgram shaderProgram);



    public abstract void update(boolean recreateRenderer);
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
    public abstract int getMaxStaticMeshBatchCount();
    public static Renderer newRenderer(Ref parent, Surface surface, int width, int height, RendererSettings settings){
        api = settings.backend;


        if(settings.backend == RenderAPI.Vulkan){
            VulkanRenderContext vkContext = new VulkanRenderContext();
            vkContext.readyDisplay(surface);

            long vkSurface = vkContext.getVkSurface();
            VkInstance instance = vkContext.getInstance();

            return new VulkanRenderer(parent, instance, vkSurface, width, height, settings, vkContext.getDebugMessenger(), surface);
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
