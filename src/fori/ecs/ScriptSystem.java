package fori.ecs;

import dev.dominion.ecs.api.Results;

import java.util.function.Consumer;

public class ScriptSystem extends EcsSystem {
    private Scene scene;

    public ScriptSystem(Scene scene) {
        this.scene = scene;
    }

    @Override
    public void run() {
        scene.getEngine().findEntitiesWith(ScriptComponent.class).stream().forEach(components -> {
            ScriptComponent scriptComponent = components.comp();

            if(!scriptComponent.script().initialized) {
                scriptComponent.script().init(components.entity());
            }

            scriptComponent.script().update(components.entity());
        });
    }

    @Override
    public void dispose() {

    }
}
