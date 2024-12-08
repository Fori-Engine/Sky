package fori.ui;

public abstract class Widget {
    public int[] layoutHints;
    public float padding;
    public String id;

    public Widget(String id, float padding) {
        this.id = id;
        this.padding = padding;
    }

    public Widget() {
    }

    abstract void draw(Adapter adapter, float x, float y, float width, float height);
    float getDrawableWidth() {
        return getWidth() - getPadding() * 2;
    }
    float getDrawableHeight() {
        return getHeight() - getPadding() * 2;
    }

    public float getPadding() {
        return padding;
    }

    abstract float getWidth();
    abstract float getHeight();

    public int[] getLayoutHints() {
        return layoutHints;
    }
}