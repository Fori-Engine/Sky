package editor;

import editor.awt.AWTVK;
import fori.Stage;
import fori.Surface;
import fori.AWTSurface;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {
    private static Timer timer = null;
    public static void main(String[] args) {

        boolean glfw = false;


        if(glfw) {
            Stage stage = new EditorStage();
            Surface surface = Surface.newSurface(stage.getStageRef(), "Window", 1000, 800);

            stage.launch(args, surface);


            while(true){
                boolean success = stage.update();

                if(!success) break;
            }

            stage.close();
        }
        else {
            SwingUtilities.invokeLater(() -> {
                Stage stage = new EditorStage();


                JFrame frame = new JFrame("AWT test");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setLayout(new BorderLayout());
                frame.setPreferredSize(new Dimension(1000, 800));


                Canvas canvas = new Canvas();
                frame.add(canvas, BorderLayout.CENTER);
                canvas.addHierarchyListener(e -> {
                    if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                        if (!e.getComponent().isDisplayable()) {
                            timer.stop();
                            stage.close();
                        }
                    }
                });
                frame.pack();

                Surface surface = Surface.newAWTSurface(stage.getStageRef(), 30, 30, frame, canvas);

                frame.add(new JButton("Lmao"), BorderLayout.NORTH);

                stage.launch(args, surface);



                timer = new Timer(16, e -> stage.update());
                timer.start();
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        System.exit(0);
                    }
                });
                frame.setVisible(true);


            });
        }




    }
}
