package fori;

import fori.graphics.Disposable;
import fori.graphics.Ref;

public abstract class Stage {
    private Ref rootRef;



    public void launch(String[] args){
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

        init(args);
    }

    public Ref getStageRef() { return rootRef; }

    public void close(){
        dispose();
        rootRef.destroyAll();
    }

    public abstract void init(String[] args);
    public abstract boolean update();
    public abstract void dispose();
}
