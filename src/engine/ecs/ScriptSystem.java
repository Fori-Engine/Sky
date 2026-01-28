package engine.ecs;

import java.util.List;

public class ScriptSystem extends EcsSystem {
    private Scene scene;

    public ScriptSystem(Scene scene) {
        this.scene = scene;
    }

    @Override
    public void run(List<Entity> entities) {
        for(Entity entity : entities) {
            if(entity.has(ScriptComponent.class)) {
                ScriptComponent scriptComponent = entity.getComponent(ScriptComponent.class);
                if (!scriptComponent.script().initialized) {
                    scriptComponent.script().init(entity);
                }

                scriptComponent.script().update(entity);

            }
        }
    }

    @Override
    public void dispose() {

    }
}
