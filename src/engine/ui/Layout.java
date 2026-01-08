package engine.ui;

import java.util.List;

public abstract class Layout {


    public abstract void layoutAndDraw(List<Widget> children, PanelScope panelScope, Adapter adapter, float parentX, float parentY, float w, float h);
    public abstract float getWidth(List<Widget> children);
    public abstract float getHeight(List<Widget> children);
}
