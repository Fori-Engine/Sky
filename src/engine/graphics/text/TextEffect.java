package engine.graphics.text;

import engine.Time;
import org.joml.Vector2f;

public abstract class TextEffect {
    public float elapsedTimeMs;
    public void update() {
        elapsedTimeMs += Time.deltaTime * 1000;
    }
    public abstract Vector2f offset(int charIndex, int stringLength);
}
