package fori.graphics.ecs;

import dev.dominion.ecs.api.Results;
import fori.Surface;
import fori.graphics.Renderer;
import fori.graphics.SizeUtil;
import org.lwjgl.system.linux.Stat;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class RenderSystem implements Runnable {
    private Renderer renderer;
    private Surface surface;
    private Scene scene;

    public RenderSystem(Renderer renderer, Scene scene, Surface surface) {
        this.renderer = renderer;
        this.scene = scene;
        this.surface = surface;
    }

    @Override
    public void run() {
        System.out.println("RenderSystem is running!");

        scene.getEngine().findEntitiesWith(TransformComponent.class, StaticMeshComponent.class).stream().forEach(components -> {
            TransformComponent transformComponent = components.comp1();
            StaticMeshComponent staticMeshComponent = components.comp2();

            ByteBuffer transformsData = staticMeshComponent.staticMeshBatch().getTransformsBuffers()[renderer.getFrameIndex()].get();
            transformComponent.transform().get(transformComponent.transformIndex() * SizeUtil.MATRIX_SIZE_BYTES, transformsData);
        });


    }
}
