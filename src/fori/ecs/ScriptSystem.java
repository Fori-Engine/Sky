package fori.ecs;

import dev.dominion.ecs.api.Results;

import java.util.function.Consumer;

public class ScriptSystem implements Runnable {
    private Scene scene;

    public ScriptSystem(Scene scene) {
        this.scene = scene;

        scene.getEngine().findEntitiesWith(ScriptComponent.class).stream().forEach(components -> {
            ScriptComponent scriptComponent = components.comp();
            scriptComponent.script().update(components.entity());
        });
    }

    @Override
    public void run() {
        scene.getEngine().findEntitiesWith(ScriptComponent.class).stream().forEach(components -> {
            ScriptComponent scriptComponent = components.comp();
            scriptComponent.script().update(components.entity());
        });
    }
}
