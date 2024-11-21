package fori.ui;

import fori.Input;
import fori.Math;
import fori.Surface;
import fori.graphics.Color;
import fori.graphics.Font;
import fori.graphics.Rect2D;

import java.util.*;


public class AmberUI {

    private static Adapter currentAdapter;
    private static final HashMap<Integer, Event> eventMap = new HashMap<>();
    private static final Stack<PanelScope> panelScopes = new Stack<>();
    private static final Stack<WindowScope> windowScopes = new Stack<>();
    private static Theme currentTheme;


    private static Surface surface;
    private static Widget lastWidget;
    private static List<Widget> windows = new ArrayList<>();
    public static String lastWidgetType = "";
    private static Widget selectedWindow;

    public static final void setTheme(Theme theme) {
        AmberUI.currentTheme = theme;
    }

    public static final void setAdapter(Adapter adapter) { AmberUI.currentAdapter = adapter;}

    public static void setSurface(Surface surface) {
        AmberUI.surface = surface;
    }

    private static <T> T getEvent(int widgetID) {
        return (T) eventMap.get(widgetID);
    }

    private static void addEvent(int widgetID, Event event) {
        eventMap.put(widgetID, event);
    }


    public static void newContext(){

    }

    public static void endContext(){
        windows.clear();
        panelScopes.clear();
    }
    public static void clearEvents() {
        eventMap.clear();
    }


    public static void render() {


        for(Widget window : windows) {
            window.draw(currentAdapter, 0, 0, window.getWidth(), window.getHeight());
        }

    }

    public static void submit(int[] layoutHints, Widget widget){
        widget.layoutHints = layoutHints;
        if(!panelScopes.isEmpty()) {
            panelScopes.peek().childWidgets.add(widget);
        }
    }

    public static void newWindow(String title, float x, float y, Font font, Layout layout) {
        int myID = getNewID();
        windowScopes.push(new WindowScope(title, x, y, myID, font));
        newPanel(layout);
    }

    public static void endWindow() {
        endPanel();

        WindowScope windowScope = windowScopes.pop();

        Widget last = lastWidget;


        Widget widget = new Widget(currentTheme.windowPadding) {
            @Override
            public void draw(Adapter adapter, float x, float y, float w, float h) {
                if(!eventMap.containsKey(windowScope.myID)) {
                    addEvent(windowScope.myID, new WindowEvent(windowScope.x, windowScope.y));
                }

                WindowEvent windowEvent = getEvent(windowScope.myID);

                Rect2D headerRect = new Rect2D(windowEvent.x, windowEvent.y - windowScope.font.getHeightOf(windowScope.title) - (currentTheme.windowHeaderPadding * 2), getWidth(), windowScope.font.getHeightOf(windowScope.title) + (currentTheme.windowHeaderPadding * 2));
                Rect2D windowRect = new Rect2D(windowEvent.x, windowEvent.y, getWidth(), getHeight());
                Rect2D combinedRect = new Rect2D(headerRect.x, headerRect.y, windowRect.w, headerRect.h + windowRect.h);

                for(int shadowIter = 0; shadowIter < currentTheme.windowShadowCount; shadowIter++) {
                    adapter.drawFilledRect(combinedRect.x - shadowIter, combinedRect.y - shadowIter, combinedRect.w + shadowIter * 2, combinedRect.h + shadowIter * 2, new Color(0, 0, 0, (float) 1 / shadowIter + 0.01f));
                }



                boolean selected = headerRect.contains(surface.getMousePos().x, surface.getMousePos().y) && surface.getMousePressed(Input.MOUSE_BUTTON_LEFT);

                if(selected && !windowEvent.initialSelect && selectedWindow == null) {
                    windowEvent.initialSelect = true;
                    windowEvent.sx = surface.getMousePos().x - windowEvent.x;
                    windowEvent.sy = surface.getMousePos().y - windowEvent.y;
                    selectedWindow = this;
                }

                if(surface.getMouseReleased(Input.MOUSE_BUTTON_LEFT)) {
                    windowEvent.initialSelect = false;
                    selectedWindow = null;
                }

                adapter.drawFilledRect(headerRect.x, headerRect.y, headerRect.w, headerRect.h, currentTheme.windowHeaderBackground);

                adapter.drawFilledRect(windowRect.x, windowRect.y, windowRect.w, windowRect.h, currentTheme.windowBackground);
                adapter.drawText(headerRect.x + currentTheme.windowHeaderPadding, headerRect.y + currentTheme.windowHeaderPadding, windowScope.title, windowScope.font, Color.WHITE);

                last.draw(adapter, windowEvent.x + getPadding(), windowEvent.y + getPadding(), getDrawableWidth(), getDrawableHeight());

                if(windowEvent.initialSelect) {
                    windowEvent.x =  surface.getMousePos().x - (windowEvent.sx);
                    windowEvent.y =  surface.getMousePos().y - (windowEvent.sy);
                }
            }

            @Override
            public float getWidth() {
                return java.lang.Math.max(windowScope.font.getWidthOf(windowScope.title), last.getWidth()) + getPadding() * 2 + (currentTheme.windowHeaderPadding * 2);
            }

            @Override
            public float getHeight() {
                return last.getHeight() + getPadding() * 2;

            }
        };
        windows.add(widget);
        submit(new int[]{}, widget);

        lastWidget = widget;
        lastWidgetType = "window";

    }

    public static void newPanel(Layout layout, int... layoutInParent){
        int myID = getNewID();
        panelScopes.push(new PanelScope(layout, layoutInParent));
    }

    public static void endPanel(){
        PanelScope panelScope = panelScopes.pop();
        Widget last = lastWidget;


        Widget widget = new Widget(currentTheme.panelPadding) {
            @Override
            public void draw(Adapter adapter, float x, float y, float w, float h) {
                panelScope.layout.layoutAndDraw(panelScope.childWidgets, panelScope, adapter, x + getPadding(), y + getPadding(), getDrawableWidth(), getDrawableHeight());
            }

            @Override
            public float getWidth() {
                return panelScope.layout.getWidth(panelScope.childWidgets) + getPadding() * 2;
            }

            @Override
            public float getHeight() {
                return panelScope.layout.getHeight(panelScope.childWidgets) + getPadding() * 2;
            }
        };

        submit(panelScope.layoutInParent, widget);

        lastWidget = widget;
        lastWidgetType = "panel";
    }

    public static void text(String text, Font font, int... layoutInParent){
        int myID = getNewID();

        Widget last = lastWidget;

        Widget widget = new Widget(currentTheme.textPadding) {
            @Override
            public void draw(Adapter adapter, float x, float y, float w, float h) {
                adapter.drawText(x + getPadding(), y + getPadding(), text, font, currentTheme.textForeground);
            }

            @Override
            public float getWidth() {
                return font.getWidthOf(text) + getPadding() * 2;
            }

            @Override
            public float getHeight() {
                return font.getHeightOf(text) + getPadding() * 2;

            }
        };
        submit(layoutInParent, widget);

        lastWidget = widget;
        lastWidgetType = "text";
    }

    public static boolean button(String text, Font font, int... layoutInParent){
        int myID = getNewID();

        Widget last = lastWidget;

        Widget widget = new Widget(currentTheme.buttonPadding) {
            @Override
            public void draw(Adapter adapter, float x, float y, float w, float h) {

                if(!eventMap.containsKey(myID)) {
                    addEvent(myID, new ButtonEvent(false));
                }



                ButtonEvent buttonEvent = getEvent(myID);



                boolean hovered = new Rect2D(x, y, w, h).contains(surface.getMousePos().x, surface.getMousePos().y);
                boolean isPressed = surface.getMousePressed(Input.MOUSE_BUTTON_LEFT) && hovered;

                Color baseColor = currentTheme.buttonIdleBackground;
                if(hovered) baseColor = currentTheme.buttonHoverBackground;
                if(isPressed) baseColor = currentTheme.buttonClickBackground;


                float circleW = 13;


                adapter.drawFilledCircle(x, y, circleW, circleW, 1, baseColor);
                adapter.drawFilledCircle(x + w - circleW, y, circleW, circleW, 1, baseColor);
                adapter.drawFilledCircle(x, y + h - circleW , circleW, circleW, 1, baseColor);
                adapter.drawFilledCircle(x + w - circleW, y + h - circleW, circleW, circleW, 1, baseColor);

                adapter.drawFilledRect(x, y + circleW / 2, w, h - circleW, baseColor);
                adapter.drawFilledRect(x + circleW / 2, y, w - circleW, h, baseColor);



                //adapter.drawFilledRect(x, y, w, h, baseColor);



                adapter.drawText(x + getPadding(), y + getPadding(), text, font, currentTheme.buttonForeground);

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
                return font.getWidthOf(text) + getPadding() * 2;
            }

            @Override
            public float getHeight() {
                return font.getHeightOf(text) + getPadding() * 2;
            }
        };
        submit(layoutInParent, widget);

        lastWidget = widget;
        lastWidgetType = "button";

        ButtonEvent buttonEvent = getEvent(myID);
        if(buttonEvent == null) return false;
        else return buttonEvent.buttonPressed;
    }

    public static float slider(String text, Font font, Color color, float max, int... layoutInParent){
        int myID = getNewID();
        float padding = 7;
        Widget last = lastWidget;



        Widget widget = new Widget() {
            @Override
            public void draw(Adapter adapter, float x, float y, float w, float h) {
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
        };

        submit(layoutInParent, widget);

        lastWidget = widget;
        lastWidgetType = "slider";
        System.out.println(lastWidgetType);


        SliderEvent sliderEvent = getEvent(myID);
        if(sliderEvent == null) return 0f;
        else return sliderEvent.value;
    }

    public static int getNewID(){
        return StackWalker.getInstance(StackWalker.Option.SHOW_HIDDEN_FRAMES).walk(
                (s) -> s.skip(2).findFirst()).get().getLineNumber();
    }
}
