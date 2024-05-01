package lake.graphics;

/***
 * Represents a drawing surface for a Renderer2D. This class also handles mouse and key input along with Disposing
 * OpenGL resources once the application is closed.
 */

public interface Window {

    void setSize(int w, int h);
    int getWidth();
    int getHeight();
    boolean shouldClose();
    void update();
    int getKey(int key);
    boolean isKeyPressed(int key);
    boolean isKeyReleased(int key);
    void setTitle(String title);
    float getMouseX();
    float getMouseY();
    boolean isMousePressed(int button);
    boolean isMouseJustPressed(int button);
    boolean isMouseReleased(int button);

    void setIcon(String path);
    int getFPS();
    void close();
}
