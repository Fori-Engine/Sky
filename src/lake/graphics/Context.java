package lake.graphics;

import javax.swing.*;
import java.awt.*;

public abstract class Context {

    public enum SurfaceType {
        PlatformWindow,
        Canvas
    }

    public abstract void enableHints();
    public abstract void setup(PlatformWindow window);
    public abstract void swapBuffers(PlatformWindow window);
    public abstract void readyDisplay(PlatformWindow window);
    public abstract void readyCanvas(Canvas canvas);



}
