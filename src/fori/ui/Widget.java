package fori.ui;

public abstract class Widget {
    public int[] layoutHints;
    abstract void draw(Adapter adapter, float x, float y, float width, float height);
    abstract float getWidth();
    abstract float getHeight();

    public int[] getLayoutHints() {
        return layoutHints;
    }
}