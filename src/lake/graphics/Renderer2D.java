package lake.graphics;

import lake.FlightRecorder;
import lake.graphics.vulkan.VulkanRenderContext;
import lake.graphics.vulkan.VulkanRenderer2D;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.vulkan.VkInstance;

import java.util.ArrayList;
import java.util.Map;

public abstract class Renderer2D implements Disposable {

    private int width;
    private int height;
    private static RenderAPI api;



    public Renderer2D(int width, int height, RenderSettings renderSettings){
        this.width = width;
        this.height = height;
        Disposer.add("renderer", this);
    }
    public abstract void onResize(int width, int height);

    public abstract void update();

    public static Renderer2D newRenderer2D(PlatformWindow window, int width, int height, RenderSettings settings){
        api = settings.backend;


        if(settings.backend == RenderAPI.Vulkan){

            VulkanRenderContext vulkanContext = new VulkanRenderContext(settings);
            window.configureAndCreateWindow(vulkanContext);
            vulkanContext.readyDisplay(window);

            long surface = vulkanContext.getPlatformWindowSurface();
            VkInstance instance = vulkanContext.getPlatformWindowInstance();

            VulkanRenderer2D vulkanRenderer2D = new VulkanRenderer2D(instance, surface, width, height, settings);
            window.onRenderContextReady(vulkanContext, vulkanRenderer2D);

            return vulkanRenderer2D;
        }
        else if(settings.backend == null){
            FlightRecorder.meltdown(Renderer2D.class, "The target graphics API was not specified in RenderSettings!");
        }
        else {
            FlightRecorder.meltdown(Renderer2D.class, "User requested renderer backend " + settings.backend + " but no backend could be initialized!");
        }



        return null;
    }
    public static RenderAPI getRenderAPI() {
        return api;
    }
}
