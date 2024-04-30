package lake.demo;

import lake.Time;
import lake.graphics.*;

public class VulkanDemo {



    public static void main(String[] args) {


        StandaloneWindow window = new StandaloneWindow(640, 480, "Vulkan Demo", false, false);
        Renderer2D renderer2D = Renderer2D.createRenderer(window, window.getWidth(), window.getHeight(), new RenderSettings(RendererBackend.Vulkan).msaa(true));



        float t = 0;
        while(!window.shouldClose()){
            renderer2D.clear(Color.BLACK);
            System.out.println(window.getFPS());

            renderer2D.drawFilledRect(0, 0, 100, 100, Color.BLUE);

            renderer2D.drawFilledRect(renderer2D.getWidth() - 100, 0, 100, 100, Color.LIGHT_GRAY);

            renderer2D.setOrigin(640 / 2f, 480 / 2f);
            renderer2D.rotate((float) Math.toRadians(t));
            renderer2D.drawFilledRect((640 / 2f) - (250 / 2f), (480 / 2f) - (250 / 2f), 250, 250, Color.RED);
            renderer2D.resetTransform();
            renderer2D.setOrigin(0, 0);




            renderer2D.render();

            t += 100 * Time.deltaTime;



            window.update();
        }



        window.close();

    }



}
