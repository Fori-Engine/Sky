package engine.ecs;

import engine.physx.ActorType;
import engine.physx.Collider;
import engine.physx.Material;
import physx.physics.PxMaterial;
import physx.physics.PxRigidActor;
import physx.physics.PxShape;

public class NVPhysXComponent extends NativeResComponent {
    public boolean initialized;
    public Collider collider;
    public Material material;
    public ActorType actorType;
    public PxShape shape;
    public PxMaterial pxMaterial;
    public PxRigidActor actor;


    public NVPhysXComponent(Collider collider, Material material, ActorType actorType) {
        this.collider = collider;
        this.material = material;
        this.actorType = actorType;
    }
}
