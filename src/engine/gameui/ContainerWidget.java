package engine.gameui;

import engine.graphics.Color;

public class ContainerWidget extends Widget {



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
        platform.drawRect(x, y, w, h, Color.GRAY);
        updateChildren(platform, x, y);
    }



}
