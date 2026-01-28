package engine.ecs;

import engine.graphics.*;
import engine.graphics.pipelines.SceneFeatures;

import java.util.List;

public class RenderSystem extends EcsSystem {
    private Renderer renderer;
    private RenderPipeline renderPipeline;
    private Scene scene;

    public RenderSystem(Renderer renderer, RenderPipeline renderPipeline, Scene scene) {
        this.renderer = renderer;
        this.renderPipeline = renderPipeline;
        this.scene = scene;
        renderPipeline.init(renderer);
    }

    @Override
    public void run(List<Entity> entities) {
        renderPipeline.getFeatures(SceneFeatures.class).setScene(scene);
        renderPipeline.render(renderer);
    }

    @Override
    public void dispose() {

    }
}
