package fori.ecs;

import fori.graphics.*;

public class RenderSystem extends EcsSystem {
    private Renderer renderer;
    private Scene scene;
    private RenderPipeline renderPipeline;

    public RenderSystem(Renderer renderer, Scene scene, RenderPipeline renderPipeline) {
        this.renderer = renderer;
        this.scene = scene;
        this.renderPipeline = renderPipeline;
        renderPipeline.init(renderer, scene);
    }

    @Override
    public void run() {
        renderPipeline.render(renderer, scene);
    }

    @Override
    public void dispose() {

    }
}
