package fori.physx;

import org.lwjgl.system.MemoryStack;
import physx.geometry.PxBoxGeometry;
import physx.geometry.PxGeometry;

public class BoxCollider extends Collider {
    public float width, height, depth;

    public BoxCollider(float width, float height, float depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    @Override
    public PxGeometry getNativePxGeometry(MemoryStack stack) {
        return PxBoxGeometry.createAt(stack, MemoryStack::nmalloc, width / 2f, height / 2f, depth / 2f);
    }
}
