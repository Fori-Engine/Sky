package lake.graphics.vulkan;

import lake.graphics.Context;
import lake.graphics.Window;

import static org.lwjgl.glfw.GLFW.*;

public class LVKContext extends Context {
    @Override
    public void enableHints() {
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    }

    @Override
    public void setup(Window window) {

    }

    @Override
    public void swapBuffers(Window window) {

    }
}
