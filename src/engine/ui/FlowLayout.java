package engine.ui;


import java.util.List;

public class FlowLayout extends Layout {

    public int axis;

    public FlowLayout(int axis) {
        this.axis = axis;
    }

    @Override
    public void layoutAndDraw(List<Widget> children, PanelScope panelScope, Adapter adapter, float parentX, float parentY, float w, float h) {
        float myX = parentX, myY = parentY;



        if(axis == Flags.Vertical){
            for (Widget f : children) {
                f.draw(adapter, myX, myY, f.getWidth(), f.getHeight());
                myY += f.getHeight();
            }
        }
        else if(axis == Flags.Horizontal){
            for (Widget f : children) {
                f.draw(adapter, myX, myY, f.getWidth(), f.getHeight());
                myX += f.getWidth();
            }
        }


    }

    @Override
    public float getWidth(List<Widget> children) {
        float width = 0;

        if(axis == Flags.Vertical){
            for(Widget f : children){
                if(f.getWidth() > width)
                    width = f.getWidth();
            }
        }
        else if(axis == Flags.Horizontal){
            for(Widget f : children){
                width += f.getWidth();
            }
        }



        return width;
    }

    @Override
    public float getHeight(List<Widget> children) {
        float height = 0;


        if(axis == Flags.Horizontal){
            //Get max height
            for(Widget f : children){
                if(f.getHeight() > height)
                    height = f.getHeight();
            }
        }
        else if(axis == Flags.Vertical){
            //Get total height
            for(Widget f : children){
                height += f.getHeight();
            }
        }


        return height;
    }
}
