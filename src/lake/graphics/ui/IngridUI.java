package lake.graphics.ui;


import lake.Input;
import lake.Math;
import lake.graphics.*;
import lake.graphics.Color;
import lake.graphics.Window;
import org.joml.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;


public class IngridUI {


    public static int West = 0;
    public static int East = 1;
    public static int North = 2;
    public static int South = 3;
    public static int Center = 4;


    public static int Vertical = 5;

    public static int Horizontal = 6;




    public static float uiScale = 1;
    private static UIWidget root;


    private static Renderer2D renderer;
    private static Camera camera;


    private static final HashMap<String, HashMap<Integer, UIEvent>> contextBasedEventMap = new HashMap<>();

    private static int currentID = 0;

    private static final Stack<UIScope> uiScopeStack = new Stack<>();
    private static String currentContext;
    private static Font2D font;

    private static Window window;
    private static boolean hasFont;

    public static void beginContext(Renderer2D renderer, Window window, String contextName){
        IngridUI.camera = renderer.getCamera2D();
        IngridUI.renderer = renderer;
        IngridUI.currentContext = contextName;

        if(!hasFont) {

            IngridUI.font = Font2D.getDefault();
            hasFont = true;
        }
        IngridUI.window = window;



        if(!contextBasedEventMap.containsKey(contextName))
            contextBasedEventMap.put(contextName, new HashMap<>());
    }

    public static void endContext(){
        root = null;
        uiScopeStack.clear();
    }




    public static void renderUI() {

        currentID = 0;

        contextBasedEventMap.get(currentContext).clear();


        if (root != null) {
            root.draw(renderer, 0, 0, (int) renderer.getWidth(), (int) renderer.getHeight());
        }

    }

    public static void submit(UIWidget uiWidget){
        if(!uiScopeStack.isEmpty()) {
            uiScopeStack.peek().childUI.add(uiWidget);
        }
        else {
            root = uiWidget;
        }
    }

    public static void startPanel(String layoutName, Color bgColor, int... initialLayoutHints){
        int myID = getNewID();
        uiScopeStack.push(new UIScope(layoutName, initialLayoutHints, bgColor));
    }

    public static void endPanel(int... layout){

        UIScope uiScope = uiScopeStack.pop();
        ArrayList<UIWidget> myChildren = uiScope.childUI;
        int[] initialLayoutHints = uiScope.initialLayoutHints;
        String layoutName = uiScope.layoutName;

        UILayout uiLayout = null;


        if(layoutName.equals("lineLayout")){
            uiLayout = new LineLayout(initialLayoutHints, myChildren, camera);
        }
        if(layoutName.equals("edgeLayout")){
            uiLayout = new EdgeLayout(initialLayoutHints, myChildren, camera);
        }


        UILayout.UILayoutDrawCommand drawCommand = uiLayout.build(root, layout, uiScope);

        submit(new UIWidget() {
            @Override
            public void draw(Renderer2D renderer, int x, int y, int width, int height) {

                //renderer.drawFilledRect(x, y, width, height, uiScope.bgColor);
                renderer.drawRect(x, y, width, height, Color.LIGHT_GRAY, 1);



                drawCommand.draw(renderer, x, y, width, height);
            }

            @Override
            public int getWidth() {
                return drawCommand.getWidth();
            }

            @Override
            public int getHeight() {
                return drawCommand.getHeight();
            }

            @Override
            public int[] getMyLayoutFlags() {
                return drawCommand.getMyLayoutFlags();
            }
        });
    }


    public static void text(String text, int... layout){
        int myID = getNewID();
        submit(new UIWidget() {
            @Override
            public void draw(Renderer2D renderer, int x, int y, int w, int h) {

                float width = font.getWidthOf(text);
                float height = font.getHeightOf(text);


                renderer.drawText(x + (w / 2f - width / 2), y + (h / 2f - height / 2), text, Color.GRAY, font);


            }

            @Override
            public int getWidth() {
                float width = font.getWidthOf(text);
                return (int) width + Text.padding * 2;
            }

            @Override
            public int getHeight() {
                float height = font.getHeightOf(text);
                return (int) height + Text.padding * 2;
            }

            @Override
            public int[] getMyLayoutFlags() {
                return layout;
            }
        });
    }
    public static boolean button(String text, int... layout) {
        int myID = getNewID();


        submit(new UIWidget() {
            @Override
            public void draw(Renderer2D renderer, int x, int y, int w, int h) {


                Color color = Color.BLACK;

                Rect2D bounds = new Rect2D(x, y, w, h);


                if(bounds.contains(window.getMouseX(), window.getMouseY())){



                    if(window.isMouseJustPressed(Input.MOUSE_BUTTON_1)) {
                        color = Color.LIGHT_GRAY;
                        contextBasedEventMap.get(currentContext).put(myID, new Buttons.ButtonEvent(true));
                    }
                    else {
                        color = Color.GRAY;
                        contextBasedEventMap.get(currentContext).put(myID, new Buttons.ButtonEvent(false));
                    }
                }

                float width = font.getWidthOf(text);
                float height = font.getHeightOf(text);

                renderer.drawFilledRect(bounds.x, bounds.y, bounds.w, bounds.h, color);
                renderer.drawText( x + (w / 2f - width / 2), y + (h / 2f - height / 2), text, Color.WHITE, font);

            }

            @Override
            public int getWidth() {
                float width = font.getWidthOf(text);

                return (int) width + Buttons.padding * 2;
            }

            @Override
            public int getHeight() {

                float height = font.getHeightOf(text);
                return (int) height + Buttons.padding * 2;
            }

            @Override
            public int[] getMyLayoutFlags() {
                return layout;
            }
        });



        UIEvent event = getEvent(myID);

        if(event == null){
            return new Buttons.ButtonEvent(false).isPressed;
        }
        else {
            return ((Buttons.ButtonEvent) event).isPressed;
        }


    }





    public static float slider(String text, float input, float min, float max, boolean snap, int... layout){
        int myID = getNewID();

        input = Math.clamp(input, min, max);

        if(snap)
            input = (int) input;


        

        float finalInput = input;

        submit(new UIWidget() {
            @Override
            public void draw(Renderer2D renderer, int x, int y, int w, int h) {
                Rect2D myBounds = new Rect2D(x, y, w, h);
                Vector2f mousePos = new Vector2f(window.getMouseX(), window.getMouseY());


                int yInset = 8;

                y += Sliders.padding;
                x += Sliders.padding;

                
                renderer.drawText(x, y, text, Color.GRAY, font);
                
                
                

                y += (int) (font.getHeightOf(text) + Sliders.padding);

                Rect2D sliderBar = new Rect2D(x + (Sliders.sliderKnobWidth / 2f), y + yInset, w - Sliders.sliderKnobHeight - Sliders.padding, Sliders.sliderKnobHeight - (yInset * 2));


                float a = (finalInput - min) / (max - min);
                float sliderX = (a * sliderBar.w) + x;


                Rect2D sliderKnob = new Rect2D(sliderX, y, Sliders.sliderKnobWidth, Sliders.sliderKnobHeight);

                {

                    renderer.drawFilledRect(sliderBar.x, sliderBar.y, sliderBar.w, sliderBar.h, Color.LIGHT_GRAY);
                    renderer.drawFilledEllipse(sliderKnob.x, sliderKnob.y, sliderKnob.w, sliderKnob.h, Color.BLACK);

                }


                if(myBounds.contains(mousePos.x, mousePos.y) && mousePos.x >= sliderBar.x && mousePos.x <= sliderBar.x + sliderBar.w){

                    if(window.isMousePressed(Input.MOUSE_BUTTON_1)) {


                        sliderKnob.x = mousePos.x - sliderKnob.w / 2;
                        sliderKnob.x = Math.clamp(sliderKnob.x, x, x + w - (sliderKnob.w / 2));



                        //Export the output value as bounds space
                        float newSliderX = (sliderKnob.x - x) / sliderBar.w;
                        float output = (min + (newSliderX * (max - min)));


                        if(snap)
                            output = (int) output;



                        String strValue = String.format("%.2f", output);

                        float height = font.getHeightOf(strValue);

                        renderer.drawText(sliderKnob.x + (sliderKnob.w * 1.5f), sliderKnob.y + (sliderKnob.h / 2) - (height / 2), strValue, Color.GRAY, font);
                        contextBasedEventMap.get(currentContext).put(myID, new Sliders.SliderEvent(output));
                    }
                }


            }

            @Override
            public int getWidth() {
                float width = font.getWidthOf(text);
                return (int) width + (Sliders.padding * 3);
            }

            @Override
            public int getHeight() {
                float height = font.getHeightOf(text);
                return (int) height + Sliders.sliderKnobHeight + (Sliders.padding * 3);
            }

            @Override
            public int[] getMyLayoutFlags() {
                return layout;
            }
        });



        UIEvent event = getEvent(myID);

        if(event == null){
            return new Sliders.SliderEvent(input).value;
        }
        else {
            return ((Sliders.SliderEvent) event).value;
        }


    }

    public static int getNewID(){
        currentID++;
        return currentID;
    }


    private static UIEvent getEvent(int id){
        UIEvent event = contextBasedEventMap.get(currentContext).get(id);
        return event;
    }

    public static Stack<UIScope> getUiScopeStack() {
        return uiScopeStack;
    }



    public interface UIWidget {
        void draw(Renderer2D renderer, int x, int y, int width, int height);
        int getWidth();
        int getHeight();
        int[] getMyLayoutFlags();
    }

    public static class UIScope {
        public ArrayList<UIWidget> childUI = new ArrayList<>();
        public String layoutName;
        public int[] initialLayoutHints;

        public Color bgColor;


        public UIScope(String layoutName, int[] initialLayoutHints, Color bgColor) {
            this.layoutName = layoutName;
            this.initialLayoutHints = initialLayoutHints;
            this.bgColor = bgColor;
        }
    }
}
