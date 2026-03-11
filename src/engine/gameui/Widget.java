package engine.gameui;

import java.util.LinkedList;
import java.util.List;

public abstract class Widget {
    private List<Widget> widgets = new LinkedList<>();
    private List<EventHandler> eventHandlers = new LinkedList<>();
    private long hints;
    private String name;
    protected LayoutEngine layoutEngine = new LayoutEngine() {
        @Override
        public int getComputedWidth() {
            return 0;
        }

        @Override
        public int getComputedHeight() {
            return 0;
        }

        @Override
        public void updateChildren(GfxPlatform platform, int x, int y) {

        }
    };

    public Widget addHint(long hint) {
        hints |= hint;
        return this;
    }

    public boolean hasHint(long hint) {
        return (hints & hint) != 0;
    }

    public Widget setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public <T> T getWidgetByPath(String... names) {

        Widget root = this;


        for(String name : names) {
            for(Widget widget : root.getWidgets()) {
                if(widget.getName() != null && widget.getName().equals(name)) {
                    root = widget;
                }
            }

        }

        return (T) root;
    }

    public abstract int getRequiredWidth();
    public abstract int getRequiredHeight();

    public abstract void update(GfxPlatform platform, int x, int y, int w, int h);
    public void updateChildren(GfxPlatform platform, int x, int y) {
        layoutEngine.updateChildren(platform, x, y);
    }
    public Widget setLayoutEngine(LayoutEngine layoutEngine) {
        this.layoutEngine = layoutEngine;
        layoutEngine.setWidget(this);
        return this;
    }
    public Widget addEventHandler(EventHandler eventHandler) {
        eventHandlers.add(eventHandler);
        return this;
    }

    public List<EventHandler> getEventHandlers() {
        return eventHandlers;
    }

    public void removeEventHandler(EventHandler eventHandler) {
        eventHandlers.remove(eventHandler);
    }

    public Widget addWidget(Widget widget) {
        widgets.add(widget);
        return this;
    }

    public List<Widget> getWidgets() {
        return widgets;
    }

    public Widget addWidgets(Widget... widgets) {
        for(Widget widget : widgets) addWidget(widget);
        return this;
    }


}
