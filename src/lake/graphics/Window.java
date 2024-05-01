package lake.graphics;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import lake.Time;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/***
 * Represents a drawing surface for a Renderer2D. This class also handles mouse and key input along with Disposing
 * OpenGL resources once the application is closed.
 */

public class Window {

    private long window;
    private Context context;
    private double mouseX, mouseY;
    private double start;
    private int width, height;
    private String title;

    public Window(int w, int h, String title, boolean resizable){


        this.width = w;
        this.height = h;
        this.title = title;



        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");


        glfwWindowHint(GLFW_RESIZABLE, glfwBool(resizable));
        glfwWindowHint(GLFW_SAMPLES, 4);


        start = glfwGetTime();
    }


    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;


        context.enableHints();
        window = glfwCreateWindow(width, height, title, NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        context.setup(this);

        glfwShowWindow(window);
    }

    private int glfwBool(boolean b){
        return b ? GLFW_TRUE : GLFW_FALSE;
    }

    public long getGLFWHandle() {
        return window;
    }

    public void setSize(int w, int h){
        this.width = w;
        this.height = h;

        glfwSetWindowSize(window, w, h);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean shouldClose(){
        return glfwWindowShouldClose(window);
    }

    /***
     * Updates the Window's events, this includes polling GLFW and updating Input and deltaTime
     */
    public void update() {

        DoubleBuffer posX = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer posY = BufferUtils.createDoubleBuffer(1);
        glfwGetCursorPos(window, posX, posY);

        mouseX = posX.get();
        mouseY = posY.get();




        context.swapBuffers(this);

        glfwPollEvents();

        double timeNow = glfwGetTime();
        double deltaTime = timeNow - start;
        start = timeNow;
        Time.deltaTime = (float) deltaTime;
    }

    public int getKey(int key){
        return glfwGetKey(window, key);
    }

    public boolean isKeyPressed(int key){
        return getKey(key) == GLFW_PRESS;
    }
    public boolean isKeyReleased(int key){
        return getKey(key) == GLFW_RELEASE;
    }

    public void setTitle(String title){
        glfwSetWindowTitle(window, title);
    }

    public float getMouseX() {
        return (float) mouseX;
    }

    public float getMouseY() {
        return (float) mouseY;
    }

    public boolean isMousePressed(int button){
        return glfwGetMouseButton(window, button) == GLFW_PRESS;
    }



    public boolean isMouseReleased(int button){
        return glfwGetMouseButton(window, button) == GLFW_RELEASE;
    }


    public void setIcon(String path) {
        try(MemoryStack stack = MemoryStack.stackPush()){

            GLFWImage.Buffer icon = GLFWImage.calloc(1, stack);

            IntBuffer w = BufferUtils.createIntBuffer(1);
            IntBuffer h = BufferUtils.createIntBuffer(1);
            IntBuffer channelsInFile = BufferUtils.createIntBuffer(1);
            ByteBuffer texture = STBImage.stbi_load(path, w, h, channelsInFile, 4);
            String error = STBImage.stbi_failure_reason();

            if(error != null){
                throw new RuntimeException("Failed to set GLFW window icon: " + error + " (" + path + ")");
            }
            int width = w.get();
            int height = h.get();

            icon.width(width);
            icon.height(height);
            icon.pixels(texture);


            glfwSetWindowIcon(window, icon);



        }
    }


    /***
     * Closes the Window, and destroys the GLFW and OpenGL context
     */

    public void close() {

        Disposer.disposeAllInCategory("managedResources");
        Disposer.disposeAllInCategory("renderer");



        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
}
