package engine;

import engine.graphics.Disposable;

public abstract class Stage extends Disposable {
    protected Surface surface;

    public Stage() {
        super(null);
    }

    public void launch(String[] args, Surface surface){
        init(args, surface);
    }


    public abstract void closing();

    public void close(){
        disposeAll();
    }

    public void init(String[] args, Surface surface) {
        this.surface = surface;
    }
    public abstract boolean update();
    public abstract void dispose();
}
