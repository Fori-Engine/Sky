package engine.physics;

import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.dynamics.DynamicsWorld;

public class Physics {
    public static DynamicsWorld world;

    public static void activateEverything() {
        for(CollisionObject collisionObject : world.getCollisionObjectArray()) {
            collisionObject.activate();
        }

    }
}
