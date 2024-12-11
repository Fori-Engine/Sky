package editor;

import fori.Stage;
import fori.Surface;

import javax.swing.*;
import java.awt.*;

public class SceneViewportPanel extends ToolPanel {

    private Stage stage;
    private Surface surface;
    private Timer timer;

    public SceneViewportPanel(String title, Stage stage) {
        super(title);
        this.stage = stage;

        JFrame frame = ForiEd.getEditorInstance().getFrame();
        Canvas canvas = new Canvas();
        surface = Surface.newAWTSurface(stage.getStageRef(), 30, 30, frame, canvas);

        add(canvas, BorderLayout.CENTER);

    }

    @Override
    public void init() {
        JFrame frame = ForiEd.getEditorInstance().getFrame();

        frame.pack();
        stage.launch(ForiEd.getEditorArgs(), surface);

        timer = new Timer(1000 / 30, e -> stage.update());
        timer.start();
    }
}
