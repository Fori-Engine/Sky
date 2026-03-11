package game.scripts;

import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.dynamics.constraintsolver.Point2PointConstraint;
import engine.Input;
import engine.Surface;
import engine.Time;
import engine.ecs.*;
import engine.graphics.Camera;
import engine.graphics.Renderer;
import engine.physics.Physics;
import engine.physics.TypeUtil;
import game.Settings;
import org.joml.Matrix4f;
import org.joml.Vector3f;


public class FPCameraController extends Script {
    private Surface surface;
    private Camera camera;
    private float yaw = 0, pitch = 0;
    private float yaw2 = 0, pitch2 = 0;
    private Vector3f pos = new Vector3f(), dir = new Vector3f(), up = new Vector3f(0.0f, 1.0f, 0.0f);
    private float lastMouseX = -1, lastMouseY = -1;
    private Matrix4f viewMatrix = new Matrix4f();
    public float mouseSensitivity = 0.01f, maxPitchDeg = 75, acceleration = 10;
    public float spectatorModeSpeed = 10;

    public boolean jumpJustPressed = false;
    private Actor selectedActor;
    private Point2PointConstraint point2PointConstraint;

    public FPCameraController(Surface surface, Renderer renderer) {
        this.surface = surface;
        camera = new Camera(
                new Matrix4f().identity(),
                new Matrix4f().perspective(
                        (float) Math.toRadians(75f),
                        (float) renderer.getWidth() / renderer.getHeight(),
                        0.01f,
                        100.0f,
                        true
                ),
                true
        );
        surface.addKeyCallback(key -> {
            if(key == Input.KEY_Q) Settings.isSpectator = !Settings.isSpectator;
        });
    }

    @Override
    public void init(Actor actor) {
        actor.getComponent(CameraComponent.class).camera = camera;
    }

    @Override
    public void update(Actor actor, Actor root) {
        RigidBodyComponent rigidBodyComponent = actor.getComponent(RigidBodyComponent.class);
        TransformComponent transformComponent = actor.getComponent(TransformComponent.class);


        //Movement
        {

            if (lastMouseX == -1 && lastMouseY == -1) {
                lastMouseX = surface.getMousePos().x;
                lastMouseY = surface.getMousePos().y;
            }

            yaw += (surface.getMousePos().x - lastMouseX) * mouseSensitivity;
            pitch += (surface.getMousePos().y - lastMouseY) * mouseSensitivity;

            pitch = (float) Math.clamp(pitch, Math.toRadians(-maxPitchDeg), Math.toRadians(maxPitchDeg));


            lastMouseX = surface.getMousePos().x;
            lastMouseY = surface.getMousePos().y;


            dir.x = (float) (Math.sin(yaw));
            dir.y = (float) (Math.sin(pitch));
            dir.z = (float) ((Math.cos(yaw) * Math.cos(pitch)));


            //Movement
            {

                if (!Settings.isSpectator) {


                    //PxRigidDynamic pxRigidDynamic = (PxRigidDynamic) nvPhysXComponent.actor;
                    Vector3f force = new Vector3f(0, 0, 0);
                    if (surface.getKeyPressed(Input.KEY_W)) {
                        force = (new Vector3f(dir).normalize().mul(acceleration));
                    }
                    if (surface.getKeyPressed(Input.KEY_S)) {
                        force = (new Vector3f(dir).normalize().mul(-acceleration));
                    }
                    if (surface.getKeyPressed(Input.KEY_D)) {
                        force = (new Vector3f(dir).normalize().cross(up).mul(acceleration));
                    }
                    if (surface.getKeyPressed(Input.KEY_A)) {
                        force = (new Vector3f(dir).normalize().cross(up).mul(-acceleration));
                    }


                    boolean jump = surface.getKeyPressed(Input.KEY_SPACE);
                    if (jump != jumpJustPressed) {
                        if (jump) force = (new Vector3f(up).mul(200));

                        jumpJustPressed = jump;
                    }


                    pos.x = transformComponent.transform().m30();
                    pos.y = transformComponent.transform().m31();
                    pos.z = transformComponent.transform().m32();


                    rigidBodyComponent.rigidBody.activate();
                    rigidBodyComponent.rigidBody.applyCentralForce(TypeUtil.vec3(force));

                } else {

                    Vector3f velocity = new Vector3f(0, 0, 0);
                    if (surface.getKeyPressed(Input.KEY_W)) {
                        velocity = (new Vector3f(dir).normalize().mul(spectatorModeSpeed));
                    }
                    if (surface.getKeyPressed(Input.KEY_S)) {
                        velocity = (new Vector3f(dir).normalize().mul(-spectatorModeSpeed));
                    }
                    if (surface.getKeyPressed(Input.KEY_D)) {
                        velocity = (new Vector3f(dir).normalize().cross(up).mul(spectatorModeSpeed));
                    }
                    if (surface.getKeyPressed(Input.KEY_A)) {
                        velocity = (new Vector3f(dir).normalize().cross(up).mul(-spectatorModeSpeed));
                    }
                    pos.add(velocity.mul(Time.deltaTime()));
                }
            }


            camera.setView(
                    viewMatrix.identity().lookAt(
                            pos,
                            new Vector3f(dir).add(pos),
                            up
                    )
            );
        }

        //Grab Tool
        {
            javax.vecmath.Vector3f fromVM = TypeUtil.vec3(pos);
            javax.vecmath.Vector3f toVM = TypeUtil.vec3(dir);
            toVM.scale(100);
            toVM.add(fromVM);

            CollisionWorld.ClosestRayResultCallback closestRayResultCallback = new CollisionWorld.ClosestRayResultCallback(fromVM, toVM);

            Physics.world.rayTest(fromVM, toVM, closestRayResultCallback);

            if(closestRayResultCallback.hasHit() && surface.getMousePressed(Input.MOUSE_BUTTON_1)) {
                selectedActor = (Actor) closestRayResultCallback.collisionObject.getUserPointer();
            }
            else selectedActor = null;



            if(selectedActor != null) {
                RigidBodyComponent selectedRigidBodyComponent = selectedActor.getComponent(RigidBodyComponent.class);


                javax.vecmath.Vector3f rayToTransformDist = closestRayResultCallback.hitPointWorld;
                rayToTransformDist.sub(closestRayResultCallback.hitPointWorld, selectedRigidBodyComponent.rigidBody.getCenterOfMassPosition(new javax.vecmath.Vector3f()));


                if (point2PointConstraint != null) Physics.world.removeConstraint(point2PointConstraint);
                point2PointConstraint = new Point2PointConstraint(selectedRigidBodyComponent.rigidBody, rayToTransformDist);
                point2PointConstraint.setting.tau = 0.1f;
                point2PointConstraint.setting.damping = 1.001f;
                point2PointConstraint.setting.impulseClamp = 1f;
                Physics.world.addConstraint(point2PointConstraint);


                toVM = TypeUtil.vec3(dir);
                toVM.add(fromVM);

                point2PointConstraint.setPivotB(toVM);
            }



        }




    }
}
