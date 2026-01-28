package engine.ecs;

import engine.graphics.ShaderProgram;

@ComponentArray(mask = 1 << 6)
public record ShaderComponent(ShaderProgram shaderProgram) {
}
