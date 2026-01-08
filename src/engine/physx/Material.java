package engine.physx;

public class Material {
    public float staticFriction, dynamicFriction, restitution;

    public Material(float staticFriction, float dynamicFriction, float restitution) {
        this.staticFriction = staticFriction;
        this.dynamicFriction = dynamicFriction;
        this.restitution = restitution;
    }
}
