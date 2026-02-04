package engine.ecs;

import engine.graphics.Camera;

@ComponentArray(mask = 1 << 1)
public class CameraComponent {
    public Camera camera;

    public CameraComponent(Camera camera) {
        this.camera = camera;
    }
}
