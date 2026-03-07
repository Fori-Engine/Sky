package engine.ecs;

public interface ActorPostVisitor extends ActorVisitor{
    void visit(Actor actor);
}
