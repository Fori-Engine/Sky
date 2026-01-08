package engine.ecs;

public abstract class EcsSystem implements Runnable {
    @Override
    public abstract void run();
    public abstract void dispose();
}
