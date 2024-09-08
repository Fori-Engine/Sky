package lake.demo;

import lake.FlightRecorder;
import lake.Time;
import lake.asset.AssetPack;
import lake.asset.AssetPacks;
import lake.graphics.*;

import java.io.File;


public class ForiTestPlatform {

    public static void main(String[] args) {
        AssetPacks.open("core", AssetPack.openPack(new File("assets.pkg")));

        FlightRecorder.setEnabled(true);

        PlatformWindow window = new PlatformWindow(640, 480, "ForiEngine", true);
        Renderer2D renderer2D = Renderer2D.newRenderer2D(window, window.getWidth(), window.getHeight(), new RenderSettings(RenderAPI.Vulkan).msaa(true).enableValidation(true));


        window.setIcon(AssetPacks.getAsset("core:assets/ForiEngine.png"));

        while(!window.shouldClose()){

            renderer2D.update();
            window.update();
        }

        window.close();
    }
}
