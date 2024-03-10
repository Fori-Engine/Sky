package lake.editor;

public abstract class Panel {
    protected String title;

    protected Panel(String title) {
        this.title = title;
    }

    public abstract void render();

    public abstract void dispose();
}
