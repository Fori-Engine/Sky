package engine.ecs;

public interface ActorPreVisitor extends ActorVisitor {
    void visit(Actor actor);
}
