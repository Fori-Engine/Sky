package fori.graphics;

import fori.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class Disposable {
    public Disposable parent;
    public List<Disposable> children = new ArrayList<>();

    public Disposable(Disposable parent){
        this.parent = parent;
        if(parent != null) parent.add(this);
    }

    public void remove(Disposable child) {
        for (Iterator<Disposable> iterator = children.iterator(); iterator.hasNext(); ) {
            Disposable ref = iterator.next();
            if (ref == child) {
                iterator.remove();
            }
        }
    }

    public void add(Disposable child){

        if(child == this) {
            throw new RuntimeException(Logger.error(Disposable.class, "Adding a Disposable to it's own child list is not allowed"));
        }
        child.parent = this;
        children.add(child);

    }

    public List<Disposable> getChildren() {
        return children;
    }

    public void disposeAll(){
        disposeRecursive(this);
    }

    public void disposeRecursive(Disposable disposable){

        for(Disposable child : disposable.children){
            disposeRecursive(child);
        }

        disposable.dispose();
    }

    public abstract void dispose();

    private String getDepthString(Disposable ref){

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
