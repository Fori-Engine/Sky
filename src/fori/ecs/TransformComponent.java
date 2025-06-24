package fori.ecs;

import org.joml.Matrix4f;

public record TransformComponent(int transformIndex, Matrix4f transform) {
    public TransformComponent(Matrix4f transform) {
        this(0, transform);
    }
}
