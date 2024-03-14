package lake.editor;

import imgui.ImGui;
import lake.FileReader;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.swing.filechooser.FileSystemView;
import java.io.File;

public class MenuBar extends Panel {
    private LakeEditor lakeEditor;
    protected MenuBar(LakeEditor lakeEditor) {
        super("Menu Bar");
        this.lakeEditor = lakeEditor;
    }

    @Override
    public void render() {

        SceneViewport sceneViewport = (SceneViewport) lakeEditor.getPanels().get("sceneViewport");
        ProjectRef projectRef = lakeEditor.getProjectRef();

        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {


                if (ImGui.menuItem("Open Project")) {
                    String projectFilePath =
                            TinyFileDialogs.tinyfd_openFileDialog(
                                    "Open Project",
                                    FileSystemView.getFileSystemView().getHomeDirectory().getPath(),
                                    null,
                                    null,
                                    false
                            );

                    if (projectFilePath != null) {

                        String outputPath = projectFilePath.strip().replace("project.lake", "") + FileReader.readFile(projectFilePath).strip();

                        if (projectRef.openProject(new File(outputPath), sceneViewport.getWidth(), sceneViewport.getHeight())) {
                            sceneViewport.useFramebuffer2D(projectRef.getViewportTextureID());
                        }


                    }

                }

                ImGui.endMenu();
            }
            ImGui.endMainMenuBar();
        }


    }

    @Override
    public void dispose() {

    }
}
