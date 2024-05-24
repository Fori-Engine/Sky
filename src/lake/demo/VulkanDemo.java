package lake.demo;

import lake.FlightRecorder;
import lake.Time;
import lake.asset.AssetPack;
import lake.asset.AssetPacks;
import lake.asset.TextureData;
import lake.graphics.*;

import java.io.File;
import java.util.Arrays;

public class VulkanDemo {



    public static void main(String[] args) {

        FlightRecorder.setEnabled(true);


        AssetPacks.open("core", AssetPack.openLocal(new File("assets")));



        Window window = new Window(640, 480, "Vulkan Demo", false);
        Renderer2D renderer2D = Renderer2D.newRenderer2D(window, window.getWidth(), window.getHeight(), new RenderSettings(RenderAPI.Vulkan).msaa(true));





        Texture2D texture2D = Texture2D.newTexture2D(AssetPacks.getAsset("core:assets/logo.png"), Texture2D.Filter.Linear);


        window.setTitle(renderer2D.getDeviceName());

        float t = 0;
        while(!window.shouldClose()){
            renderer2D.clear(Color.BLACK);



            renderer2D.drawFilledRect(0, 0, 100, 100, Color.BLUE);

            renderer2D.drawFilledRect(renderer2D.getWidth() - 100, 0, 100, 100, Color.LIGHT_GRAY);

            renderer2D.setOrigin(640 / 2f, 480 / 2f);
            renderer2D.rotate((float) Math.toRadians(Math.sin(t / 100) * 360));
            renderer2D.drawFilledRect((640 / 2f) - (250 / 2f), (480 / 2f) - (250 / 2f), 250, 250, Color.RED);
            renderer2D.resetTransform();
            renderer2D.setOrigin(0, 0);


            renderer2D.drawText(0, 0, "FPS: " + Time.framesPerSecond(), Color.GREEN, Font2D.getDefault());

            renderer2D.drawTexture(60,60, 100, 100, texture2D);



            renderer2D.render();

            t += 100 * Time.deltaTime;



            window.update();
        }



        window.close();

    }



}
