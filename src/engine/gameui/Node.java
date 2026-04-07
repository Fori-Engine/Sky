package engine.gameui;

import engine.graphics.text.MsdfFont;

import static engine.gameui.TextValue.text;

public class Node extends ContainerWidget {
    private boolean expanded = true;
    private Button button;
    private TextValue value;
    public Node(TextValue value, MsdfFont msdfFont) {
        this.value = value;
        setLayoutEngine(new LineLayoutEngine(LineLayoutEngine.Line.Vertical));
        addWidget(button = new Button(value, msdfFont));

        button.addEventHandler(new EventHandler() {
            @Override
            public void onClick() {
                expanded = !expanded;
            }
        });

    }

    @Override
    public void onAdded() {
        if(parent instanceof Node) {
            setIgnore(true);
            setPadding(0);
        }
    }

    public TextValue getValue() {
        return value;
    }

    @Override
    public int getRequiredWidth() {
        if(expanded) return super.getRequiredWidth();
        else return button.getRequiredWidth();
    }

    @Override
    public int getRequiredHeight() {
        if(expanded) return super.getRequiredHeight();
        else return button.getRequiredHeight();
    }

    @Override
    public void update(GfxPlatform platform, int x, int y, int w, int h) {
        if(expanded)
            super.update(platform, x, y, w, h);
        else
            button.update(platform, x, y, w, h);
    }
}
