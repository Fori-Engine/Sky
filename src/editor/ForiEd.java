package editor;

import com.formdev.flatlaf.FlatDarculaLaf;
import fori.Stage;
import org.flexdock.docking.DockingConstants;
import org.flexdock.docking.DockingManager;
import org.flexdock.docking.DockingPort;
import org.flexdock.docking.defaults.DefaultDockingPort;

import javax.swing.*;
import java.awt.*;

public class ForiEd {

    private JFrame frame;
    private DefaultDockingPort dockingPort = new DefaultDockingPort();
    private static ForiEd editor;
    private static String[] args;

    public JFrame init(Stage stage, String[] args) {
        editor = this;
        ForiEd.args = args;

        FlatDarculaLaf.setup();

        frame = new JFrame("ForiEd (" + System.getProperty("os.name") + " " + System.getProperty("os.arch") + ")");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setPreferredSize(new Dimension(1400, 800));

        frame.setContentPane(dockingPort);



        ToolPanel sceneHierarchyPanel = new SceneHierarchyPanel("Scene Hierarchy");
        DockingManager.dock(sceneHierarchyPanel, (DockingPort) dockingPort, DockingConstants.WEST_REGION);
        sceneHierarchyPanel.init();

        ToolPanel sceneViewport = new SceneViewportPanel("Scene Viewport", stage);
        DockingManager.dock(sceneViewport, (DockingPort) dockingPort, DockingConstants.EAST_REGION);
        sceneViewport.init();









        return frame;
    }



    public static String[] getEditorArgs() {
        return args;
    }

    public static ForiEd getEditorInstance() {
        return editor;
    }

    public JFrame getFrame() {
        return frame;
    }

    public void close() {




    }
}
