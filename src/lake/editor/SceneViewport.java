package lake.editor;

import lake.graphics.Game;
import lake.graphics.StandaloneWindow;
import lake.graphics.Window;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.opengl.GL11.glViewport;

public class SceneViewport extends Panel {
    private int framebufferID;
    private boolean isConnected;

    private float mouseX, mouseY;
    private int width, height;
    private boolean focused;

    protected SceneViewport(StandaloneWindow window) {
        super("Scene Viewport");

        Game.window = new SceneViewportWindow(window);
    }

    public void useFramebuffer2D(int framebufferID){
        this.framebufferID = framebufferID;
        isConnected = true;
    }

    public void disconnect(){
        isConnected = false;
    }

    @Override
    public void render() {
        //Nicht sehr gut aber was anders kann ich tun?
        Game.window.update();


        ImGui.begin(title);
        {

            //Resizing the window does not change the Renderer2D's dimensions the game setup
            width = (int) ImGui.getWindowSizeX();
            height = (int) ImGui.getWindowSizeY();

            focused = ImGui.isWindowFocused();




            if(isConnected) {
                ImVec2 pos = ImGui.getWindowPos();



                mouseX = ImGui.getMousePosX() - ImGui.getWindowPosX();
                mouseY = ImGui.getMousePosY() - ImGui.getWindowPosY();

                //Todo, kind of ugly. The project's Renderer2D has to be regenerated
                glViewport(0, 0, (int) width, (int) height);


                ImGui.getWindowDrawList().addImage(
                        framebufferID,
                        pos.x,
                        pos.y,
                        pos.x + width,
                        pos.y + height,
                        0, 1,
                        1, 0
                );

                //if(ImGui.)
            }
            else {
                ImGui.text("Open a project");
            }



        }
        ImGui.end();

    }

    @Override
    public void dispose() {

    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    private class SceneViewportWindow implements Window {

        private Window window;

        public SceneViewportWindow(StandaloneWindow window){
            this.window = window;
        }
        @Override
        public void setSize(int w, int h) {

        }

        @Override
        public int getWidth() {
            return (int) width;
        }

        @Override
        public int getHeight() {
            return (int) height;
        }

        @Override
        public boolean shouldClose() {
            return false;
        }

        @Override
        public void update() {

        }

        @Override
        public int getKey(int key) {
            if(!focused) return GLFW.GLFW_RELEASE;
            return window.getKey(key);
        }

        @Override
        public boolean isKeyPressed(int key) {
            if(!focused) return false;
            return window.isKeyPressed(key);
        }

        @Override
        public boolean isKeyReleased(int key) {
            if(!focused) return false;
            return window.isKeyReleased(key);
        }

        @Override
        public void setTitle(String title) {

        }

        @Override
        public float getMouseX() {
            return mouseX;
        }

        @Override
        public float getMouseY() {
            return mouseY;
        }

        @Override
        public boolean isMousePressed(int button) {
            if(!focused) return false;
            return window.isMousePressed(button);
        }

        @Override
        public boolean isMouseJustPressed(int button) {
            return false;
        }

        @Override
        public boolean isMouseReleased(int button) {
            if(!focused) return false;
            return window.isMouseReleased(button);
        }

        @Override
        public int getFPS() {
            return window.getFPS();
        }

        @Override
        public void close() {


        }
    }
}
