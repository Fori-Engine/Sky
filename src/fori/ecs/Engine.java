package fori.ecs;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class Engine {
    private final List<EntitySystem> systems = new ArrayList<>();
    private final List<Component> components = new ArrayList<>();
    private final List<Pair<Integer, Integer>> views = new ArrayList<>();


    private int currentComponentOffset = 0;




    private final MessageQueue messageQueue = new MessageQueue();

    public Engine(EntitySystem... systems){
        Collections.addAll(this.systems, systems);
    }

    public void addComponents(Entity entity, Component... components){
        if((entity.id + 1) > views.size()) views.add(entity.id, new Pair<>(currentComponentOffset, 0));


        Pair<Integer, Integer> componentRange = views.get(entity.id);
        componentRange.b = componentRange.a + components.length;

        this.components.addAll(List.of(components));

        currentComponentOffset += components.length;
    }

    public <T> void view(Class<T> componentClass, ViewFunction<T> viewFunction){
        for(Pair<Integer, Integer> view : views){
            for (int i = view.a; i < view.b; i++) {

                Component component = components.get(i);

                if(component.getClass() == componentClass || componentClass.isAssignableFrom(component.getClass())) {
                    viewFunction.onViewEntry((T) component);
                }



            }
        }



    }


    public List<Component> getComponents() {
        return components;
    }

    public List<Pair<Integer, Integer>> getViews() {
        return views;
    }

    public void update() {
        for(EntitySystem system : systems)
            system.update(this, messageQueue);
    }

    public void shutdown(){
        for(EntitySystem system : systems){
            system.shutdown();
        }
    }
}
