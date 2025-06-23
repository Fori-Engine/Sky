package editor;

import fori.Stage;
import fori.Surface;
import org.lwjgl.system.Configuration;


public class Launcher {
    public static void main(String[] args) {
        Configuration.DEBUG.set(true);
        Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);

        Stage stage = new RuntimeStage();
        Surface surface = Surface.newSurface(stage.getStageRef(), "Runtime", 1920, 1080);

        stage.launch(args, surface);


        while(true){
            boolean success = stage.update();

            if(!success) break;
        }

        stage.closing();
        stage.close();


    }
}
