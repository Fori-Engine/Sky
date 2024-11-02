package fori.graphics;

import fori.Surface;

public abstract class RenderContext {
    public abstract void enableHints();
    public abstract void setup();
    public abstract void readyDisplay(Surface surface);
}
