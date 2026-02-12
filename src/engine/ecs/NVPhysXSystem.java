package engine.ecs;

import engine.Input;
import engine.Logger;
import engine.Surface;
import engine.Time;
import engine.physx.ActorType;
import engine.physx.Collider;
import engine.physx.Material;
import org.joml.Quaternionf;
import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.physics.*;

import java.util.List;

public class NVPhysXSystem extends EcsSystem {
    private PxScene pxScene;
    private PxPhysics physics;
    private PxFoundation foundation;
    private PxDefaultErrorCallback errorCb;
    private PxDefaultAllocator allocator;
    private PxSceneDesc sceneDesc;
    private PxTolerancesScale tolerances;
    private PxVec3 gravity;
    private PxDefaultCpuDispatcher cpuDispatcher;
    private PxSimulationFilterShader filterShader;

    private Surface surface;
    private Scene scene;
    private float accumulator = 0;
    private int numThreads;
    private float timestep;

    public NVPhysXSystem(Surface surface, Scene scene, int numThreads, float timestep) {
        this.surface = surface;
        this.scene = scene;
        this.numThreads = numThreads;
        this.timestep = timestep;

        int version = PxTopLevelFunctions.getPHYSICS_VERSION();
        int versionMajor = version >> 24;
        int versionMinor = (version >> 16) & 0xff;
        int versionMicro = (version >> 8) & 0xff;

        Logger.info(NVPhysXSystem.class, "Loading NVIDIA PhysX version: " + versionMajor + "." + versionMinor + "." + versionMicro);

        allocator = new PxDefaultAllocator();
        errorCb = new PxDefaultErrorCallback();
        foundation = PxTopLevelFunctions.CreateFoundation(version, allocator, errorCb);

        tolerances = new PxTolerancesScale();
        tolerances.setLength(100);
        tolerances.setSpeed(981);

        physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerances);


        cpuDispatcher = PxTopLevelFunctions.DefaultCpuDispatcherCreate(numThreads);
        filterShader = PxTopLevelFunctions.DefaultFilterShader();

        gravity = new PxVec3(0f, -9.81f, 0f);
        sceneDesc = new PxSceneDesc(tolerances);
        sceneDesc.setGravity(gravity);
        sceneDesc.setCpuDispatcher(cpuDispatcher);
        sceneDesc.setFilterShader(filterShader);
        pxScene = physics.createScene(sceneDesc);





    }

    @Override
    public void run(List<Entity> entities) {

        for (Entity entity : entities) {
            if(entity.has(NVPhysXComponent.class) && entity.has(TransformComponent.class)) {
                NVPhysXComponent nvPhysXComponent = entity.getComponent(NVPhysXComponent.class);
                TransformComponent transformComponent = entity.getComponent(TransformComponent.class);

                if(!nvPhysXComponent.initialized) {
                    try(MemoryStack stack = MemoryStack.stackPush()) {
                        Material material = nvPhysXComponent.material;
                        Collider collider = nvPhysXComponent.collider;

                        PxTransform transform = PxTransform.createAt(stack, MemoryStack::nmalloc, PxIDENTITYEnum.PxIdentity);

                        {
                            PxVec3 pos = PxVec3.createAt(stack, MemoryStack::nmalloc);

                            pos.setX(transformComponent.transform().m30());
                            pos.setY(transformComponent.transform().m31());
                            pos.setZ(transformComponent.transform().m32());

                            transform.setP(pos);
                        }


                        {
                            PxQuat quat = PxQuat.createAt(stack, MemoryStack::nmalloc);
                            Quaternionf rotation = new Quaternionf();
                            transformComponent.transform().getNormalizedRotation(rotation);

                            quat.setX(rotation.x);
                            quat.setY(rotation.y);
                            quat.setZ(rotation.z);
                            quat.setW(rotation.w);

                            transform.setQ(quat);
                        }




                        PxFilterData filterData = PxFilterData.createAt(stack, MemoryStack::nmalloc, 1, 1, 0, 0);

                        PxShapeFlags shapeFlags = PxShapeFlags.createAt(stack, MemoryStack::nmalloc, (byte) (PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));
                        nvPhysXComponent.pxMaterial = physics.createMaterial(material.staticFriction, material.dynamicFriction, material.restitution);
                        nvPhysXComponent.shape = physics.createShape(collider.getNativePxGeometry(stack), nvPhysXComponent.pxMaterial, true, shapeFlags);

                        if(nvPhysXComponent.actorType == ActorType.Static) {
                            nvPhysXComponent.actor = physics.createRigidStatic(transform);
                        }
                        else if(nvPhysXComponent.actorType == ActorType.Dynamic) {
                            nvPhysXComponent.actor = physics.createRigidDynamic(transform);
                        }

                        nvPhysXComponent.shape.setSimulationFilterData(filterData);
                        nvPhysXComponent.actor.attachShape(nvPhysXComponent.shape);
                        pxScene.addActor(nvPhysXComponent.actor);

                        nvPhysXComponent.releaseCallback = () -> {
                            pxScene.removeActor(nvPhysXComponent.actor);
                            nvPhysXComponent.actor.release();
                            nvPhysXComponent.shape.release();
                            nvPhysXComponent.pxMaterial.release();
                        };


                    }
                    nvPhysXComponent.initialized = true;
                }

                PxQuat quat = nvPhysXComponent.actor.getGlobalPose().getQ();
                PxVec3 pos = nvPhysXComponent.actor.getGlobalPose().getP();

                Quaternionf rotation = new Quaternionf(quat.getX(), quat.getY(), quat.getZ(), quat.getW());


                transformComponent
                        .transform()
                        .identity()
                        .translate(pos.getX(), pos.getY(), pos.getZ())
                        .rotate(rotation);
            }
        }





        if(surface.getKeyPressed(Input.KEY_SPACE)) {


            float frameTime = Math.min(Time.deltaTime(), 0.25f);
            accumulator += frameTime;
            while (accumulator >= timestep) {

                pxScene.simulate(timestep);
                pxScene.fetchResults(true);

                accumulator -= timestep;
            }
        }





    }

    @Override
    public void dispose() {
        gravity.destroy();
        tolerances.destroy();
        sceneDesc.destroy();
        pxScene.release();
        physics.release();
        cpuDispatcher.destroy();
        foundation.release();
        errorCb.destroy();
        allocator.destroy();


    }
}
