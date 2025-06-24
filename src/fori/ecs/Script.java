package fori.ecs;


import dev.dominion.ecs.api.Entity;

public interface Script {
    void init(Entity entity);
    void update(Entity entity);
}
