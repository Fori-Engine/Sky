package fori.ecs;

public class Entity {
    private String tag;
    private int id;
    private static int currentID;
    private Entity parent;

    public Entity(String tag) {
        this.tag = tag;
        this.id = currentID++;
    }

    public String getTag() {
        return tag;
    }

    public int getID() {
        return id;
    }

    public static int getCurrentID() {
        return currentID;
    }

    public void setParent(Entity parent) {
        this.parent = parent;
    }

    public Entity getParent() {
        return parent;
    }
}
