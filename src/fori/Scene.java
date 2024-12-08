package fori;

import fori.ecs.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Scene {

    private List<Entity> entities = new LinkedList<>();
    public static int MAX_FRAMES_IN_FLIGHT = 2;
    private final List<Component> components = new ArrayList<>();
    private final List<Pair<Integer, Integer>> views = new ArrayList<>();
    private final HashMap<Pair<Integer, Integer>, Entity> viewsToEntityMap = new HashMap<>();
    private final HashMap<Entity, Pair<Integer, Integer>> entityToViewsMap = new HashMap<>();
    private int currentComponentOffset = 0;

    public Scene(){

    }

    public void addEntity(Entity entity, Component... components){
        if((entity.getID() + 1) > views.size()) views.add(entity.getID(), new Pair<>(currentComponentOffset, 0));


        Pair<Integer, Integer> view = views.get(entity.getID());
        view.b = view.a + components.length;

        this.components.addAll(List.of(components));

        currentComponentOffset += components.length;
        entities.add(entity);
        viewsToEntityMap.put(view, entity);
        entityToViewsMap.put(entity, view);
    }

    public void addChildEntity(Entity parent, Entity child) {
        child.setParent(parent);
    }



    public <T> void view(Class<T> componentClass, ViewFunction<T> viewFunction){
        for(Pair<Integer, Integer> view : views){
            for (int i = view.a; i < view.b; i++) {

                Component component = components.get(i);
                if(component.getClass() == componentClass || componentClass.isAssignableFrom(component.getClass())) {
                    viewFunction.onViewEntry(viewsToEntityMap.get(view), (T) component);
                }
            }
        }
    }

    public <T> T get(Entity entity, Class<T> componentClass){
        Pair<Integer, Integer> view = entityToViewsMap.get(entity);
        for (int i = view.a; i < view.b; i++) {

            Component component = components.get(i);
            if(component.getClass() == componentClass || componentClass.isAssignableFrom(component.getClass())) {
                return (T) component;
            }
        }

        return null;
    }



    public List<Component> getComponents() {
        return components;
    }
    public List<Pair<Integer, Integer>> getViews() {
        return views;
    }

}
