package lake.graphics.opengl;

import lake.graphics.Context;
import lake.graphics.Window;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.opengl.GL11.glViewport;

public class GLContext extends Context {


    @Override
    public void enableHints() {

    }

    @Override
    public void setup(Window window) {
        glfwMakeContextCurrent(window.getGLFWHandle());
        glfwSwapInterval(1);
        GL.createCapabilities();


        glfwSetWindowSizeCallback(window.getGLFWHandle(), (window1, width, height) -> glViewport(0, 0, width, height));
    }

    @Override
    public void swapBuffers(Window window) {
        glfwSwapBuffers(window.getGLFWHandle());
    }
}
