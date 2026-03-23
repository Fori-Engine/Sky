package engine.gameui;

import engine.graphics.Color;

public class ContainerWidget extends Widget {
    private boolean ignore;

    public Widget setIgnore(boolean ignore) {
        this.ignore = ignore;
        return this;
    }

    @Override
    public int getRequiredWidth() {
        return layoutEngine.getComputedWidth();
    }

    @Override
    public int getRequiredHeight() {
        return layoutEngine.getComputedHeight();
    }

    @Override
    public void update(GfxPlatform platform, int x, int y, int w, int h) {
        if(!ignore)
            platform.drawRect(x, y, w, h, platform.getTheme().containerBackgroundColor);
        updateChildren(platform, x, y, w, h);
    }



}
