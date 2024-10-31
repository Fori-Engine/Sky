package fori.ecs;

import fori.Scene;

import java.util.List;

public abstract class EntitySystem {

    public abstract void update(Scene scene, MessageQueue messageQueue);

    public void shutdown() {}
}
