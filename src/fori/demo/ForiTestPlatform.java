package fori.demo;

import fori.Logger;
import fori.asset.AssetPack;
import fori.asset.AssetPacks;
import fori.graphics.*;

import java.io.File;


public class ForiTestPlatform {

    public static void main(String[] args) {
        AssetPacks.open("core", AssetPack.openPack(new File("assets.pkg")));

        Logger.setConsoleTarget(System.out);

        Logger.meltdown(ForiTestPlatform.class, "I'm lazy");

        PlatformWindow window = new PlatformWindow(640, 480, "ForiEngine", true);
        SceneRenderer sceneRenderer = SceneRenderer.newSceneRenderer(window, window.getWidth(), window.getHeight(), new RendererSettings(RenderAPI.Vulkan).validation(true).vsync(false));
        window.setIcon(AssetPacks.getAsset("core:assets/ForiEngine.png"));







        while(!window.shouldClose()){

            //System.out.println(Time.framesPerSecond());

            sceneRenderer.update();
            window.update();
        }

        window.close();
    }
}
