package editor;

import fori.Stage;
import fori.Surface;
import fori.graphics.AWTSurface;
import fori.graphics.vulkan.VkRenderContext;
import org.lwjgl.vulkan.awt.AWTVKCanvas;
import org.lwjgl.vulkan.awt.VKData;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {


        Stage stage = new EditorStage();


        JFrame frame = new JFrame("AWT test");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setPreferredSize(new Dimension(600, 600));

        Surface awtSurface = new AWTSurface(stage.getStageRef(), "AWT", 1000, 800, false);

        VKData vkData = new VKData();
        vkData.instance = VkRenderContext.createInstance("Fori", List.of(""), awtSurface);

        Canvas canvas = new AWTVKCanvas() {
            @Override
            public void initVK() {
                stage.launch(args, awtSurface);
            }

            @Override
            public void paintVK() {
              stage.update();
            }
        };
        frame.add(canvas, BorderLayout.CENTER);
        frame.pack(); // Packing causes the canvas to be lockable, and is the earliest time it can be used

        frame.setVisible(true);


    }
}
