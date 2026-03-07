package engine.ecs;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Scene {
    private List<ActorSystem> systems = new ArrayList<>();
    private Actor root;
    private String name;

    public Scene(String name) {
        this.name = name;
    }

    public void tick() {
        for(ActorSystem system : systems) {
            system.run(root);
        }
    }

    public Actor getRootActor() {
        return root;
    }

    public void setRootActor(Actor root) {
        this.root = root;
    }

    public Actor newActor(String name, Object... components) {
        Actor actor = new Actor(name);
        for(Object component : components)
            actor.add(component);

        return actor;
    }

    public Actor newRootActor(String name) {
        return new Actor(name);
    }



    public void addSystem(ActorSystem... actorSystems) {
        this.systems.addAll(Arrays.asList(actorSystems));
    }


    public void removeSystem(ActorSystem actorSystem) {
        systems.remove(actorSystem);
    }


    public void close() {
        for(ActorSystem system : systems) {
            system.dispose();
        }
    }


}
