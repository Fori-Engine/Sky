package engine.ecs;

import engine.graphics.*;

public class RenderSystem extends EcsSystem {
    private Renderer renderer;
    private RenderPipeline renderPipeline;

    public RenderSystem(Renderer renderer, RenderPipeline renderPipeline) {
        this.renderer = renderer;
        this.renderPipeline = renderPipeline;
        renderPipeline.init(renderer);
    }

    @Override
    public void run() {
        renderPipeline.render(renderer);
    }

    @Override
    public void dispose() {

    }
}
