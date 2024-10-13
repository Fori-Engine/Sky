package fori.ecs;

import java.util.List;

public abstract class EntitySystem {

    public abstract void update(Engine ecs, MessageQueue messageQueue);

    public void shutdown() {}
}
