package engine.ecs;

import java.util.List;

public abstract class ActorSystem {
    public abstract void run(Actor root);
    public abstract void dispose();
}
