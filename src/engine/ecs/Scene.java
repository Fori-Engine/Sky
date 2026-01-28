package engine.ecs;


import java.util.ArrayList;
import java.util.List;

public class Scene {
    private List<Entity> entities = new ArrayList<>();
    private List<EcsSystem> systems = new ArrayList<>();

    public Scene(String name) {

    }

    public void tick() {
        for(EcsSystem system : systems) {
            system.run(entities);
        }
    }


    public Entity createEntity(Object... components) {
        Entity entity = new Entity();
        for(Object component : components)
            entity.add(component);

        entities.add(entity);
        return entity;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void removeEntity(Entity entity) {
        entities.remove(entity);
    }

    public void addSystem(EcsSystem system) {
        systems.add(system);
    }


    public void removeSystem(Runnable system) {
        systems.remove(system);
    }


    public void close() {
        for(EcsSystem system : systems) {
            system.dispose();
        }
    }


}
