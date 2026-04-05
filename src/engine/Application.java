package engine;

import engine.graphics.Disposable;

public abstract class Application extends Disposable {
    protected Surface surface;

    public Application() {
        super(null);
    }

    public void launch(String[] args, Surface surface){
        init(args, surface);
    }

    public void close(){
        //disposeAll();
    }

    public void init(String[] args, Surface surface) {
        this.surface = surface;
    }
    public abstract boolean update();
    public abstract void dispose();
}
