package engine.graphics;

import engine.ecs.Scene;

public abstract class RenderPipeline {
    protected RenderGraph renderGraph;
    public abstract void init(Renderer renderer, Scene scene);
    public abstract void render(Renderer renderer, Scene scene);
}
