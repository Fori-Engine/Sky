package app;

import engine.Stage;
import engine.Surface;
import engine.asset.AssetPackage;

import java.nio.file.Path;


public class Launcher {
    public static void main(String[] args) {
        //Configuration.DEBUG.set(true);
        //Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);

        Stage stage = new ExampleStage();
        Surface surface = Surface.newSwingSurface(stage, "SkySOFT Editor", 1920, 1080); //Surface.newSurface(stage, "SkySOFT Engine", 1920, 1080);

        stage.launch(args, surface);


        while(true){
            boolean success = stage.update();
            System.out.println("Tick");

            if(!success) break;
        }

        stage.closing();
        stage.close();


    }
}
