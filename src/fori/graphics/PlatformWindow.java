package fori.graphics;

import fori.asset.Asset;
import fori.asset.TextureData;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import fori.Time;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;



public class PlatformWindow {
    private long window;
    private RenderContext renderContext;
    private double mouseX, mouseY;
    private double start;
    private int width, height;
    private String title;

    public PlatformWindow(int w, int h, String title, boolean resizable){


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


    public RenderContext getContext() {
        return renderContext;
    }

    public void configureAndCreateWindow(RenderContext renderContext) {
        this.renderContext = renderContext;


        renderContext.enableHints();
        window = glfwCreateWindow(width, height, title, NULL, NULL);
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        renderContext.setup(this);

        glfwShowWindow(window);
    }

    public void onRenderContextReady(RenderContext renderContext, Renderer renderer){

        glfwSetFramebufferSizeCallback(window, (window, width, height) -> {

            int[] w = new int[1], h = new int[1];

            glfwGetFramebufferSize(window, w, h);

            while(w[0] == 0 || h[0] == 0){
                glfwGetFramebufferSize(window, w, h);
                glfwWaitEvents();
            }



            renderer.onSurfaceResized(w[0], h[0]);


        });

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




        renderContext.swapBuffers(this);

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



    public void setIcon(Asset<TextureData> texture) {
        GLFWImage.Buffer icon = GLFWImage.create(1);
        ByteBuffer textureBytes = MemoryUtil.memAlloc(texture.asset.width * texture.asset.height * 4);


        textureBytes.put(texture.asset.data);


        textureBytes.flip();

        icon.width(texture.asset.width);
        icon.height(texture.asset.height);
        icon.pixels(textureBytes);


        System.out.println(window);
        glfwSetWindowIcon(window, icon);

        icon.free();
        MemoryUtil.memFree(textureBytes);
    }


    public void close() {

        Disposer.disposeAllInCategory("managedResources");
        Disposer.disposeAllInCategory("renderer");



        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }




}
