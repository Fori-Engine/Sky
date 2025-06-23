package fori.graphics.ecs;

import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import dev.dominion.ecs.api.Scheduler;
import fori.graphics.Renderer;
import fori.graphics.StaticMeshBatch;
import org.lwjgl.system.linux.Stat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Scene {
    private Dominion dominion;
    private ArrayList<Runnable> systems = new ArrayList<>();
    private Scheduler scheduler;
    private Map<String, StaticMeshBatch> staticMeshBatches = new HashMap<>();

    public Scene(String name) {
        dominion = Dominion.create(name);
        scheduler = dominion.createScheduler();
    }

    public void start(int tickRate) {
        scheduler.tickAtFixedRate(tickRate);
    }

    public Entity createEntity(Object... components) {
        return dominion.createEntity(components);
    }

    public void removeEntity(Entity entity) {
        dominion.deleteEntity(entity);
    }

    public void addSystem(Runnable system) {
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


    public void removeSystem(Runnable system) {
        systems.remove(system);
    }


    public void close() {
        scheduler.shutDown();
        dominion.close();
    }
}
