package fori.ecs;

public class Entity {
    public String tag;
    public int id;
    public static int currentID;

    public Entity(String tag) {
        this.tag = tag;
        this.id = currentID++;
    }



    /*
    public <T> T get(Class<T> c) {

        for(Component component : components.values()){
            if(component.getClass() == c || c.isAssignableFrom(component.getClass())) {
                return c.cast(component);
            }
        }

        return null;
    }

    public boolean has(Class... c){
        for(Class d : c){
            if(!components.containsKey(d)) return false;
        }

        return true;
    }

    public void put(Component component){
        components.put(component.getClass(), component);
    }

    public void removeSpecific(Class c){
        //TODO: Remove only completely matching types
        Logger.todo(Entity.class, "TODO: Entity (" + id + ") removeSpecific() [Entity.java]");
    }

    public void removeIfExists(Class c){
        if(components.containsKey(c)) remove(c);
    }
    public void remove(Class c){
        //Removes all types (even subclasses of that type)
        components.remove(c);
    }

    public HashMap<Class, Component> getComponents() {
        return components;
    }

     */
}
