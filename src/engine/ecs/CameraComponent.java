package engine.ecs;

import engine.graphics.Camera;

@ComponentArray(mask = 1 << 1)
public record CameraComponent(Camera camera) {
}
