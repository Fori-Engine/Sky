package lake.physics;

import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;
import org.joml.Vector2f;
import lake.graphics.Rect2D;

public class RectBody2D extends RigidBody2D {

    private Rect2D rect2D;



    public RectBody2D(float screen2Physics, World world, Rect2D rect2D, Type bodyType, boolean canRotate) {
        this.screen2Physics = screen2Physics;
        this.rect2D = rect2D;
        this.bodyType = bodyType;

        BodyDef bodyDef = new BodyDef();
        bodyDef.setFixedRotation(!canRotate);
        bodyDef.type = toBox2DType(bodyType);
        bodyDef.position.set(rect2D.x * screen2Physics, rect2D.y * screen2Physics);
        body = world.createBody(bodyDef);
    }

    @Override
    public void setPhysicalProps(float density, float friction, float res) {
        super.setPhysicalProps(density, friction, res);
        body.createFixture(Fixtures.Box((rect2D.w * screen2Physics) / 2f, (rect2D.h * screen2Physics) / 2f, density, friction, res));
    }


    public float getWidth(){
        return rect2D.w;
    }

    public float getHeight(){
        return rect2D.h;
    }


    public Vector2f getPosition(){
        return PhysicsUtil.toVector2f(body.getPosition().mul(1 / screen2Physics));
    }
}
