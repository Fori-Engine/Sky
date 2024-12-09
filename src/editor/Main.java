package editor;

import fori.Stage;
import fori.Surface;

public class Main {
    public static void main(String[] args) {


        Stage stage = new EditorStage();
        Surface surface = Surface.newSurface(stage.getStageRef(), "Hello Fori!", 1920, 1080);

        stage.launch(args, surface);


        while(true){
            boolean success = stage.update();

            if(!success) break;
        }

        stage.close();


    }
}
