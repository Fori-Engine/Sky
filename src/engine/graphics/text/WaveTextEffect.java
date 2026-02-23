package engine.graphics.text;

import org.joml.Math;
import org.joml.Vector2f;

public class WaveTextEffect extends TextEffect {
    private Vector2f offset = new Vector2f();
    @Override
    public Vector2f offset(int charIndex, int stringLength) {
        float scalar = (float) charIndex / stringLength;
        offset.set(0, 10 * (float) Math.sin((2 * Math.PI * scalar) - 5 * (elapsedTimeMs / 1000)));
        return offset;
    }
}
