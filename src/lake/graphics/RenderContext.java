package lake.graphics;

public abstract class RenderContext {

    public enum SurfaceType {
        PlatformWindow,
        Canvas
    }

    public abstract void enableHints();
    public abstract void setup(PlatformWindow window);
    public abstract void swapBuffers(PlatformWindow window);
    public abstract void readyDisplay(PlatformWindow window);


}
