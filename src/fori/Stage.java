package fori;

import fori.graphics.Disposable;
import fori.graphics.Ref;

import java.io.File;

public abstract class Stage {
    private Ref rootRef;
    protected Surface surface;


    public Stage() {
        rootRef = new Ref(new Disposable() {
            @Override
            public void dispose() {
                Logger.info(Stage.class, "The Stage will now be disposed");
            }

            @Override
            public Ref getRef() {
                return rootRef;
            }
        });

        Logger.setConsoleTarget(System.out);
    }

    public void launch(String[] args, Surface surface){


        init(args, surface);
    }

    public Ref getStageRef() { return rootRef; }

    public void close(){
        dispose();
        rootRef.destroyAll();
    }

    public void init(String[] args, Surface surface) {
        this.surface = surface;
    }
    public abstract boolean update();
    public abstract void dispose();
}
