package fori.ui;

import java.util.ArrayList;

public class PanelScope {
    public ArrayList<fori.ui.Widget> childWidgets = new ArrayList<>();
    public int[] layoutInParent;
    public Layout layout;
    public int id;


    public PanelScope(int id, Layout layout, int... layoutInParent) {
        this.id = id;
        this.layout = layout;
        this.layoutInParent = layoutInParent;
    }
}