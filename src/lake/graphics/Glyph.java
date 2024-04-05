package lake.graphics;

public class Glyph {
    private float x;
    private float y;
    private float w;
    private float h;
    private float xOffset;
    private float yOffset;
    private float xAdvance;


    public Glyph(float x, float y, float w, float h, float xOffset, float yOffset, float xAdvance) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.xOffset = xOffset;
        this.yOffset = yOffset;

        this.xAdvance = xAdvance;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getW() {
        return w;
    }

    public float getH() {
        return h;
    }

    public float getxOffset() {
        return xOffset;
    }

    public float getyOffset() {
        return yOffset;
    }

    public float getXAdvance() {
        return xAdvance;
    }
}
