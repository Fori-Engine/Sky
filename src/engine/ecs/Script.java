package engine.ecs;


import dev.dominion.ecs.api.Entity;

public abstract class Script {
    public boolean initialized;
    public abstract void init(Entity entity);
    public abstract void update(Entity entity);
}
