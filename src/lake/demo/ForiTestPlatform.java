package lake.demo;

import lake.FlightRecorder;
import lake.asset.AssetPack;
import lake.asset.AssetPacks;
import lake.graphics.*;

import java.io.File;


public class ForiTestPlatform {

    public static void main(String[] args) {
        AssetPacks.open("core", AssetPack.openPack(new File("assets.pkg")));

        FlightRecorder.setEnabled(true);

        PlatformWindow window = new PlatformWindow(640, 480, "ForiEngine", true);
        SceneRenderer sceneRenderer = SceneRenderer.newSceneRenderer(window, window.getWidth(), window.getHeight(), new RenderSettings(RenderAPI.Vulkan).msaa(true).enableValidation(true));


        window.setIcon(AssetPacks.getAsset("core:assets/ForiEngine.png"));

        while(!window.shouldClose()){


            sceneRenderer.update();
            window.update();
        }

        window.close();
    }
}
