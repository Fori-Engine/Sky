package fori.physics;

import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;
import org.joml.Vector2f;

public class CircleBody2D extends RigidBody2D {
    private float radius;

    public CircleBody2D(float screen2Physics, World world, float radius, Vector2f pos, Type bodyType, boolean canRotate) {
        this.screen2Physics = screen2Physics;
        this.radius = radius;
        this.bodyType = bodyType;

        BodyDef bodyDef = new BodyDef();
        bodyDef.setFixedRotation(!canRotate);
        bodyDef.type = toBox2DType(this.bodyType);
        bodyDef.position.set(pos.x * screen2Physics, pos.y * screen2Physics);
        body = world.createBody(bodyDef);
    }

    @Override
    public void setPhysicalProps(float density, float friction, float res) {
        super.setPhysicalProps(density, friction, res);


        body.createFixture(Fixtures.Circle(radius * screen2Physics, density, friction, res));
    }


    public Vector2f getPosition(){
        return PhysicsUtil.toVector2f(body.getPosition().mul(1 / screen2Physics)).sub(radius, radius);
    }




    public float getRadius() {
        return radius;
    }
}
