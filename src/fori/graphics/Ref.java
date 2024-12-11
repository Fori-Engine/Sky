package fori.graphics;

import fori.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Ref {
    public String name;
    public Disposable disposable;
    public List<Ref> children = new ArrayList<>();
    public Ref parent;

    public Ref(Disposable disposable){
        if(disposable != null)
            name = disposable.getClass().getName();
        this.disposable = disposable;
    }

    public Ref add(Disposable disposable) {
        return add(new Ref(disposable));
    }

    public void remove(Disposable disposable) {
        for (Iterator<Ref> iterator = children.iterator(); iterator.hasNext(); ) {
            Ref ref = iterator.next();
            if (ref.disposable == disposable) {
                Logger.info(Ref.class, "Stopped tracking reference to " + disposable.getClass().getSimpleName());

                iterator.remove();
            }
        }
    }

    public Ref add(Ref child){

        if(child == this) {
            throw new RuntimeException(Logger.error(Ref.class, "Adding a Ref to it's own child list is not allowed"));
        }
        child.parent = this;
        children.add(child);
        Logger.info(Ref.class, "Tracking reference to " + child.disposable.getClass().getSimpleName());
        return child;
    }

    public void destroyAll(){
        destroyAll(this);
    }

    public void destroyAll(Ref ref){
        System.out.println(getDepthString(ref) + " " + ref.disposable.getClass().getSimpleName());

        for(Ref child : ref.children){
            destroyAll(child);
        }

        if(ref.disposable != null) ref.disposable.dispose();
    }

    private String getDepthString(Ref ref){

        int i = 0;

        while(ref.parent != null){
            ref = ref.parent;
            i++;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int j = 0; j < i; j++) {
            if(j == i - 1) stringBuilder.append("|");
            else stringBuilder.append("  ");
        }

        return stringBuilder.toString();
    }


}
