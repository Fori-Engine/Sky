package fori.graphics;

import fori.ecs.Scene;

public abstract class RenderPipeline {
    public abstract void init(Renderer renderer, Scene scene);
    public abstract void render(Renderer renderer, Scene scene);
}
