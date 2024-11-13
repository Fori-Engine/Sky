package fori.ui;


import java.util.List;

public class EdgeLayout extends Layout {

    @Override
    public void layoutAndDraw(Widget root, List<Widget> children, PanelScope panelScope, Adapter adapter, float parentX, float parentY, float w, float h) {
        float myX = parentX, myY = parentY;

        float northOffset = 0;
        float southOffset = 0;

        for(Widget widget : children){

            for(int layoutFlag : widget.layoutHints) {

                if (layoutFlag == Flags.North) {
                    widget.draw(adapter, myX, myY, w, widget.getHeight());
                    northOffset = widget.getHeight();
                }
                if (layoutFlag == Flags.East) {
                    widget.draw(adapter, myX + (w - widget.getWidth()), myY + northOffset, widget.getWidth(), h - southOffset);
                }
                if (layoutFlag == Flags.South) {
                    widget.draw(adapter, myX, myY + (h - widget.getHeight()), w, widget.getHeight());
                    southOffset = widget.getHeight();
                }
                if (layoutFlag == Flags.West) {
                    widget.draw(adapter, myX, myY + northOffset, widget.getWidth(), h - southOffset);
                }
                if (layoutFlag == Flags.Center) {
                    widget.draw(adapter, myX + w / 2 - widget.getWidth() / 2, myY + h / 2 - widget.getHeight() / 2, widget.getWidth(), widget.getHeight());
                }
            }

        }
    }

    @Override
    public float getWidth(List<Widget> children) {
        float width = 0;

        float northWidth = 0, southWidth = 0;

        for(Widget w : children){
            for(int flag : w.getLayoutHints()){
                if(flag == Flags.West || flag == Flags.East || flag == Flags.Center)
                    width += w.getWidth();

                if(flag == Flags.North)
                    northWidth = w.getWidth();
                if(flag == Flags.South)
                    southWidth = w.getWidth();
            }
        }

        if(northWidth != 0 || southWidth != 0){
            width = Math.max(northWidth, southWidth);
        }

        return width;
    }

    @Override
    public float getHeight(List<Widget> children) {
        float height = 0;

        float westHeight = 0, eastHeight = 0, centerHeight = 0;

        for(Widget w : children){
            for(int flag : w.layoutHints){
                if(flag == Flags.North || flag == Flags.South)
                    height += w.getHeight();

                if(flag == Flags.Center)
                    centerHeight = w.getHeight();
                if(flag == Flags.West)
                    westHeight = w.getHeight();
                if(flag == Flags.East)
                    eastHeight = w.getHeight();
            }
        }

        height += Math.max(centerHeight, Math.max(westHeight, eastHeight));

        return height;
    }
}
