package lake.editor;

import imgui.type.ImBoolean;
import lake.Utils;
import lake.graphics.Color;
import lake.graphics.Disposer;
import lake.graphics.Texture2D;
import lake.script.EditorUI;
import imgui.ImGui;
import org.joml.Vector3f;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.swing.filechooser.FileSystemView;
import java.lang.reflect.Field;

public class ProjectInspector extends Panel {
    private SceneViewport sceneViewport;
    protected ProjectInspector(SceneViewport sceneViewport) {
        super("Project Inspector");
        this.sceneViewport = sceneViewport;
    }


    private boolean showLog = false;
    @Override
    public void render() {
        ImGui.begin(title);
        {


            ProjectRef projectRef = ProjectManager.getProjectRef();

            if(projectRef.getLastProjectThrowableLog() != null){

                ImGui.textColored(1f, 0f, 0f, 1f, "Error");
                ImGui.sameLine();
                if(ImGui.button("View Project Log")) {
                    showLog = true;
                }

                if(showLog){
                    ImGui.openPopup("The project crashed!");
                    if (ImGui.beginPopupModal("The project crashed!", new ImBoolean(true))) {
                        ImGui.text(Utils.exceptionToString(projectRef.getLastProjectThrowableLog()));

                        if (ImGui.button("Close")) {
                            ImGui.closeCurrentPopup();
                            showLog = false;
                            projectRef.clearLastProjectThrowableLog();
                        }
                        ImGui.endPopup();
                    }
                }
            }

            if(projectRef.isProjectOpened()) {

                ImGui.text(projectRef.getProjectPath().getPath());
                ImGui.separator();


                if (ImGui.button(projectRef.isCurrentProjectPaused() ? "Resume" : "Pause")) {
                    ProjectManager.getProjectRef().setCurrentProjectPaused(!projectRef.isCurrentProjectPaused());
                }

                ImGui.sameLine();

                if (ImGui.button("Reload")) {
                    projectRef.openProject(projectRef.getProjectPath(), sceneViewport.getWidth(), sceneViewport.getHeight());
                    projectRef.setCurrentProjectPaused(false);
                    sceneViewport.useFramebuffer2D(projectRef.getViewportTextureID());
                }



            }



            ImGui.separator();

            for(Object object : EditorUI.getRegistry()){

                ImGui.text(object.getClass().getSimpleName() + " [/" + object + "]");

                for(Field field : object.getClass().getFields()){

                    try {

                        if (int.class.equals(field.getType())) {
                            int i = field.getInt(object);
                            int[] z = new int[]{i};
                            if (ImGui.sliderInt(field.getName(), z, 0, 200)) {
                                field.setInt(object, z[0]);
                            }
                        }

                        if (float.class.equals(field.getType())) {
                            float i = field.getFloat(object);
                            float[] z = new float[]{i};
                            if (ImGui.sliderFloat(field.getName(), z, 0, 200)) {
                                field.setFloat(object, z[0]);
                            }
                        }

                        if (double.class.equals(field.getType())) {
                            double i = field.getDouble(object);
                            float[] z = new float[]{(float) i};
                            if (ImGui.sliderFloat(field.getName(), z, 0, 200)) {
                                field.setDouble(object, z[0]);
                            }
                        }

                        if (Color.class.equals(field.getType())) {
                            Color i = (Color) field.get(object);
                            float[] color = new float[]{i.r, i.g, i.b, i.a};
                            if (ImGui.colorEdit4(field.getName(), color)) {
                                field.set(object, new Color(color[0], color[1], color[2], color[3]));
                            }
                        }

                        if (boolean.class.equals(field.getType())) {
                            boolean i = field.getBoolean(object);

                            if(ImGui.checkbox(field.getName(), i)){
                                field.setBoolean(object, !i);
                            }
                        }

                        if(Vector3f.class.equals(field.getType())){
                            Vector3f i = (Vector3f) field.get(object);


                            float[] vec = new float[]{i.x, i.y, i.z};

                            if(ImGui.sliderFloat3(field.getName(), vec, 0, 200)){
                                field.set(object, new Vector3f(vec[0], vec[1], vec[2]));
                            }
                        }
                        if(Texture2D.class.equals(field.getType())){

                            ImGui.separator();

                            Texture2D i = (Texture2D) field.get(object);

                            ImGui.text(field.getName());
                            if(ImGui.button("Select")){
                                String texturePath =
                                        TinyFileDialogs.tinyfd_openFileDialog(
                                                "Select Image",
                                                FileSystemView.getFileSystemView().getHomeDirectory().getPath(),
                                                null,
                                                null,
                                                false
                                        );


                                if(texturePath != null) {
                                    i.dispose();
                                    Disposer.remove(i);

                                    field.set(object, new Texture2D(texturePath));
                                }
                            }
                            ImGui.sameLine();
                            ImGui.text(i.getPath());

                            ImGui.image(i.getTexID(), 256, 256);




                        }


                    }
                    catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }

                }


                ImGui.separator();

            }


        }
        ImGui.end();

    }

    @Override
    public void dispose() {

    }
}
