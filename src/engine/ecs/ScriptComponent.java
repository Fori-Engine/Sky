package engine.ecs;

@ComponentArray(mask = 1 << 5)
public record ScriptComponent(Script script) {
}
