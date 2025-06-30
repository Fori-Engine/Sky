package fori.ecs;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Scheduler;
import fori.graphics.Renderer;
import fori.graphics.StaticMeshBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Scene {
    private Dominion dominion;
    private ArrayList<EcsSystem> systems = new ArrayList<>();
    private Scheduler scheduler;
    private Map<String, StaticMeshBatch> staticMeshBatches = new HashMap<>();

    public Scene(String name) {
        dominion = Dominion.create(name);
        scheduler = dominion.createScheduler();
    }

    public void tick() {
        scheduler.tick();
    }

    public Map<String, StaticMeshBatch> getStaticMeshBatches() {
        return staticMeshBatches;
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

    public void registerStaticMeshBatch(String name, StaticMeshBatch staticMeshBatch) {
        staticMeshBatches.put(name, staticMeshBatch);
    }

    public void removeStaticMeshBatch(Renderer renderer, String name) {
        StaticMeshBatch staticMeshBatch = staticMeshBatches.get(name);
        renderer.destroyStaticMeshBatch(staticMeshBatch);
        staticMeshBatches.remove(name);
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

        for(StaticMeshBatch staticMeshBatch : staticMeshBatches.values()) {
            renderer.destroyStaticMeshBatch(staticMeshBatch);
        }

        dominion.findEntitiesWith(DynamicMeshComponent.class).stream().forEach(components -> {
            DynamicMeshComponent dynamicMeshComponent = components.comp();
            renderer.destroyDynamicMesh(dynamicMeshComponent.dynamicMesh());
        });




        scheduler.shutDown();
        dominion.close();
    }
}
