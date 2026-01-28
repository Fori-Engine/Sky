package engine.ecs;

import engine.Logger;

public class Entity {
    private Object[] components = new Object[64];
    private long mask = 0L;

    public static void tryClassload(Class<?> clazz) {
        Logger.info(Entity.class, "Trying to load class: " + clazz.getSimpleName());
    }

    public void add(Object component) {
        int index = getIndex(component.getClass());
        components[index] = component;
        mask |= getMask(component.getClass());
    }

    public boolean has(Class<?> clazz) {
        return (mask & getMask(clazz)) != 0;
    }

    public void remove(Class<?> clazz) {
        int index = getIndex(clazz);
        components[index] = null;
        mask = mask & ~getMask(clazz);
    }

    public <T> T getComponent(Class<T> c) {
        return ((T) components[getIndex(c)]);
    }

    public static long getMask(Class<?> clazz) {
        return clazz.getAnnotation(ComponentArray.class).mask();
    }

    public long getMask() {
        return mask;
    }

    public static int getIndex(Class<?> clazz) {
        return 64 - Long.numberOfTrailingZeros(getMask(clazz)) - 1;
    }

}
