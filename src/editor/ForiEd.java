package editor;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import fori.Stage;
import fori.Surface;

import javax.swing.*;
import java.awt.*;

public class ForiEd {

    private JFrame frame;
    private Stage stage;
    private Timer timer;



    public JFrame init(Stage stage, String[] args) {
        FlatDarculaLaf.setup();

        frame = new JFrame("ForiEd (" + System.getProperty("os.name") + " " + System.getProperty("os.arch") + ")");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setPreferredSize(new Dimension(1920, 1080));

        Canvas canvas = new Canvas();
        Surface surface = Surface.newAWTSurface(stage.getStageRef(), 30, 30, frame, canvas);
        frame.add(canvas, BorderLayout.CENTER);
        frame.pack();

        stage.launch(args, surface);

        timer = new Timer(1000 / 30, e -> stage.update());
        timer.start();

        return frame;
    }



    public JFrame getFrame() {
        return frame;
    }

    public void close() {




    }
}
