package fori.graphics.ecs;

import dev.dominion.ecs.api.Results;
import fori.Surface;
import fori.graphics.Camera;
import fori.graphics.Renderer;
import fori.graphics.SizeUtil;
import org.lwjgl.system.linux.Stat;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class RenderSystem implements Runnable {
    private Renderer renderer;
    private Surface surface;
    private Scene scene;
    private Camera sceneCamera;

    public RenderSystem(Renderer renderer, Scene scene, Surface surface) {
        this.renderer = renderer;
        this.scene = scene;
        this.surface = surface;
    }

    @Override
    public void run() {


        scene.getEngine().findEntitiesWith(CameraComponent.class).stream().forEach(components1 -> {
            CameraComponent cameraComponent = components1.comp();
            sceneCamera = cameraComponent.camera();
        });


        scene.getEngine().findEntitiesWith(TransformComponent.class, StaticMeshComponent.class).stream().forEach(components -> {


            TransformComponent transformComponent = components.comp1();
            StaticMeshComponent staticMeshComponent = components.comp2();

            ByteBuffer transformsData = staticMeshComponent.staticMeshBatch().getTransformsBuffers()[renderer.getFrameIndex()].get();
            transformComponent.transform().get(transformComponent.transformIndex() * SizeUtil.MATRIX_SIZE_BYTES, transformsData);
            ByteBuffer cameraData = staticMeshComponent.staticMeshBatch().getCameraBuffers()[renderer.getFrameIndex()].get();

            sceneCamera.getView().get(0, cameraData);
            sceneCamera.getProj().get(SizeUtil.MATRIX_SIZE_BYTES, cameraData);


        });

        scene.getEngine().findEntitiesWith(TransformComponent.class, DynamicMeshComponent.class).stream().forEach(components -> {


            TransformComponent transformComponent = components.comp1();
            DynamicMeshComponent dynamicMeshComponent = components.comp2();

            ByteBuffer transformsData = dynamicMeshComponent.dynamicMesh().getTransformsBuffers()[renderer.getFrameIndex()].get();
            transformComponent.transform().get(0, transformsData);
            ByteBuffer cameraData = dynamicMeshComponent.dynamicMesh().getCameraBuffers()[renderer.getFrameIndex()].get();

            sceneCamera.getView().get(0, cameraData);
            sceneCamera.getProj().get(SizeUtil.MATRIX_SIZE_BYTES, cameraData);

        });




    }
}
