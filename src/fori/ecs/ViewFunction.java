package fori.ecs;

public interface ViewFunction<T> {
    void onViewEntry(Entity entity, T component);
}
