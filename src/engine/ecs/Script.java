package engine.ecs;


public abstract class Script {
    public boolean initialized;
    public abstract void init(Actor actor);
    public abstract void update(Actor actor);
}
