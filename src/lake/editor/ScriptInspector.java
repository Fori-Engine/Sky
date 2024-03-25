package lake.editor;

import imgui.type.ImBoolean;
import lake.Utils;
import lake.graphics.Color;
import lake.graphics.Disposer;
import lake.graphics.opengl.GLTexture2D;
import lake.script.EditorUI;
import org.joml.Vector3f;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.swing.filechooser.FileSystemView;
import java.lang.reflect.Field;

import static imgui.ImGui.*;

public class ScriptInspector extends Panel {
    private LakeEditor lakeEditor;
    protected ScriptInspector(LakeEditor lakeEditor) {
        super("Inspector");
        this.lakeEditor = lakeEditor;
    }


    private boolean showLog = false;
    @Override
    public void render() {
        begin(title);
        {

            ProjectRef projectRef = lakeEditor.getProjectRef();



            if(projectRef.getLastProjectThrowableLog() != null){


                text("Project Error (Details in Project Log)");


                if(button("Open Project Log")) {
                    showLog = true;
                }

                if(showLog){
                    openPopup("The project crashed!");
                    if (beginPopupModal("The project crashed!", new ImBoolean(true))) {
                        text(Utils.exceptionToString(projectRef.getLastProjectThrowableLog()));

                        if (button("Close")) {
                            closeCurrentPopup();
                            showLog = false;
                            projectRef.clearLastProjectThrowableLog();
                        }
                        endPopup();
                    }
                }
            }

            if(projectRef.isProjectOpened()) {

                text(projectRef.getProjectPath().getPath());
                separator();



                if (button(projectRef.isCurrentProjectPaused() ? "Resume" : "Pause")) {
                    projectRef.setCurrentProjectPaused(!projectRef.isCurrentProjectPaused());
                }


                sameLine();







                separator();
                for(String id : EditorUI.getRegistry().keySet()){

                    Object object = EditorUI.getRegistry().get(id);

                    String headerText = id + " [" + object + "]";



                    if(collapsingHeader(headerText)) {




                        Field[] fields = object.getClass().getFields();
                        for (int j = 0; j < fields.length; j++) {
                            Field field = fields[j];
                            try {

                                if (int.class.equals(field.getType())) {
                                    int i = field.getInt(object);
                                    int[] z = new int[]{i};
                                    if (sliderInt(field.getName(), z, 0, 200)) {
                                        field.setInt(object, z[0]);
                                    }
                                }
                                else if (float.class.equals(field.getType())) {
                                    float i = field.getFloat(object);
                                    float[] z = new float[]{i};
                                    if (sliderFloat(field.getName(), z, 0, 200)) {
                                        field.setFloat(object, z[0]);
                                    }
                                }
                                else if (double.class.equals(field.getType())) {
                                    double i = field.getDouble(object);
                                    float[] z = new float[]{(float) i};
                                    if (sliderFloat(field.getName(), z, 0, 200)) {
                                        field.setDouble(object, z[0]);
                                    }
                                }
                                else if (Color.class.equals(field.getType())) {
                                    Color i = (Color) field.get(object);
                                    float[] color = new float[]{i.r, i.g, i.b, i.a};
                                    if (colorEdit4(field.getName(), color)) {
                                        field.set(object, new Color(color[0], color[1], color[2], color[3]));
                                    }
                                }
                                else if (boolean.class.equals(field.getType())) {
                                    boolean i = field.getBoolean(object);

                                    if (checkbox(field.getName(), i)) {
                                        field.setBoolean(object, !i);
                                    }
                                }
                                else if (Vector3f.class.equals(field.getType())) {
                                    Vector3f i = (Vector3f) field.get(object);


                                    float[] vec = new float[]{i.x, i.y, i.z};

                                    if (sliderFloat3(field.getName(), vec, 0, 200)) {
                                        field.set(object, new Vector3f(vec[0], vec[1], vec[2]));
                                    }
                                }
                                else if (GLTexture2D.class.equals(field.getType())) {

                                    separator();

                                    GLTexture2D i = (GLTexture2D) field.get(object);

                                    text(field.getName());
                                    if (button("Select##" + i.toString())) {
                                        String texturePath =
                                                TinyFileDialogs.tinyfd_openFileDialog(
                                                        "Select Image",
                                                        FileSystemView.getFileSystemView().getHomeDirectory().getPath(),
                                                        null,
                                                        null,
                                                        false
                                                );


                                        if (texturePath != null) {
                                            i.dispose();
                                            Disposer.remove(i);

                                            field.set(object, new GLTexture2D(texturePath));
                                        }
                                    }
                                    sameLine();
                                    text(i.getPath());

                                    image(i.getTexID(), 256, 256);
                                }
                                else {
                                    textColored(1.0f, 0.0f, 0.0f, 1.0f, (field.getName() + "[" + field.getType() + "]\nNot editable! (Is it marked public?)"));
                                    separator();
                                }
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }

                        }
                    }


                }






            }


        }
        end();

    }

    @Override
    public void dispose() {

    }
}
