package fori.ui;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PanelScope {
    public List<Widget> childWidgets = new LinkedList<>();
    public int[] layoutInParent;
    public Layout layout;
    public int id;


    public PanelScope(int id, Layout layout, int... layoutInParent) {
        this.id = id;
        this.layout = layout;
        this.layoutInParent = layoutInParent;
    }
}