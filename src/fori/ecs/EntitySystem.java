package fori.ecs;

public abstract class EntitySystem {
    public abstract void process(Entity entity, MessageBus messageBus);

    public void update(){}

    public void shutdown() {}
}
