package engine.physx;

import org.lwjgl.system.MemoryStack;
import physx.geometry.PxGeometry;
import physx.geometry.PxSphereGeometry;

public class SphereCollider extends Collider {
    public float radius;

    public SphereCollider(float radius) {
        this.radius = radius;
    }

    @Override
    public PxGeometry getNativePxGeometry(MemoryStack stack) {
        return PxSphereGeometry.createAt(stack, MemoryStack::nmalloc, radius);
    }
}
