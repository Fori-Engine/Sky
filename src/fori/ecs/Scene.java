package fori.ecs;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Scheduler;
import fori.graphics.DynamicMesh;
import fori.graphics.Renderer;
import fori.graphics.StaticMeshBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scene {
    private Dominion dominion;
    private ArrayList<EcsSystem> systems = new ArrayList<>();
    private Scheduler scheduler;
    private Map<String, StaticMeshBatch> staticMeshBatches = new HashMap<>();
    private List<DynamicMesh> dynamicMeshes = new ArrayList<>();


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

    public void registerStaticMeshBatch(String name, StaticMeshBatch staticMeshBatch) {
        staticMeshBatches.put(name, staticMeshBatch);
    }

    public void removeStaticMeshBatch(String name, Renderer renderer) {
        renderer.destroyStaticMeshBatch(staticMeshBatches.get(name));
        staticMeshBatches.remove(name);
    }

    public Map<String, StaticMeshBatch> getStaticMeshBatches() {
        return staticMeshBatches;
    }

    public void registerDynamicMesh(DynamicMesh dynamicMesh) {
        dynamicMeshes.add(dynamicMesh);
    }

    public void removeDynamicMesh(DynamicMesh dynamicMesh, Renderer renderer) {
        renderer.destroyDynamicMesh(dynamicMesh);
        dynamicMeshes.remove(dynamicMesh);
    }

    public List<DynamicMesh> getDynamicMeshes() {
        return dynamicMeshes;
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

        for(DynamicMesh dynamicMesh : dynamicMeshes) {
            renderer.destroyDynamicMesh(dynamicMesh);
        }



        dominion.findEntitiesWith(DynamicMeshComponent.class).stream().forEach(components -> {
            DynamicMeshComponent dynamicMeshComponent = components.comp();
            renderer.destroyDynamicMesh(dynamicMeshComponent.dynamicMesh());
        });




        scheduler.shutDown();
        dominion.close();
    }


}
