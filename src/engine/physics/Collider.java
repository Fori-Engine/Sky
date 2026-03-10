package engine.physics;

import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.SphereShape;

import javax.vecmath.Vector3f;

public class Collider {
    private CollisionShape collisionShape;

    private Collider(CollisionShape collisionShape) {
        this.collisionShape = collisionShape;
    }

    public CollisionShape getCollisionShape() {
        return collisionShape;
    }

    public static Collider newBoxCollider(float w, float h, float d) {
        return new Collider(new BoxShape(new Vector3f(w / 2, h / 2, d / 2)));
    }

    public static Collider newSphereCollider(float r) {
        return new Collider(new SphereShape(r));
    }
}
