package fori.graphics.ecs;

import fori.Surface;
import fori.graphics.Renderer;

public class RenderSystem implements Runnable {
    private Renderer renderer;
    private Surface surface;

    public RenderSystem(Renderer renderer, Surface surface) {
        this.renderer = renderer;
        this.surface = surface;
    }

    @Override
    public void run() {
        System.out.println("RenderSystem is running!");
    }
}
