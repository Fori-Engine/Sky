package fori.physx;

import org.lwjgl.system.MemoryStack;
import physx.geometry.PxGeometry;

public abstract class Collider {
    public abstract PxGeometry getNativePxGeometry(MemoryStack stack);
}
