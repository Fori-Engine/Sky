package fori.ecs;

import fori.Surface;
import fori.graphics.*;

import java.nio.ByteBuffer;

public class RenderSystem extends EcsSystem {
    private Renderer renderer;
    private Surface surface;
    private Scene scene;
    private Camera sceneCamera;
    private RenderTarget swapchainRenderTarget;
    private GraphicsCommandList graphicsCommands;

    public RenderSystem(Renderer renderer, Scene scene, Surface surface) {
        this.renderer = renderer;
        this.scene = scene;
        this.surface = surface;
        swapchainRenderTarget = renderer.getSwapchainRenderTarget();
        graphicsCommands = CommandList.newGraphicsCommandList(renderer, renderer.getMaxFramesInFlight());
    }

    @Override
    public void run() {
        scene.getEngine().findEntitiesWith(CameraComponent.class).stream().forEach(components1 -> {
            CameraComponent cameraComponent = components1.comp();
            sceneCamera = cameraComponent.camera();
        });


        graphicsCommands.startRecording(
                renderer.getFrameStartSync(),
                swapchainRenderTarget,
                renderer.getFrameIndex()
        );

        /*
        scene.getEngine().findEntitiesWith(TransformComponent.class, StaticMeshComponent.class).stream().forEach(components -> {


            TransformComponent transformComponent = components.comp1();
            StaticMeshComponent staticMeshComponent = components.comp2();

            ByteBuffer transformsData = staticMeshComponent.staticMeshBatch().getTransformsBuffers()[renderer.getFrameIndex()].get();
            transformComponent.transform().get(transformComponent.transformIndex() * SizeUtil.MATRIX_SIZE_BYTES, transformsData);
            ByteBuffer cameraData = staticMeshComponent.staticMeshBatch().getCameraBuffers()[renderer.getFrameIndex()].get();

            sceneCamera.getView().get(0, cameraData);
            sceneCamera.getProj().get(SizeUtil.MATRIX_SIZE_BYTES, cameraData);

        });
        */

        scene.getEngine().findEntitiesWith(TransformComponent.class, DynamicMeshComponent.class).stream().forEach(components -> {


            TransformComponent transformComponent = components.comp1();
            DynamicMeshComponent dynamicMeshComponent = components.comp2();

            ByteBuffer transformsData = dynamicMeshComponent.dynamicMesh().getTransformsBuffers()[renderer.getFrameIndex()].get();
            transformComponent.transform().get(0, transformsData);
            ByteBuffer cameraData = dynamicMeshComponent.dynamicMesh().getCameraBuffers()[renderer.getFrameIndex()].get();

            sceneCamera.getView().get(0, cameraData);
            sceneCamera.getProj().get(SizeUtil.MATRIX_SIZE_BYTES, cameraData);

            graphicsCommands.setDrawBuffers(
                    dynamicMeshComponent.dynamicMesh().getVertexBuffer(),
                    dynamicMeshComponent.dynamicMesh().getIndexBuffer()
            );
            graphicsCommands.setShaderProgram(
                    dynamicMeshComponent.dynamicMesh().getShaderProgram()
            );
            graphicsCommands.drawIndexed(dynamicMeshComponent.dynamicMesh());
        });

        graphicsCommands.endRecording();
        renderer.addCommandList(graphicsCommands);
    }

    @Override
    public void dispose() {

    }
}
