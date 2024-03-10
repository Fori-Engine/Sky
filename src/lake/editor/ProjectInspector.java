package lake.editor;

import imgui.type.ImBoolean;
import lake.Utils;
import lake.graphics.Color;
import lake.script.EditorUI;
import imgui.ImGui;

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
