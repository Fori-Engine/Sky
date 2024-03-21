package lake.demo;

import lake.graphics.Renderer2D;
import lake.graphics.RendererType;
import lake.graphics.StandaloneWindow;

public class VulkanDemo {



    public static void main(String[] args) {


        StandaloneWindow window = new StandaloneWindow(640, 480, "Vulkan Demo", false, false);
        Renderer2D renderer2D = Renderer2D.createRenderer(RendererType.VULKAN, window,640, 480, true);



        while(!window.shouldClose()){
            System.out.println(window.getFPS());

            renderer2D.render();

            window.update();
        }



        window.close();

    }



}
