package game.scripts;

import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.linearmath.Transform;
import engine.Input;
import engine.Surface;
import engine.Time;
import engine.ecs.*;
import engine.gameui.Text;
import engine.graphics.Camera;
import engine.graphics.Renderer;
import engine.physics.Physics;
import engine.physics.MathUtil;
import game.Settings;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.vecmath.Quat4f;

import static com.bulletphysics.collision.dispatch.CollisionFlags.NO_CONTACT_RESPONSE;


public class FPPlayerController extends Script {
    private Surface surface;
    private Camera camera;
    private float yaw = 0, pitch = 0;
    private Vector3f pos = new Vector3f(), dir = new Vector3f(), up = new Vector3f(0.0f, 1.0f, 0.0f);
    private float lastMouseX = -1, lastMouseY = -1;
    private Matrix4f viewMatrix = new Matrix4f();
    public float mouseSensitivity = 0.01f, maxPitchDeg = 75, moveForce = 5;
    public float spectatorModeSpeed = 10;

    public boolean jumpJustPressed = false;
    private Actor selectedActor;
    public Actor uiActor;
    private float distScale = 1.5f;

    private Vector3f tmpVec3 = new Vector3f();

    public FPPlayerController(Surface surface, Renderer renderer, Actor uiActor) {
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
        this.uiActor = uiActor;
    }

    @Override
    public void init(Actor actor) {
        actor.getComponent(CameraComponent.class).camera = camera;
    }

    @Override
    public void fixedUpdate(Actor actor, float timestep) {
        if (!Settings.isSpectator) {
            RigidBodyComponent rigidBodyComponent = actor.getComponent(RigidBodyComponent.class);
            TransformComponent transformComponent = actor.getComponent(TransformComponent.class);



            //PxRigidDynamic pxRigidDynamic = (PxRigidDynamic) nvPhysXComponent.actor;
            Vector3f force = new Vector3f(0, 0, 0);
            if (surface.getKeyPressed(Input.KEY_W)) {
                force.zero().set(dir).normalize().mul(moveForce);
            }
            if (surface.getKeyPressed(Input.KEY_S)) {
                force.zero().set(dir).normalize().mul(-moveForce);
            }
            if (surface.getKeyPressed(Input.KEY_D)) {
                force.zero().set(dir).normalize().cross(up).mul(moveForce);
            }
            if (surface.getKeyPressed(Input.KEY_A)) {
                force.zero().set(dir).normalize().cross(up).mul(-moveForce);
            }


            boolean jump = surface.getKeyPressed(Input.KEY_SPACE);
            if (jump != jumpJustPressed) {
                if (jump) force = (tmpVec3.set(up).mul(200));

                jumpJustPressed = jump;
            }


            pos.x = transformComponent.transform().m30();
            pos.y = transformComponent.transform().m31();
            pos.z = transformComponent.transform().m32();


            rigidBodyComponent.rigidBody.activate();
            rigidBodyComponent.rigidBody.applyCentralForce(MathUtil.vec3(force));

        }


        //Grab Tool
        {
            UIComponent uiComponent = uiActor.getComponent(UIComponent.class);

            Text wSelectedActorText = uiComponent.widget.getWidgetByPath("W_Container", "W_SelectedActorText");
            wSelectedActorText.getText().string = selectedActor == null ? "Nothing selected" : selectedActor.getName();



            javax.vecmath.Vector3f fromVM = MathUtil.vec3(pos);
            javax.vecmath.Vector3f toVM = MathUtil.vec3(dir);
            toVM.scale(100);
            toVM.add(fromVM);

            CollisionWorld.ClosestRayResultCallback closestRayResultCallback = new CollisionWorld.ClosestRayResultCallback(fromVM, toVM);

            Physics.world.rayTest(fromVM, toVM, closestRayResultCallback);

            if(closestRayResultCallback.hasHit() && surface.getMousePressed(Input.MOUSE_BUTTON_1) && selectedActor == null) {
                Actor newSelectedActor = (Actor) closestRayResultCallback.collisionObject.getUserPointer();
                if(!newSelectedActor.getName().equals("Floor")) {
                    selectedActor = newSelectedActor;
                    RigidBodyComponent selectedActorRigidBodyComponent = selectedActor.getComponent(RigidBodyComponent.class);
                    selectedActorRigidBodyComponent.rigidBody.setCollisionFlags(NO_CONTACT_RESPONSE);
                    selectedActorRigidBodyComponent.rigidBody.setLinearVelocity(new javax.vecmath.Vector3f(0, 0, 0));
                    selectedActorRigidBodyComponent.rigidBody.setAngularVelocity(new javax.vecmath.Vector3f(0, 0, 0));
                    selectedActorRigidBodyComponent.rigidBody.setGravity(new javax.vecmath.Vector3f(0, 0, 0));
                    selectedActorRigidBodyComponent.rigidBody.activate(true);


                }

            }
            else if(surface.getMouseReleased(Input.MOUSE_BUTTON_1) && selectedActor != null) {
                RigidBodyComponent selectedActorRigidBodyComponent = selectedActor.getComponent(RigidBodyComponent.class);
                selectedActorRigidBodyComponent.rigidBody.setCollisionFlags(
                        selectedActorRigidBodyComponent.rigidBody.getCollisionFlags() & ~NO_CONTACT_RESPONSE
                );
                selectedActorRigidBodyComponent.rigidBody.setGravity(Physics.world.getGravity(new javax.vecmath.Vector3f()));

                selectedActorRigidBodyComponent.rigidBody.activate(true);

                selectedActor = null;
            }

            if(selectedActor != null) {


                RigidBodyComponent selectedActorRigidBodyComponent = selectedActor.getComponent(RigidBodyComponent.class);
                selectedActorRigidBodyComponent.rigidBody.activate();
                selectedActorRigidBodyComponent.rigidBody.setAngularFactor(0.1f);
                selectedActorRigidBodyComponent.rigidBody.setWorldTransform(
                        new Transform(new javax.vecmath.Matrix4f(
                                selectedActorRigidBodyComponent.rigidBody.getWorldTransform(new Transform()).getRotation(new Quat4f()),
                                MathUtil.vec3(new Vector3f(pos).add(new Vector3f(dir).mul(distScale))),
                                1.0f
                        ))
                );


            }




        }

    }

    @Override
    public void update(Actor actor, Actor root) {


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
                if(Settings.isSpectator) {

                    Vector3f velocity = new Vector3f(0, 0, 0);
                    if (surface.getKeyPressed(Input.KEY_W)) {
                        velocity.zero().set(dir).normalize().mul(spectatorModeSpeed);
                    }
                    if (surface.getKeyPressed(Input.KEY_S)) {
                        velocity.zero().set(dir).normalize().mul(-spectatorModeSpeed);
                    }
                    if (surface.getKeyPressed(Input.KEY_D)) {
                        velocity.zero().set(dir).normalize().cross(up).mul(spectatorModeSpeed);
                    }
                    if (surface.getKeyPressed(Input.KEY_A)) {
                        velocity.zero().set(dir).normalize().cross(up).mul(-spectatorModeSpeed);
                    }
                    pos.add(velocity.mul(Time.deltaTime()));
                }
            }

            camera.setView(
                    viewMatrix.identity().lookAt(
                            pos,
                            tmpVec3.set(dir).add(pos),
                            up
                    )
            );

            UIComponent uiComponent = uiActor.getComponent(UIComponent.class);

            Text wFPSText = uiComponent.widget.getWidgetByPath("W_Container", "W_FPSText");
            wFPSText.getText().string = "FPS: " + Time.framesPerSecond();
        }






    }
}
