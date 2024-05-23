package lake.demo;

import lake.Time;
import lake.asset.AssetPack;
import lake.asset.AssetPacks;
import lake.graphics.*;
import org.joml.Vector2f;

import java.io.File;

public class TestPlatform {
    public static void main(String[] args) {
        AssetPacks.open("core", AssetPack.openPack(new File("assets.pkg")));
        Window window = new Window(1920, 1080, "LakeEngine Test Platform", false);
        Renderer2D renderer2D = Renderer2D.newRenderer2D(window, window.getWidth(), window.getHeight(), new RenderSettings(RenderAPI.Vulkan));

        Texture2D logo = Texture2D.newTexture2D(AssetPacks.getAsset("core:assets/logo.png"), Texture2D.Filter.Linear);
        Vector2f logoCenter = new Vector2f(600, 600);

        Font2D font = Font2D.getDefault();

        while(!window.shouldClose()){
            renderer2D.clear(Color.LIGHT_GRAY);


            renderer2D.drawText(0, 0,
                    "Welcome to LakeEngine!\n" +
                    "FPS: " + Time.framesPerSecond() + "\n" +
                    "Renderer Backend: " + Renderer2D.getRenderAPI()  + "\n"

                    , Color.RED, font);

            renderer2D.drawTexture(logoCenter.x, logoCenter.y, 300, 300, logo);




            renderer2D.render();
            window.update();
        }
        window.close();









    }
}
