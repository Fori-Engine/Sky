package editor;

import editor.awt.AWTVK;
import fori.Stage;
import fori.Surface;
import fori.graphics.AWTSurface;
import org.lwjgl.vulkan.awt.VKData;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            Stage stage = new EditorStage();


            JFrame frame = new JFrame("AWT test");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.setPreferredSize(new Dimension(1000, 800));

            Surface awtSurface = new AWTSurface(stage.getStageRef(), "AWT", 1000, 800, false);

            Canvas canvas = new Canvas();

            frame.add(canvas, BorderLayout.CENTER);
            frame.add(new JButton("Lmao"), BorderLayout.NORTH);
            frame.pack();

            long surface = 0;
            try {
                surface = AWTVK.create(canvas, awtSurface.getVulkanInstance());
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }
            ((AWTSurface) awtSurface).setVulkanSurface(surface);
            ((AWTSurface) awtSurface).setComponent(canvas);

            stage.launch(args, awtSurface);



            new Timer(16, e -> stage.update()).start();


            frame.setVisible(true);

        });




    }
}
