package fori.ecs;

import dev.dominion.ecs.api.Results;
import fori.Logger;
import fori.Time;
import fori.physx.ActorType;
import fori.physx.Collider;
import fori.physx.Material;
import org.joml.Quaternionf;
import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.physics.*;

public class NVPhysXSystem implements Runnable {
    private PxScene pxScene;
    private Scene scene;
    private PxPhysics physics;
    private float accumulator = 0;
    private float timestep;


    public NVPhysXSystem(Scene scene, float timestep) {
        this.scene = scene;
        this.timestep = timestep;
        int version = PxTopLevelFunctions.getPHYSICS_VERSION();

        int versionMajor = version >> 24;
        int versionMinor = (version >> 16) & 0xff;
        int versionMicro = (version >> 8) & 0xff;

        Logger.info(NVPhysXSystem.class, "Loading NVIDIA PhysX version: " + versionMajor + "." + versionMinor + "." + versionMicro);

        PxDefaultAllocator allocator = new PxDefaultAllocator();
        PxDefaultErrorCallback errorCb = new PxDefaultErrorCallback();
        PxFoundation foundation = PxTopLevelFunctions.CreateFoundation(version, allocator, errorCb);

        PxTolerancesScale tolerances = new PxTolerancesScale();
        tolerances.setLength(100);
        tolerances.setSpeed(981);

        physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerances);


        int numThreads = 4;
        PxDefaultCpuDispatcher cpuDispatcher = PxTopLevelFunctions.DefaultCpuDispatcherCreate(numThreads);

        PxVec3 gravity = new PxVec3(0f, -9.81f, 0f);
        PxSceneDesc sceneDesc = new PxSceneDesc(tolerances);
        sceneDesc.setGravity(gravity);
        sceneDesc.setCpuDispatcher(cpuDispatcher);
        sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
        pxScene = physics.createScene(sceneDesc);



    }

    @Override
    public void run() {
        scene.getEngine().findEntitiesWith(NVPhysXComponent.class, TransformComponent.class).stream().forEach(components -> {
            NVPhysXComponent nvPhysXComponent = components.comp1();
            TransformComponent transformComponent = components.comp2();

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
                        transformComponent.transform().getUnnormalizedRotation(rotation);

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
        });





        float frameTime = Math.min(Time.deltaTime(), 0.25f);
        accumulator += frameTime;
        while (accumulator >= timestep) {

            pxScene.simulate(timestep);
            pxScene.fetchResults(true);

            accumulator -= timestep;
        }






    }
}
