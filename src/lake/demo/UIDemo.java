package lake.demo;

import lake.FlightRecorder;
import lake.graphics.*;
import lake.graphics.vulkan.LVKRenderer2D;

import static lake.graphics.ui.IngridUI.*;

public class UIDemo {
    public static void main(String[] args) {

        FlightRecorder.setEnabled(true);

        StandaloneWindow window = new StandaloneWindow(1920, 1080, "UI Demo", false, true);
        Renderer2D renderer2D = Renderer2D.createRenderer(RendererType.OPENGL, window, window.getWidth(), window.getHeight(), new RenderSettings().msaa(true));


        float value = 0;


        int i = 0;
        while(!window.shouldClose()){
            renderer2D.clear(Color.WHITE);




            beginContext(renderer2D, window, "HUD");

            //Entire Screen
            startPanel("edgeLayout", new Color(0, 0, 0, 1));
            {

                startPanel("edgeLayout", Color.BLUE, Horizontal);
                {

                    if (button("This is a button", East)) {
                        System.out.println("Shut up please " + i++);
                    }

                    button("Hey I just met you and this is crazy, \nbut here's my number so call me maybe?", West);
                }
                endPanel(North);

                startPanel("lineLayout", Color.RED, Vertical);
                {
                    button("This is a test");


                    startPanel("lineLayout", Color.BLUE, Horizontal);
                    {

                        text("A slider");
                        value = slider("This is a slider", value, 0, 10, false);
                    }
                    endPanel();

                    startPanel("lineLayout", Color.BLUE, Horizontal);
                    {

                        text("A button");
                        if(button("Quit")){
                            System.exit(1);
                        }
                    }
                    endPanel();

                    startPanel("lineLayout", Color.BLUE, Horizontal);
                    {

                        text("A slider");
                        value = slider("This is a slider", value, 0, 10, false);
                    }
                    endPanel();

                    startPanel("lineLayout", Color.BLUE, Horizontal);
                    {

                        text("A button");
                        if(button("Quit")){
                            System.exit(1);
                        }
                    }
                    endPanel();

                    startPanel("lineLayout", Color.BLUE, Horizontal);
                    {

                        text("A slider");
                        value = slider("This is a slider", value, 0, 10, false);
                    }
                    endPanel();

                    startPanel("lineLayout", Color.BLUE, Horizontal);
                    {

                        text("A button");
                        if(button("Quit")){
                            System.exit(1);
                        }
                    }
                    endPanel();

                    startPanel("lineLayout", Color.BLUE, Horizontal);
                    {

                        text("A slider");
                        value = slider("This is a slider", value, 0, 10, false);
                    }
                    endPanel();


                    startPanel("lineLayout", Color.BLUE, Horizontal);
                    {

                        text("A slider");
                        value = slider("This is a slider", value, 0, 10, false);
                    }
                    endPanel();

                    startPanel("lineLayout", Color.BLUE, Horizontal);
                    {

                        text("A button");
                        if(button("Quit")){
                            System.exit(1);
                        }
                    }
                    endPanel();

                    startPanel("lineLayout", Color.BLUE, Horizontal);
                    {

                        text("A slider");
                        value = slider("This is a slider", value, 0, 10, false);
                    }
                    endPanel();

                    startPanel("lineLayout", Color.BLUE, Horizontal);
                    {

                        text("A button");
                        if(button("Quit")){
                            System.exit(1);
                        }
                    }
                    endPanel();




                }
                endPanel(West);

                startPanel("lineLayout", Color.GREEN, Vertical);
                {
                    button("Click");
                    text("More testing...");
                    value = slider("This is a slider", value, 0, 10, true);
                }
                endPanel(East);



                startPanel("lineLayout", Color.BLUE, Horizontal);
                {
                    if(button("There are many like it, but this one is mine", West)){
                        System.out.println("Shut up please " + i++);
                    }
                }
                endPanel(South);






            }
            endPanel();


            renderUI();
            endContext();














            renderer2D.render();
            window.update();
        }
        window.close();

    }
}
