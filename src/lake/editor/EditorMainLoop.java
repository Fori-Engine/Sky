package lake.editor;

import lake.graphics.StandaloneWindow;

public class EditorMainLoop {

    public static void start(String[] args){

        boolean quit = false;

        while(!quit){
            StandaloneWindow window = new StandaloneWindow(1920, 1080, "LakeEditor");
            LakeEditor lakeEditor = new LakeEditor(window);

            while(true){


                lakeEditor.update();

                if(!window.shouldClose()) {

                    if (lakeEditor.isRestartRequested()) {
                        lakeEditor.destroy();
                        window.close();
                        lakeEditor.setRestartRequested(false);
                        break;
                    }

                    window.update();
                }
                else {
                    //This leaves the process running somehow lol
                    lakeEditor.destroy();
                    quit = true;
                    window.close();



                }


            }
        }


        System.out.println("I'm outta here");

    }
}
