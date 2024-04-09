package lake.graphics.ui;

import lake.graphics.Camera;
import java.util.ArrayList;
import static lake.graphics.ui.IngridUI.*;
import lake.graphics.Renderer2D;


public class LineLayout extends UILayout {


    public LineLayout(int[] layoutHints, ArrayList<IngridUI.UIWidget> myChildren, Camera camera) {
        super(layoutHints, myChildren, camera);
    }

    public UILayoutDrawCommand build(IngridUI.UIWidget root, int[] layout, IngridUI.UIScope uiScope){
        UILayoutDrawCommand drawCommand = new UILayoutDrawCommand() {
            @Override
            public void draw(Renderer2D renderer, int parentX, int parentY, int w, int h) {

                int myX = parentX, myY = parentY;


                for(int i : layoutHints){
                    if(i == Vertical){
                        for (IngridUI.UIWidget f : myChildren) {

                            f.draw(renderer, myX, myY, f.getWidth(), f.getHeight());


                            myY += f.getHeight();
                        }
                    }
                    else if(i == Horizontal){
                        for (IngridUI.UIWidget f : myChildren) {
                            f.draw(renderer, myX, myY, f.getWidth(), f.getHeight());



                            myX += f.getWidth();
                        }
                    }
                }


            }

            @Override
            public int getWidth() {

                int width = 0;

                for(int i : layoutHints){
                    //Get max width
                    if(i == Vertical){
                        for(IngridUI.UIWidget f : myChildren){
                            if(f.getWidth() > width)
                                width = f.getWidth();
                        }
                    }
                    else if(i == Horizontal){
                        for(IngridUI.UIWidget f : myChildren){
                            width += f.getWidth();
                        }
                    }
                }




                return width;
            }

            @Override
            public int getHeight() {



                int height = 0;


                for(int i : layoutHints){
                    if(i == Horizontal){
                        //Get max height
                        for(IngridUI.UIWidget f : myChildren){
                            if(f.getHeight() > height)
                                height = f.getHeight();
                        }
                    }
                    else if (i == Vertical){
                        //Get total height
                        for(IngridUI.UIWidget f : myChildren){
                            height += f.getHeight();
                        }
                    }
                }


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
