package engine.ecs;

import engine.Logger;

public class Actor {
    private Object[] components = new Object[64];
    private long mask = 0L;
    private Actor[] children = new Actor[64];
    private int nextChildIndex;
    private String name;

    public Actor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static void tryClassload(Class<?> clazz) {
        Logger.info(Actor.class, "Trying to load class: " + clazz.getSimpleName());
    }

    public Actor addActor(Actor... actor) {
        for(Actor a : actor) {
            children[nextChildIndex++] = a;
        }

        return this;
    }

    public void previsitAllActors(ActorPreVisitor visitor) {
        visitor.visit(this);
        visitRecursive(visitor, this, true);
    }

    public void postvisitAllActors(ActorPostVisitor visitor) {
        visitRecursive(visitor, this, false);
        visitor.visit(this);
    }

    private void visitRecursive(ActorVisitor visitor, Actor actor, boolean pre) {
        Actor[] actors = actor.children;
        for (int i = 0; i < actors.length; i++) {
            Actor child = actors[i];

            if(child != null) {
                if(pre) visitor.visit(child);
                visitRecursive(visitor, child, pre);
                if(!pre) visitor.visit(child);
            }
        }
    }


    public void add(Object component) {
        int index = getIndex(component.getClass());
        components[index] = component;
        mask |= getMask(component.getClass());
    }

    public boolean has(Class<?> clazz) {
        return (mask & getMask(clazz)) != 0;
    }

    public void remove(Class<?> clazz) {
        int index = getIndex(clazz);
        components[index] = null;
        mask = mask & ~getMask(clazz);
    }

    public <T> T getComponent(Class<T> c) {
        return ((T) components[getIndex(c)]);
    }

    public static long getMask(Class<?> clazz) {
        return clazz.getAnnotation(ComponentArray.class).mask();
    }

    public long getMask() {
        return mask;
    }

    public static int getIndex(Class<?> clazz) {
        return 64 - Long.numberOfTrailingZeros(getMask(clazz)) - 1;
    }

}
