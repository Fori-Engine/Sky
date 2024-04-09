package lake.graphics.ui;


import lake.graphics.Camera;
import lake.graphics.Renderer2D;

import java.util.ArrayList;

public abstract class UILayout {

    public final int[] layoutHints;

    public final ArrayList<IngridUI.UIWidget> myChildren;

    public final Camera camera;
    public UILayout(int[] layoutHints, ArrayList<IngridUI.UIWidget> myChildren, Camera camera) {
        this.layoutHints = layoutHints;
        this.myChildren = myChildren;
        this.camera = camera;
    }

    public abstract UILayoutDrawCommand build(IngridUI.UIWidget root, int[] layout, IngridUI.UIScope uiScope);

    public interface UILayoutDrawCommand {
        void draw(Renderer2D renderer, int x, int y, int width, int height);
        int getWidth();
        int getHeight();
        int[] getMyLayoutFlags();
    }






    public boolean isUIHint(int[] uiHints, int hint){

        for(int i : uiHints){
            if(i == hint)
                return true;
        }


        return false;
    }

}
