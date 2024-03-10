package lake.editor;

import imgui.ImGui;
import lake.Time;
import lake.graphics.Window;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;

import java.io.OutputStream;
import java.io.PrintStream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glGetError;

public class GPUDriverOutput extends Panel {


    private static class CustomOutputStream extends OutputStream {

        private StringBuilder string = new StringBuilder();
        public boolean pause;

        @Override
        public void write(int b) {

            if(!pause) {

                this.string.append((char) b);

                if (string.length() >= 1000) string.setLength(0);
            }
        }

        @Override
        public String toString() {
            return this.string.toString();
        }
    }

    private CustomOutputStream customOutputStream = new CustomOutputStream();

    private Callback callback;
    private Window window;
    public GPUDriverOutput(Window window){
        super("GPU Driver Output");
        this.window = window;

        callback = GLUtil.setupDebugMessageCallback(new PrintStream(customOutputStream));
    }

    @Override
    public void render() {
        ImGui.begin(title);
        {
            ImGui.text(glGetString(GL_VENDOR));
            ImGui.sameLine();
            ImGui.text(glGetString(GL_RENDERER));


            ImGui.text("Delta Time [" + Time.deltaTime + "]");
            ImGui.text(window.getFPS() + " FPS");


            ImGui.text("glGetError [" + glGetError() + "]");
            ImGui.separator();

            if(ImGui.button(customOutputStream.pause ? "Resume" : "Pause")){
                customOutputStream.pause = !customOutputStream.pause;
            }

            ImGui.text(customOutputStream.string.toString());
        }
        ImGui.end();


    }

    @Override
    public void dispose() {

    }
}
