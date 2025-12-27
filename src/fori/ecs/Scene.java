package fori.ecs;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Scheduler;
import fori.graphics.Renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scene {
    private Dominion dominion;
    private ArrayList<EcsSystem> systems = new ArrayList<>();
    private Scheduler scheduler;

    public Scene(String name) {
        dominion = Dominion.create(name);
        scheduler = dominion.createScheduler();
    }

    public void tick() {
        scheduler.tick();
    }


    public Entity createEntity(Object... components) {
        return dominion.createEntity(components);
    }

    public void removeEntity(Entity entity) {
        dominion.deleteEntity(entity);
    }

    public void addSystem(EcsSystem system) {
        systems.add(system);
        scheduler.schedule(system);
    }

    public Dominion getEngine() {
        return dominion;
    }


    public void removeSystem(Runnable system) {
        systems.remove(system);
    }


    public void close(Renderer renderer) {

        for(EcsSystem system : systems) {
            system.dispose();
        }

        scheduler.shutDown();
        dominion.close();
    }


}
