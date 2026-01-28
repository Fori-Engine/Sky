package engine.ecs;

import java.util.List;

public abstract class EcsSystem {
    public abstract void run(List<Entity> entities);
    public abstract void dispose();
}
