package editor;

import fori.Stage;
import fori.Surface;
import org.lwjgl.system.Configuration;


public class Launcher {
    public static void main(String[] args) {

        Stage stage = new RuntimeStage();
        Surface surface = Surface.newSurface(stage.getStageRef(), "Runtime", 1000, 800);

        stage.launch(args, surface);


        while(true){
            boolean success = stage.update();

            if(!success) break;
        }

        stage.close();


    }
}
