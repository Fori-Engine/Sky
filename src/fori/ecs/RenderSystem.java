package fori.ecs;

import fori.asset.AssetPacks;
import fori.graphics.*;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

import static fori.graphics.ShaderRes.ShaderStage.*;
import static fori.graphics.ShaderRes.Type.*;
import static fori.graphics.VertexAttributes.Type.*;

public class RenderSystem extends EcsSystem {
    private Renderer renderer;
    private Scene scene;
    private Camera sceneCamera;
    private ShaderProgram computeShaderProgram;



    private Camera uiCamera;
    private ShaderProgram uiShaderProgram;
    private Buffer uiVertexBuffer, uiIndexBuffer;
    private Buffer[] uiCameraBuffers;



    private GraphicsCommandList graphicsCommands;
    private ComputeCommandList computeCommands;

    public RenderSystem(Renderer renderer, Scene scene) {
        this.renderer = renderer;
        this.scene = scene;
        graphicsCommands = CommandList.newGraphicsCommandList(renderer, renderer.getMaxFramesInFlight());
        computeCommands = CommandList.newComputeCommandList(renderer, renderer.getMaxFramesInFlight());

        //Compute Shader
        {
            ShaderReader.ShaderSources shaderSources = ShaderReader.read(
                    AssetPacks.<String>getAsset("core:assets/shaders/vulkan/Compute.glsl").asset
            );


            computeShaderProgram = ShaderProgram.newComputeShaderProgram(renderer);
            computeShaderProgram.setShaders(
                    Shader.newShader(computeShaderProgram, ShaderType.Compute, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Compute), ShaderType.Compute))
            );

            computeShaderProgram.bind(null,
                    new ShaderResSet(
                    0,
                    new ShaderRes(
                            "test",
                            0,
                            UniformBuffer,
                            VertexStage
                    ).sizeBytes(SizeUtil.MATRIX_SIZE_BYTES)
            ));


        }

        //UI
        {
            uiCamera = new Camera(
                    new Matrix4f().identity(),
                    new Matrix4f().ortho(0, renderer.getWidth(), 0, renderer.getHeight(), 0, 1, true),
                    false
            );

            ShaderReader.ShaderSources shaderSources = ShaderReader.read(
                    AssetPacks.<String>getAsset("core:assets/shaders/vulkan/UI.glsl").asset
            );


            uiShaderProgram = ShaderProgram.newGraphicsShaderProgram(renderer, TextureFormatType.ColorR8G8B8A8StandardRGB, TextureFormatType.Depth32Float);
            uiShaderProgram.setShaders(
                    Shader.newShader(uiShaderProgram, ShaderType.Vertex, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Vertex), ShaderType.Vertex)),
                    Shader.newShader(uiShaderProgram, ShaderType.Fragment, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Fragment), ShaderType.Fragment))
            );


            uiShaderProgram.bind(
                    new VertexAttributes.Type[]{
                            PositionFloat2,
                            ColorFloat4
                    },
                    new ShaderResSet(
                            0,
                            new ShaderRes(
                                    "camera",
                                    0,
                                    UniformBuffer,
                                    VertexStage
                            ).sizeBytes(SizeUtil.MATRIX_SIZE_BYTES)
                    )
            );

            uiVertexBuffer = Buffer.newBuffer(
                    renderer,
                    VertexAttributes.getSize(uiShaderProgram.getAttributes()) * Float.BYTES * 4,
                    Buffer.Usage.VertexBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );
            uiIndexBuffer = Buffer.newBuffer(
                    renderer,
                    6 * Integer.BYTES,
                    Buffer.Usage.IndexBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );

            uiCameraBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                uiCameraBuffers[i] = Buffer.newBuffer(
                        renderer,
                        Camera.SIZE,
                        Buffer.Usage.UniformBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
            }

            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {

                ByteBuffer uiCameraData = uiCameraBuffers[frameIndex].get();

                uiCamera.getProj().get(0, uiCameraData);


                uiShaderProgram.updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("camera", 0, 0, uiCameraBuffers[frameIndex])
                );
            }

            uiVertexBuffer.get().clear();
            uiIndexBuffer.get().clear();


            float x = 100, y = 100, w = 300, h = 300;
            Color color = Color.RED;

            ByteBuffer uiVertexBufferData = uiVertexBuffer.get();
            ByteBuffer uiIndexBufferData = uiIndexBuffer.get();


            uiVertexBufferData.putFloat(x);
            uiVertexBufferData.putFloat(y);
            uiVertexBufferData.putFloat(color.r);
            uiVertexBufferData.putFloat(color.g);
            uiVertexBufferData.putFloat(color.b);
            uiVertexBufferData.putFloat(color.a);

            uiVertexBufferData.putFloat(x);
            uiVertexBufferData.putFloat(y + h);
            uiVertexBufferData.putFloat(color.r);
            uiVertexBufferData.putFloat(color.g);
            uiVertexBufferData.putFloat(color.b);
            uiVertexBufferData.putFloat(color.a);

            uiVertexBufferData.putFloat(x + w);
            uiVertexBufferData.putFloat(y + h);
            uiVertexBufferData.putFloat(color.r);
            uiVertexBufferData.putFloat(color.g);
            uiVertexBufferData.putFloat(color.b);
            uiVertexBufferData.putFloat(color.a);

            uiVertexBufferData.putFloat(x + w);
            uiVertexBufferData.putFloat(y);
            uiVertexBufferData.putFloat(color.r);
            uiVertexBufferData.putFloat(color.g);
            uiVertexBufferData.putFloat(color.b);
            uiVertexBufferData.putFloat(color.a);

            uiIndexBufferData.putInt(0);
            uiIndexBufferData.putInt(1);
            uiIndexBufferData.putInt(2);
            uiIndexBufferData.putInt(2);
            uiIndexBufferData.putInt(3);
            uiIndexBufferData.putInt(0);
        }
    }

    @Override
    public void run() {
        scene.getEngine().findEntitiesWith(CameraComponent.class).stream().forEach(components1 -> {
            CameraComponent cameraComponent = components1.comp();
            sceneCamera = cameraComponent.camera();
        });

        graphicsCommands.startRecording(
                renderer.getFrameStartSync(),
                renderer.getSwapchainRenderTarget(),
                renderer.getFrameIndex()
        );
        {


            scene.getEngine().findEntitiesWith(TransformComponent.class, StaticMeshComponent.class).stream().forEach(components -> {


                TransformComponent transformComponent = components.comp1();
                StaticMeshComponent staticMeshComponent = components.comp2();

                ByteBuffer transformsData = staticMeshComponent.staticMeshBatch().getTransformsBuffers()[renderer.getFrameIndex()].get();
                transformComponent.transform().get(transformComponent.transformIndex() * SizeUtil.MATRIX_SIZE_BYTES, transformsData);
                ByteBuffer cameraData = staticMeshComponent.staticMeshBatch().getCameraBuffers()[renderer.getFrameIndex()].get();

                sceneCamera.getView().get(0, cameraData);
                sceneCamera.getProj().get(SizeUtil.MATRIX_SIZE_BYTES, cameraData);

                graphicsCommands.setDrawBuffers(
                        staticMeshComponent.staticMeshBatch().getVertexBuffer(),
                        staticMeshComponent.staticMeshBatch().getIndexBuffer()
                );
                graphicsCommands.setShaderProgram(
                        staticMeshComponent.staticMeshBatch().getShaderProgram()
                );
                graphicsCommands.drawIndexed(staticMeshComponent.staticMeshBatch().getIndexCount());
            });


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
                graphicsCommands.drawIndexed(dynamicMeshComponent.dynamicMesh().getIndexCount());
            });

            graphicsCommands.setDrawBuffers(uiVertexBuffer, uiIndexBuffer);
            graphicsCommands.setShaderProgram(uiShaderProgram);
            graphicsCommands.drawIndexed(6);

        }
        graphicsCommands.endRecording();

        computeCommands.startRecording(
                graphicsCommands.getFinishedSemaphores(),
                renderer.getSwapchainRenderTarget(),
                renderer.getFrameIndex()
        );
        {
            computeCommands.setShaderProgram(computeShaderProgram);
            computeCommands.dispatch(10, 10, 10);
        }
        computeCommands.endRecording();






        renderer.addCommandList(graphicsCommands);
        renderer.addCommandList(computeCommands);
    }

    @Override
    public void dispose() {

    }
}
