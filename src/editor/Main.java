package editor;

import fori.Stage;

public class Main {
    public static void main(String[] args) {


        Stage stage = new EditorStage();

        stage.launch(args);


        while(true){
            boolean success = stage.update();

            if(!success) break;
        }

        stage.close();


    }
}
