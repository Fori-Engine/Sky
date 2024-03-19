package lake.demo;

import lake.graphics.Renderer2D;
import lake.graphics.RendererType;
import lake.graphics.StandaloneWindow;
import lake.graphics.vulkan.VkRenderer2D;

public class VulkanDemo {



    public static void main(String[] args) {


        StandaloneWindow window = new StandaloneWindow(640, 480, "Vulkan Demo", false, false);
        Renderer2D renderer2D = Renderer2D.createRenderer(RendererType.VULKAN, window,640, 480, true);



        while(!window.shouldClose()){
            System.out.println(window.getFPS());

            renderer2D.render();

            window.update();
        }




        //TODO Remove this!
        ((VkRenderer2D) renderer2D).destroyVulkanObjects();



        window.close();

    }



}
