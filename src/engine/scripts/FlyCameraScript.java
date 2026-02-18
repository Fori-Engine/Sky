package engine.scripts;

import engine.Input;
import engine.Surface;
import engine.Time;
import engine.ecs.CameraComponent;
import engine.ecs.Entity;
import engine.ecs.Script;
import engine.graphics.Camera;
import engine.graphics.Renderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class FlyCameraScript extends Script {
    private Surface surface;
    private Camera camera;
    private float yaw = 0, pitch = 0;
    private Vector3f pos = new Vector3f(), dir = new Vector3f(), up = new Vector3f(0.0f, 1.0f, 0.0f);
    private float lastMouseX = -1, lastMouseY = -1;
    private Matrix4f viewMatrix = new Matrix4f();
    public float mouseSensitivity = 0.01f, maxPitchDeg = 75, moveSpeed = 10;

    public FlyCameraScript(Surface surface, Renderer renderer) {
        this.surface = surface;
        surface.setCaptureMouse(true);
        camera = new Camera(
                new Matrix4f().identity(),
                new Matrix4f().perspective(
                        (float) Math.toRadians(45.0f),
                        (float) renderer.getWidth() / renderer.getHeight(),
                        0.01f,
                        100.0f,
                        true
                ),
                true
        );
    }

    @Override
    public void init(Entity entity) {
        entity.getComponent(CameraComponent.class).camera = camera;
    }

    @Override
    public void update(Entity entity) {

        if(lastMouseX == -1 && lastMouseY == -1) {
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
            if(surface.getKeyPressed(Input.KEY_W)) {
                pos.add(new Vector3f(dir).normalize().mul(moveSpeed * Time.deltaTime()));
            }
            if(surface.getKeyPressed(Input.KEY_S)) {
                pos.add(new Vector3f(dir).normalize().mul(-moveSpeed * Time.deltaTime()));
            }
            if(surface.getKeyPressed(Input.KEY_D)) {
                pos.add(new Vector3f(dir).normalize().cross(up).mul(moveSpeed * Time.deltaTime()));
            }
            if(surface.getKeyPressed(Input.KEY_A)) {
                pos.add(new Vector3f(dir).normalize().cross(up).mul(-moveSpeed * Time.deltaTime()));
            }
        }


        camera.setView(
                viewMatrix.identity().lookAt(
                        pos,
                        dir.add(pos),
                        up
                )
        );
    }
}
