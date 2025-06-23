package fori.graphics.ecs;

import org.joml.Matrix4f;

public record TransformComponent(int transformIndex, Matrix4f transform) {
}
