package engine.ecs;

import org.joml.Matrix4f;

@ComponentArray(mask = 1 << 7)
public record TransformComponent(int transformIndex, Matrix4f transform) {
    public TransformComponent(Matrix4f transform) {
        this(0, transform);
    }
}
