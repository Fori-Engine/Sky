package fori.graphics;

import org.joml.Vector3f;

public class Light {
    private Vector3f pos;
    private float intensity;
    private Color color;

    public static final int SIZE = 8 * Float.BYTES;

    public Light(Vector3f pos, float intensity, Color color) {
        this.pos = pos;
        this.intensity = intensity;
        this.color = color;
    }
}
