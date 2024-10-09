package fori.ecs;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class Engine {
    private final ArrayList<EntitySystem> systems = new ArrayList<>();
    private final MessageBus messageBus = new MessageBus();

    public Engine(EntitySystem... systems){
        Collections.addAll(this.systems, systems);
    }

    public void update(ArrayList<Entity> entities) {


        for(EntitySystem system : systems)
            system.update();

        for (Iterator<Entity> iterator = entities.iterator(); iterator.hasNext(); ) {
            Entity entity = iterator.next();

            if (!entity.dead) {
                for (EntitySystem s : systems) {
                    s.process(entity, messageBus);
                }
            }
            else {
                iterator.remove();
            }
        }




    }

    public void shutdown(){
        for(EntitySystem system : systems){
            system.shutdown();
        }
    }
}
