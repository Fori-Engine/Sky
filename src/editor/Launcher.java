package editor;

import fori.Stage;
import fori.Surface;

import javax.swing.*;
import java.awt.*;

public class Launcher {
    public static void main(String[] args) {

        boolean glfw = false;


        if(glfw) {
            Stage stage = new RuntimeStage();
            Surface surface = Surface.newSurface(stage.getStageRef(), "Runtime", 1000, 800);

            stage.launch(args, surface);


            while(true){
                boolean success = stage.update();

                if(!success) break;
            }

            stage.close();
        }
        else {
            SwingUtilities.invokeLater(() -> {
                Stage stage = new RuntimeStage();

                ForiEd editor = new ForiEd();

                editor.init(stage, args).setVisible(true);


            });
        }




    }
}
