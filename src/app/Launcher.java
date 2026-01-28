package app;

import engine.Logger;
import engine.SkyRuntimeException;
import engine.Stage;
import engine.Surface;
import engine.asset.AssetPackage;

import java.io.File;
import java.nio.file.Path;


public class Launcher {


    public void initLogging() {
        Logger.setFileTarget(new File("engine.log"));
    }


    public void launch(String[] args) {
        //Configuration.DEBUG.set(true);
        //Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);



        Stage stage = new ExampleStage();
        Surface surface = Surface.newSurface(stage, "SkySOFT Engine", 1920, 1080);

        stage.launch(args, surface);


        while(true){
            boolean success = stage.update();

            if(!success) break;
        }

        stage.closing();
        stage.close();




    }


}
