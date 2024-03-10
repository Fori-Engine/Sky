package lake.editor;

import lake.graphics.Color;
import lake.script.EditorUI;
import imgui.ImGui;

import java.lang.reflect.Field;

public class CodeInspector extends Panel {
    private SceneViewport sceneViewport;
    protected CodeInspector(SceneViewport sceneViewport) {
        super("Code Inspector");
        this.sceneViewport = sceneViewport;
    }

    @Override
    public void render() {
        ImGui.begin(title);
        {



            if(ProjectManager.getProjectRef().isProjectOpened()) {

                ImGui.text(ProjectManager.getProjectRef().getProjectPath().getPath());
                ImGui.separator();


                if (ImGui.button(ProjectManager.getProjectRef().isPauseUpdate() ? "Resume" : "Pause")) {
                    ProjectManager.getProjectRef().setPauseUpdate(!ProjectManager.getProjectRef().isPauseUpdate());
                }

                ImGui.sameLine();

                if (ImGui.button("Reload")) {
                    ProjectManager.getProjectRef().openProject(ProjectManager.getProjectRef().getProjectPath());
                    ProjectManager.getProjectRef().setPauseUpdate(false);
                    sceneViewport.useFramebuffer2D(ProjectManager.getProjectRef().getViewportTextureID());
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
