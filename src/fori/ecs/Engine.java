package fori.ecs;


import fori.Scene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Engine {
    private final List<EntitySystem> systems = new ArrayList<>();
    private final MessageQueue messageQueue = new MessageQueue();

    public Engine(EntitySystem... systems){
        Collections.addAll(this.systems, systems);
    }


    public void update(Scene scene) {
        for(EntitySystem system : systems)
            system.update(scene, messageQueue);
    }

    public void shutdown(){
        for(EntitySystem system : systems){
            system.shutdown();
        }
    }
}
