package engine.gameui;

import org.joml.Vector2f;

public class Loop {
    private GfxPlatform gfxPlatform;
    private Widget widget;

    public GfxPlatform getGfxPlatform() {
        return gfxPlatform;
    }

    public void setGfxPlatform(GfxPlatform gfxPlatform) {
        this.gfxPlatform = gfxPlatform;
    }

    public Widget getWidget() {
        return widget;
    }

    public void setWidget(Widget widget) {
        this.widget = widget;
    }

    public void update(int x, int y) {
        widget.update(gfxPlatform, x, y, widget.getRequiredWidth(), widget.getRequiredHeight());
    }
    public void update(int x, int y, int w, int h) {
        widget.update(gfxPlatform, x, y, w, h);
    }



}
