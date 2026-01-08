package engine.ui;

import engine.Input;
import engine.Surface;
import engine.graphics.Color;
import engine.graphics.Font;
import engine.graphics.Rect2D;

import java.util.*;
import java.util.List;


public class AmberUI {

    private static Adapter currentAdapter;
    private static final HashMap<String, Event> eventMap = new HashMap<>();
    private static final Stack<PanelScope> panelScopes = new Stack<>();
    private static final Stack<WindowScope> windowScopes = new Stack<>();
    private static Theme currentTheme;
    private static Surface surface;
    private static Widget builderLastWidget;
    private static Map<String, Widget> runtimeWindows = new HashMap<>();
    private static List<Widget> runtimeWindowRenderList = new ArrayList<>();
    public static String builderLastWidgetType = "";
    private static String runtimeLastSelectedWindowID = null, runtimeSelectedWindowID = null;
    private static String builderCurrentWindowID;

    private static Stack<String> namespaces = new Stack<>();
    private static StringBuilder namespaceAssembly = new StringBuilder();
    private static Surface.Cursor cursor;



    public static final void setTheme(Theme theme) {
        AmberUI.currentTheme = theme;
    }

    public static final void setAdapter(Adapter adapter) { AmberUI.currentAdapter = adapter;}

    public static void setSurface(Surface surface) {
        AmberUI.surface = surface;
    }

    private static <T> T getEvent(String widgetID) {
        return (T) eventMap.get(widgetID);
    }

    private static void addEvent(String widgetID, Event event) {
        eventMap.put(widgetID, event);
    }

    public static void pushNamespace(String namespace) {
        namespaces.push(namespace);
    }

    public static void popNamespace() {
        namespaces.pop();
    }

    public static void newContext(){

    }

    public static void endContext(){
        runtimeWindowRenderList.clear();
        panelScopes.clear();
        windowScopes.clear();
        runtimeWindows.clear();
    }
    public static void clearEvents() {
        eventMap.clear();
    }


    public static void render() {


        for(Widget window : runtimeWindowRenderList) {
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
        String id = getNewID();
        windowScopes.push(new WindowScope(title, x, y, id, font));
        builderCurrentWindowID = id;
        newPanel(layout);
    }

    public static void endWindow() {
        endPanel();



        WindowScope windowScope = windowScopes.pop();
        Widget last = builderLastWidget;



        Widget widget = new Widget(windowScope.id, currentTheme.windowPadding) {
            @Override
            public void draw(Adapter adapter, float x, float y, float w, float h) {



                if(!eventMap.containsKey(windowScope.id)) {
                    addEvent(windowScope.id, new WindowEvent(windowScope.x, windowScope.y));
                }

                WindowEvent windowEvent = getEvent(windowScope.id);
                windowEvent.title = windowScope.title;

                Rect2D headerRect = new Rect2D(windowEvent.x, windowEvent.y - windowScope.font.getHeightOf(windowScope.title) - (currentTheme.windowHeaderPadding * 2), getWidth(), windowScope.font.getHeightOf(windowScope.title) + (currentTheme.windowHeaderPadding * 2));
                Rect2D clientRect = new Rect2D(windowEvent.x, windowEvent.y, getWidth(), getHeight());
                windowEvent.windowRect = new Rect2D(headerRect.x, headerRect.y, clientRect.w, headerRect.h + clientRect.h);


                Optional<Boolean> isWindowSelected = isHighestWindowForInput(id, (ignored, e) -> e.windowRect.contains(surface.getMousePos().x, surface.getMousePos().y) && surface.getMousePressed(Input.MOUSE_BUTTON_LEFT));

                if(isWindowSelected.isPresent() && isWindowSelected.get()) {
                    boolean inHeader = headerRect.contains(surface.getMousePos().x, surface.getMousePos().y);
                    boolean inClient = clientRect.contains(surface.getMousePos().x, surface.getMousePos().y);

                    if (!windowEvent.initialSelect && runtimeSelectedWindowID == null) {
                        if (inHeader) {
                            windowEvent.initialSelect = true;
                            windowEvent.sx = surface.getMousePos().x - windowEvent.x;
                            windowEvent.sy = surface.getMousePos().y - windowEvent.y;
                            runtimeSelectedWindowID = windowScope.id;
                            runtimeLastSelectedWindowID = runtimeSelectedWindowID;
                        }
                        if (inClient) {
                            runtimeSelectedWindowID = windowScope.id;
                            runtimeLastSelectedWindowID = runtimeSelectedWindowID;
                        }
                    }
                }



                if(surface.getMouseReleased(Input.MOUSE_BUTTON_LEFT)) {
                    windowEvent.initialSelect = false;
                    runtimeSelectedWindowID = null;
                }


                adapter.drawFilledRect(clientRect.x, clientRect.y, clientRect.w, clientRect.h, currentTheme.windowBackground);

                last.draw(adapter, windowEvent.x + getPadding(), windowEvent.y + getPadding(), getDrawableWidth(), getDrawableHeight());

                adapter.drawFilledRect(headerRect.x, headerRect.y, headerRect.w, headerRect.h, currentTheme.windowHeaderBackground);
                adapter.drawText(headerRect.x + currentTheme.windowHeaderPadding, headerRect.y + currentTheme.windowHeaderPadding, windowScope.title, windowScope.font, Color.WHITE);
                if(windowEvent.initialSelect) {
                    windowEvent.x =  surface.getMousePos().x - (windowEvent.sx);
                    windowEvent.y =  surface.getMousePos().y - (windowEvent.sy);
                }




                Optional<Boolean> showCursor = isHighestWindowForInput(id, (ignored, e) -> {
                    if(e.windowRect.contains(surface.getMousePos().x, surface.getMousePos().y)) {
                        float indentSize = 10;
                        float offset = 5;

                        Rect2D leftIndent = new Rect2D(windowEvent.windowRect.x - indentSize + offset, windowEvent.windowRect.y, indentSize, windowEvent.windowRect.h);
                        Rect2D rightIndent = new Rect2D(windowEvent.windowRect.x + windowEvent.windowRect.w - indentSize + offset, windowEvent.windowRect.y, indentSize, windowEvent.windowRect.h);

                        if (leftIndent.contains(surface.getMousePos().x, surface.getMousePos().y)) {
                            windowEvent.cursor = Surface.Cursor.ResizeWE;
                            adapter.drawFilledRect(leftIndent.x, leftIndent.y, leftIndent.w, leftIndent.h, Color.RED);
                            return true;
                        }
                        else if (rightIndent.contains(surface.getMousePos().x, surface.getMousePos().y)) {
                            windowEvent.cursor = Surface.Cursor.ResizeWE;
                            adapter.drawFilledRect(rightIndent.x, rightIndent.y, rightIndent.w, rightIndent.h, Color.RED);
                            return true;
                        }

                    }

                    return false;
                });
                if(showCursor.isPresent()) {
                    if(showCursor.get()) {
                        System.out.println(windowEvent.title + " is cursed");
                    }
                }
                else {
                    System.out.println("No window is cursed");
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
        runtimeWindows.put(windowScope.id, widget);
        runtimeWindowRenderList.add(widget);



        submit(new int[]{}, widget);

        builderLastWidget = widget;
        builderLastWidgetType = "window";
        builderCurrentWindowID = null;
    }

    public static void newPanel(Layout layout, int... layoutInParent){
        String id = getNewID();
        panelScopes.push(new PanelScope(id, layout, layoutInParent));
    }

    public static void endPanel(){
        PanelScope panelScope = panelScopes.pop();
        Widget last = builderLastWidget;


        Widget widget = new Widget(panelScope.id, currentTheme.panelPadding) {
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

        builderLastWidget = widget;
        builderLastWidgetType = "panel";
    }

    public static void text(String text, Font font, int... layoutInParent){
        String id = getNewID();

        Widget last = builderLastWidget;

        Widget widget = new Widget(id, currentTheme.textPadding) {
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

        builderLastWidget = widget;
        builderLastWidgetType = "text";
    }

    public static Optional<Boolean> isHighestWindowForInput(String windowID, InputFunction inputFunction) {
        //Find all the windows that contain the mouse
        //Whichever window is highest in the queue gets to process the event, and nobody else




        int highestIndex = -1;

        for(Widget window : runtimeWindowRenderList) {
            WindowEvent windowEvent = getEvent(window.id);

            if (windowEvent != null) {

                if (inputFunction.input(window, windowEvent)) {
                    int index = runtimeWindowRenderList.indexOf(window);
                    if (index > highestIndex) highestIndex = index;
                }
            }
        }
        if(highestIndex == -1) return Optional.empty();
        return Optional.of(runtimeWindowRenderList.get(highestIndex).id == windowID);
    }



    public static boolean button(String text, Font font, int... layoutInParent){
        String id = getNewID();

        Widget last = builderLastWidget;
        String currentWindowID = builderCurrentWindowID;

        Widget widget = new Widget(id, currentTheme.buttonPadding) {
            @Override
            public void draw(Adapter adapter, float x, float y, float w, float h) {

                if(!eventMap.containsKey(id)) {
                    addEvent(id, new ButtonEvent(false));
                }

                ButtonEvent buttonEvent = getEvent(id);
                Color baseColor = currentTheme.buttonIdleBackground;


                boolean hovered, pressed = false;


                Optional<Boolean> isWindowSelected = isHighestWindowForInput(currentWindowID, (ignored, e) -> e.windowRect.contains(surface.getMousePos().x, surface.getMousePos().y));





                if(isWindowSelected.isPresent() && isWindowSelected.get()) {

                    hovered = new Rect2D(x, y, w, h).contains(surface.getMousePos().x, surface.getMousePos().y);
                    if(hovered) baseColor = currentTheme.buttonHoverBackground;

                    pressed = surface.getMousePressed(Input.MOUSE_BUTTON_LEFT) && hovered;
                    if(pressed) baseColor = currentTheme.buttonClickBackground;
                }




                float circleW = 13;


                adapter.drawFilledCircle(x, y, circleW, circleW, 1, baseColor);
                adapter.drawFilledCircle(x + w - circleW, y, circleW, circleW, 1, baseColor);
                adapter.drawFilledCircle(x, y + h - circleW , circleW, circleW, 1, baseColor);
                adapter.drawFilledCircle(x + w - circleW, y + h - circleW, circleW, circleW, 1, baseColor);

                adapter.drawFilledRect(x, y + circleW / 2, w, h - circleW, baseColor);
                adapter.drawFilledRect(x + circleW / 2, y, w - circleW, h, baseColor);
                adapter.drawText(x + getPadding(), y + getPadding(), text, font, currentTheme.buttonForeground);



                if(currentWindowID == runtimeLastSelectedWindowID) {
                    if (pressed) {
                        if (!buttonEvent.lock) {
                            buttonEvent.lock = true;
                            buttonEvent.buttonPressed = true;
                            return;
                        }
                        if (buttonEvent.buttonPressed) {
                            buttonEvent.buttonPressed = false;
                        }
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

        builderLastWidget = widget;
        builderLastWidgetType = "button";

        ButtonEvent buttonEvent = getEvent(id);
        if(buttonEvent == null) return false;
        else return buttonEvent.buttonPressed;
    }




    public static String getNewID(){
        namespaceAssembly.setLength(0);

        for(String namespace : namespaces) {
            namespaceAssembly.append(namespace).append('/');
        }


        return namespaceAssembly.toString() + StackWalker.getInstance(StackWalker.Option.SHOW_HIDDEN_FRAMES).walk(
                (s) -> s.skip(2).findFirst()).get().getLineNumber();
    }
}
