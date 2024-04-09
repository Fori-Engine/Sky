package lake.graphics.ui;


import lake.graphics.Camera;
import lake.graphics.Renderer2D;

import java.util.ArrayList;

import static lake.graphics.ui.IngridUI.*;


public class EdgeLayout extends UILayout {
    public EdgeLayout(int[] layoutHints, ArrayList<IngridUI.UIWidget> myChildren, Camera camera) {
        super(layoutHints, myChildren, camera);
    }

    public UILayoutDrawCommand build(IngridUI.UIWidget root, int[] layout, IngridUI.UIScope uiScope){
        UILayoutDrawCommand drawCommand = new UILayoutDrawCommand() {
            @Override
            public void draw(Renderer2D renderer, int parentX, int parentY, int w, int h) {


                int myX = parentX, myY = parentY;


                int northOffset = 0;



                int requiredHeight = h;


                for(UIWidget j : myChildren){
                    if(isUIHint(j.getMyLayoutFlags(), North)){
                        northOffset = j.getHeight();
                        requiredHeight -= j.getHeight();
                    }
                    if(isUIHint(j.getMyLayoutFlags(), South)){

                        requiredHeight -= j.getHeight();
                    }

                }

                for(IngridUI.UIWidget j : myChildren){



                    for(int layoutFlag : j.getMyLayoutFlags()) {








                        if (layoutFlag == North) {
                            j.draw(renderer, myX, myY, w, j.getHeight());
                        }
                        if (layoutFlag == East) {
                            j.draw(renderer, myX + (w - j.getWidth()), myY + northOffset, j.getWidth(), requiredHeight);
                        }
                        if (layoutFlag == South) {
                            j.draw(renderer, myX, h - j.getHeight(), w, j.getHeight());
                        }
                        if (layoutFlag == West) {
                            j.draw(renderer, myX, myY + northOffset, j.getWidth(), requiredHeight);

                        }
                        if (layoutFlag == Center) {
                            j.draw(renderer, myX + w / 2 - j.getWidth() / 2, myY + h / 2 - j.getHeight() / 2, j.getWidth(), j.getHeight());
                        }
                    }

                }


            }

            @Override
            public int getWidth() {


                int width = 0;

                int northWidth = 0, southWidth = 0;

                for(IngridUI.UIWidget w : myChildren){
                    for(int flag : getMyLayoutFlags()){
                        if(flag == West || flag == East || flag == Center)
                            width += w.getWidth();

                        if(flag == North)
                            northWidth = w.getWidth();
                        if(flag == South)
                            southWidth = w.getWidth();
                    }
                }

                if(northWidth != 0 || southWidth != 0){
                    width = Math.max(northWidth, southWidth);
                }

                return width;
            }

            @Override
            public int getHeight() {

                int height = 0;

                int westHeight = 0, eastHeight = 0, centerHeight = 0;

                for(IngridUI.UIWidget w : myChildren){
                    for(int flag : getMyLayoutFlags()){
                        if(flag == North || flag == South)
                            height += w.getHeight();

                        if(flag == Center)
                            centerHeight = w.getHeight();
                        if(flag == West)
                            westHeight = w.getHeight();
                        if(flag == East)
                            eastHeight = w.getHeight();
                    }
                }

                height += Math.max(centerHeight, Math.max(westHeight, eastHeight));


                return height;
            }

            @Override
            public int[] getMyLayoutFlags() {
                return layout;
            }
        };

        return drawCommand;
    }
}
