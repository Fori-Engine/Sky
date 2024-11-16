package fori.ui;

import fori.Input;
import fori.Math;
import fori.Surface;
import fori.graphics.Color;
import fori.graphics.Font;
import fori.graphics.Rect2D;
import org.lwjgl.system.MathUtil;

import java.util.*;


public class AmberUI {

    private static Widget root;
    private static Adapter currentAdapter;
    private static final HashMap<String, HashMap<Integer, Event>> contextBasedEventMap = new HashMap<>();
    private static int currentWidgetID = 0;
    private static final Stack<PanelScope> panelScopes = new Stack<>();
    private static String currentContext;
    private static Surface surface;

    public static final void setAdapter(Adapter adapter) { AmberUI.currentAdapter = adapter;}

    public static void setSurface(Surface surface) {
        AmberUI.surface = surface;
    }

    private static <T> T getEvent(int widgetID) {
        return (T) contextBasedEventMap.get(currentContext).get(widgetID);
    }

    private static void addEvent(int widgetID, Event event) {
        contextBasedEventMap.get(currentContext).put(widgetID, event);
    }


    public static void newContext(String contextName){
        AmberUI.currentContext = contextName;

        if(!contextBasedEventMap.containsKey(contextName))
            contextBasedEventMap.put(contextName, new HashMap<>());
    }

    public static void endContext(){
        root = null;
        panelScopes.clear();
    }
    public static void clearEvents() {
        contextBasedEventMap.get(currentContext).clear();
    }


    public static void render() {
        currentWidgetID = 0;
        if (root != null) {
            root.draw(currentAdapter, 0, 0, (int) currentAdapter.getSize().x, (int) currentAdapter.getSize().y);
        }

    }

    public static void submit(int[] layoutHints, Widget widget){
        widget.layoutHints = layoutHints;
        if(!panelScopes.isEmpty()) {
            panelScopes.peek().childWidgets.add(widget);
        }
        else {
            root = widget;
        }
    }

    public static void newPanel(Layout layout, int... layoutInParent){
        int myID = getNewID();
        panelScopes.push(new PanelScope(layout, layoutInParent));
    }

    public static void endPanel(){
        PanelScope panelScope = panelScopes.pop();

        submit(panelScope.layoutInParent, new Widget() {
            @Override
            public void draw(Adapter adapter, float x, float y, float width, float height) {
                panelScope.layout.layoutAndDraw(root, panelScope.childWidgets, panelScope, adapter, x, y, width, height);
            }

            @Override
            public float getWidth() {
                return panelScope.layout.getWidth(panelScope.childWidgets);
            }

            @Override
            public float getHeight() {
                return panelScope.layout.getHeight(panelScope.childWidgets);
            }
        });



    }

    public static void text(String text, Font font, Color color, int... layoutInParent){
        int myID = getNewID();
        float padding = 7;
        submit(layoutInParent, new Widget() {
            @Override
            public void draw(Adapter adapter, float x, float y, float w, float h) {
                adapter.drawText(x + padding, y + padding, text, font, color);
            }

            @Override
            public float getWidth() {
                return font.getWidthOf(text) + padding * 2;
            }

            @Override
            public float getHeight() {
                return font.getHeightOf(text) + padding * 2;
            }
        });
    }

    public static boolean button(String text, Font font, Color color, int... layoutInParent){
        int myID = getNewID();
        float padding = 7;

        submit(layoutInParent, new Widget() {
            @Override
            public void draw(Adapter adapter, float x, float y, float w, float h) {
                Map<Integer, Event> eventMap = contextBasedEventMap.get(currentContext);
                if(!eventMap.containsKey(myID)) {
                    addEvent(myID, new ButtonEvent());
                }

                Color baseColor = new Color(56f / 255, 56f / 255, 56f / 255, 1);

                ButtonEvent buttonEvent = getEvent(myID);



                boolean hovered = new Rect2D(x, y, w, h).contains(surface.getMousePos().x, surface.getMousePos().y);
                boolean isPressed = surface.getMousePressed(Input.MOUSE_BUTTON_LEFT) && hovered;

                if(hovered) {
                    baseColor = new Color(102f / 255, 102f / 255, 102f / 255, 1.0f);
                }
                if(isPressed) {
                    baseColor = new Color(120f / 255, 120f / 255, 120f / 255, 1.0f);
                }

                adapter.drawFilledCircle(x, y, 20, 20, 1, baseColor);
                adapter.drawFilledCircle(x + w - 20, y, 20, 20, 1, baseColor);
                adapter.drawFilledCircle(x, y + h - 20 , 20, 20, 1, baseColor);
                adapter.drawFilledCircle(x + w - 20, y + h - 20, 20, 20, 1, baseColor);

                adapter.drawFilledRect(x, y + 10, w, h - 20, baseColor);
                adapter.drawFilledRect(x + 10, y, w - 20, h, baseColor);


                adapter.drawText(x + padding, y + padding - 3, text, font, color);

                if(isPressed) {
                    if (!buttonEvent.lock) {
                        buttonEvent.lock = true;
                        buttonEvent.buttonPressed = true;
                        return;
                    }
                    if(buttonEvent.buttonPressed) {
                        buttonEvent.buttonPressed = false;
                    }
                }




                if(buttonEvent.lock && surface.getMouseReleased(Input.MOUSE_BUTTON_LEFT)) {
                    buttonEvent.lock = false;
                    buttonEvent.buttonPressed = false;
                }



            }

            @Override
            public float getWidth() {
                return font.getWidthOf(text) + padding * 2;
            }

            @Override
            public float getHeight() {
                return font.getHeightOf(text) + padding * 2;
            }
        });

        ButtonEvent buttonEvent = getEvent(myID);
        if(buttonEvent == null) return false;
        else return buttonEvent.buttonPressed;
    }

    public static float slider(String text, Font font, Color color, float max, int... layoutInParent){
        int myID = getNewID();
        float padding = 7;

        submit(layoutInParent, new Widget() {
            @Override
            public void draw(Adapter adapter, float x, float y, float w, float h) {
                Map<Integer, Event> eventMap = contextBasedEventMap.get(currentContext);
                if(!eventMap.containsKey(myID)) {
                    addEvent(myID, new SliderEvent());
                }

                Color barColor = new Color(188f / 255, 188f / 255, 188f / 255, 1);
                Color grabColor = new Color(56f / 255, 56f / 255, 56f / 255, 1);


                SliderEvent sliderEvent = getEvent(myID);


                float barHeight = 6;
                float barWidth = w;

                float grabWidth = 15;
                float grabHeight = h - 10;

                Rect2D grab = new Rect2D(x, y + (h / 2) - (grabHeight / 2), grabWidth, grabHeight);
                grab.x += (sliderEvent.value / max) * w;


                boolean isPressed = surface.getMousePressed(Input.MOUSE_BUTTON_LEFT) && grab.contains(surface.getMousePos().x, surface.getMousePos().y);
                grab.x = Math.clamp(surface.getMousePos().x, x, x + w);


                adapter.drawFilledRect(x, y + (h / 2) - (barHeight / 2), barWidth, barHeight, barColor);
                adapter.drawFilledRect(grab.x, grab.y, grab.w, grab.h, grabColor);

                sliderEvent.value = max * ((grab.x - x) / w);

                String valueText = String.format("%.2f", sliderEvent.value);
                adapter.drawText(x + w - font.getWidthOf(valueText), y + grabHeight + 5, valueText, font, Color.WHITE);

            }

            @Override
            public float getWidth() {
                return font.getWidthOf(text) + padding * 2;
            }

            @Override
            public float getHeight() {

                return font.getHeightOf(text) + padding * 2;
            }
        });

        SliderEvent sliderEvent = getEvent(myID);
        if(sliderEvent == null) return 0f;
        else return sliderEvent.value;
    }

    public static int getNewID(){
        currentWidgetID++;
        return currentWidgetID;
    }
}
