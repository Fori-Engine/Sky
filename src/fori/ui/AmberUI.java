package fori.ui;

import fori.Input;
import fori.Surface;
import fori.graphics.Color;
import fori.graphics.Font;
import fori.graphics.Rect2D;

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
        submit(layoutInParent, new Widget() {
            @Override
            public void draw(Adapter adapter, float x, float y, float w, float h) {
                adapter.drawText(x, y, text, font, color);
            }

            @Override
            public float getWidth() {
                return font.getWidthOf(text);
            }

            @Override
            public float getHeight() {
                return font.getHeightOf(text);
            }
        });
    }

    public static boolean button(String text, Font font, Color color, int... layoutInParent){
        int myID = getNewID();
        submit(layoutInParent, new Widget() {
            @Override
            public void draw(Adapter adapter, float x, float y, float w, float h) {
                Map<Integer, Event> eventMap = contextBasedEventMap.get(currentContext);
                if(!eventMap.containsKey(myID)) {
                    addEvent(myID, new ButtonEvent());
                }

                ButtonEvent buttonEvent = getEvent(myID);

                boolean isPressed = surface.getMousePressed(Input.MOUSE_BUTTON_LEFT) && new Rect2D(x, y, w, h).contains(surface.getMousePos().x, surface.getMousePos().y);

                if(isPressed) {
                    if (!buttonEvent.lock) {
                        buttonEvent.lock = true;
                        buttonEvent.buttonPressed = true;
                        return;
                    }
                    if(buttonEvent.buttonPressed) {
                        buttonEvent.buttonPressed = false;
                    }
                    adapter.drawFilledRect(x, y, w, h, Color.LIGHT_GRAY);
                }
                else {
                    adapter.drawFilledRect(x, y, w, h, Color.BLACK);
                }
                adapter.drawText(x, y, text, font, color);



                if(buttonEvent.lock && surface.getMouseReleased(Input.MOUSE_BUTTON_LEFT)) {
                    buttonEvent.lock = false;
                    buttonEvent.buttonPressed = false;
                }



            }

            @Override
            public float getWidth() {
                return font.getWidthOf(text);
            }

            @Override
            public float getHeight() {
                return font.getHeightOf(text);
            }
        });

        ButtonEvent buttonEvent = getEvent(myID);
        if(buttonEvent == null) return false;
        else return buttonEvent.buttonPressed;
    }

    public static int getNewID(){
        currentWidgetID++;
        return currentWidgetID;
    }
}
