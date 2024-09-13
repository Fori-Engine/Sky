package lake.graphics;

import lake.FlightRecorder;
import lake.graphics.vulkan.VkRenderContext;
import lake.graphics.vulkan.VkSceneRenderer;
import org.lwjgl.vulkan.VkInstance;

public abstract class SceneRenderer implements Disposable {

    protected int width;
    protected int height;
    private static RenderAPI api;



    public SceneRenderer(int width, int height, RenderSettings renderSettings){
        this.width = width;
        this.height = height;
        Disposer.add("renderer", this);
    }
    public abstract void onResize(int width, int height);

    public abstract void update();

    public static SceneRenderer newSceneRenderer(PlatformWindow window, int width, int height, RenderSettings settings){
        api = settings.backend;


        if(settings.backend == RenderAPI.Vulkan){

            VkRenderContext vulkanContext = new VkRenderContext(settings);
            window.configureAndCreateWindow(vulkanContext);
            vulkanContext.readyDisplay(window);

            long surface = vulkanContext.getPlatformWindowSurface();
            VkInstance instance = vulkanContext.getPlatformWindowInstance();

            VkSceneRenderer vulkanRenderer2D = new VkSceneRenderer(instance, surface, width, height, settings, vulkanContext.getDebugMessenger());
            window.onRenderContextReady(vulkanContext, vulkanRenderer2D);

            return vulkanRenderer2D;
        }
        else if(settings.backend == null){
            FlightRecorder.meltdown(SceneRenderer.class, "The target graphics API was not specified in RenderSettings!");
        }
        else {
            FlightRecorder.meltdown(SceneRenderer.class, "User requested renderer backend " + settings.backend + " but no backend could be initialized!");
        }



        return null;
    }
    public static RenderAPI getRenderAPI() {
        return api;
    }
}
