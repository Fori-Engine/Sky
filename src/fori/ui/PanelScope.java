package fori.ui;

import java.util.ArrayList;

public class PanelScope {
    public ArrayList<fori.ui.Widget> childWidgets = new ArrayList<>();
    public int[] layoutInParent;
    public Layout layout;


    public PanelScope(Layout layout, int... layoutInParent) {
        this.layout = layout;
        this.layoutInParent = layoutInParent;
    }
}