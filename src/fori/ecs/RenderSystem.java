package fori.ecs;

import fori.asset.AssetPacks;
import fori.graphics.*;
import org.joml.Matrix4f;
import java.nio.ByteBuffer;
import java.util.Optional;

import static fori.graphics.ShaderRes.ShaderStage.*;
import static fori.graphics.ShaderRes.Type.*;
import static fori.graphics.VertexAttributes.Type.*;

public class RenderSystem extends EcsSystem {
    private Renderer renderer;
    private Scene scene;
    private Camera sceneCamera;



    private Camera swapchainPassCamera;
    private ShaderProgram swapchainPassShaderProgram;
    private Buffer swapchainPassVertexBuffer, swapchainPassIndexBuffer;
    private Buffer[] swapchainPassCameraBuffers;



    private GraphicsPass sceneColorPass, swapchainPass;
    private RenderTarget sceneColorRT;

    private Texture[] sceneColorTextures;
    private Texture[] swapchainColorTextures;

    private RenderGraph renderGraph;

    public RenderSystem(Renderer renderer, Scene scene) {
        this.renderer = renderer;
        this.scene = scene;


        {
            sceneColorRT = new RenderTarget(renderer, 3);
            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                sceneColorRT.addTexture(frameIndex, Texture.newColorTexture(sceneColorRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR8G8B8A8StandardRGB, Texture.Filter.Nearest, Texture.Filter.Nearest));
            }
            sceneColorRT.addTexture(2, Texture.newDepthTexture(sceneColorRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.Depth32Float, Texture.Filter.Nearest, Texture.Filter.Nearest));

            sceneColorTextures = new Texture[] {
                    sceneColorRT.getTexture(0),
                    sceneColorRT.getTexture(1)
            };

            swapchainColorTextures = new Texture[]{
                    renderer.getSwapchainRenderTarget().getTexture(0),
                    renderer.getSwapchainRenderTarget().getTexture(1)
            };

        }

        //Swapchain Pass Resources
        {
            swapchainPassCamera = new Camera(
                    new Matrix4f().identity(),
                    new Matrix4f().ortho(0, renderer.getWidth(), 0, renderer.getHeight(), 0, 1, true),
                    false
            );

            ShaderReader.ShaderSources shaderSources = ShaderReader.read(
                    AssetPacks.<String>getAsset("core:assets/shaders/vulkan/SwapchainPass.glsl").asset
            );


            swapchainPassShaderProgram = ShaderProgram.newGraphicsShaderProgram(renderer, TextureFormatType.ColorR8G8B8A8StandardRGB, TextureFormatType.Depth32Float);
            swapchainPassShaderProgram.setShaders(
                    Shader.newShader(swapchainPassShaderProgram, ShaderType.Vertex, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Vertex), ShaderType.Vertex)),
                    Shader.newShader(swapchainPassShaderProgram, ShaderType.Fragment, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Fragment), ShaderType.Fragment))
            );


            swapchainPassShaderProgram.bind(
                    Optional.of(new VertexAttributes.Type[]{
                            PositionFloat2,
                            ColorFloat4,
                            UVFloat2
                    }),
                    new ShaderResSet(
                            0,
                            new ShaderRes(
                                    "camera",
                                    0,
                                    UniformBuffer,
                                    VertexStage
                            ).sizeBytes(SizeUtil.MATRIX_SIZE_BYTES),
                            new ShaderRes(
                                    "textures",
                                    1,
                                    CombinedSampler,
                                    FragmentStage
                            ).count(1)
                    )

            );

            swapchainPassVertexBuffer = Buffer.newBuffer(
                    renderer,
                    VertexAttributes.getSize(swapchainPassShaderProgram.getAttributes().get()) * Float.BYTES * 4,
                    Buffer.Usage.VertexBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );
            swapchainPassIndexBuffer = Buffer.newBuffer(
                    renderer,
                    6 * Integer.BYTES,
                    Buffer.Usage.IndexBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );

            swapchainPassCameraBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                swapchainPassCameraBuffers[i] = Buffer.newBuffer(
                        renderer,
                        Camera.SIZE,
                        Buffer.Usage.UniformBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
            }

            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {

                ByteBuffer swapchainPassCameraBufferData = swapchainPassCameraBuffers[frameIndex].get();

                swapchainPassCamera.getProj().get(0, swapchainPassCameraBufferData);


                swapchainPassShaderProgram.updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("camera", 0, 0, swapchainPassCameraBuffers[frameIndex])
                );
            }

            swapchainPassVertexBuffer.get().clear();
            swapchainPassIndexBuffer.get().clear();


            {
                float x = 0, y = 0, w = 1920, h = 1080;

                ByteBuffer swapchainPassVertexBufferData = swapchainPassVertexBuffer.get();
                ByteBuffer swapchainPassIndexBufferData = swapchainPassIndexBuffer.get();


                swapchainPassVertexBufferData.putFloat(x);
                swapchainPassVertexBufferData.putFloat(y);
                swapchainPassVertexBufferData.putFloat(1f);
                swapchainPassVertexBufferData.putFloat(0f);
                swapchainPassVertexBufferData.putFloat(1f);
                swapchainPassVertexBufferData.putFloat(1f);
                swapchainPassVertexBufferData.putFloat(0);
                swapchainPassVertexBufferData.putFloat(0);

                swapchainPassVertexBufferData.putFloat(x);
                swapchainPassVertexBufferData.putFloat(y + h);
                swapchainPassVertexBufferData.putFloat(1f);
                swapchainPassVertexBufferData.putFloat(1f);
                swapchainPassVertexBufferData.putFloat(0f);
                swapchainPassVertexBufferData.putFloat(1f);
                swapchainPassVertexBufferData.putFloat(0);
                swapchainPassVertexBufferData.putFloat(1);


                swapchainPassVertexBufferData.putFloat(x + w);
                swapchainPassVertexBufferData.putFloat(y + h);
                swapchainPassVertexBufferData.putFloat(0f);
                swapchainPassVertexBufferData.putFloat(0f);
                swapchainPassVertexBufferData.putFloat(1f);
                swapchainPassVertexBufferData.putFloat(1f);
                swapchainPassVertexBufferData.putFloat(1);
                swapchainPassVertexBufferData.putFloat(1);


                swapchainPassVertexBufferData.putFloat(x + w);
                swapchainPassVertexBufferData.putFloat(y);
                swapchainPassVertexBufferData.putFloat(0f);
                swapchainPassVertexBufferData.putFloat(1f);
                swapchainPassVertexBufferData.putFloat(1f);
                swapchainPassVertexBufferData.putFloat(1f);
                swapchainPassVertexBufferData.putFloat(1);
                swapchainPassVertexBufferData.putFloat(0);


                swapchainPassIndexBufferData.putInt(0);
                swapchainPassIndexBufferData.putInt(1);
                swapchainPassIndexBufferData.putInt(2);
                swapchainPassIndexBufferData.putInt(2);
                swapchainPassIndexBufferData.putInt(3);
                swapchainPassIndexBufferData.putInt(0);
            }

        }

        renderGraph = new RenderGraph(renderer);

        sceneColorPass = Pass.newGraphicsPass(renderGraph, "SceneColor", renderer.getMaxFramesInFlight());
        {
            sceneColorPass.setDependsOn(
                new ResourceDependency<>(
                    sceneColorTextures,
                    ResourceDependencyType.RenderTargetWrite
                )
            );

        }
        swapchainPass = Pass.newGraphicsPass(renderGraph, "Swapchain", renderer.getMaxFramesInFlight());
        {
            swapchainPass.setDependsOn(
                new ResourceDependency<>(
                    sceneColorTextures,
                    ResourceDependencyType.FragmentShaderRead
                ),
                new ResourceDependency<>(
                    swapchainColorTextures,
                    ResourceDependencyType.RenderTargetWrite
                ),
                new ResourceDependency<>(
                    swapchainColorTextures,
                    ResourceDependencyType.Present
                )
            );
        }

        renderGraph.addPasses(
                sceneColorPass,
                swapchainPass
        );
    }

    @Override
    public void run() {
        scene.getEngine().findEntitiesWith(CameraComponent.class).stream().forEach(components1 -> {
            CameraComponent cameraComponent = components1.comp();
            sceneCamera = cameraComponent.camera();
        });


        swapchainPassShaderProgram.updateTextures(renderer.getFrameIndex(), new ShaderUpdate<>("textures", 0, 1, sceneColorRT.getTexture(renderer.getFrameIndex())).arrayIndex(0));


        sceneColorPass.setPassExecuteCallback(() -> {
            sceneColorPass.startRecording(renderer.getFrameIndex());
            {

                sceneColorPass.resolveBarriers();

                sceneColorPass.startRendering(sceneColorRT, true);
                {
                    scene.getEngine().findEntitiesWith(TransformComponent.class, StaticMeshComponent.class).stream().forEach(components -> {


                        TransformComponent transformComponent = components.comp1();
                        StaticMeshComponent staticMeshComponent = components.comp2();

                        ByteBuffer transformsData = staticMeshComponent.staticMeshBatch().getTransformsBuffers()[renderer.getFrameIndex()].get();
                        transformComponent.transform().get(transformComponent.transformIndex() * SizeUtil.MATRIX_SIZE_BYTES, transformsData);
                        ByteBuffer cameraData = staticMeshComponent.staticMeshBatch().getCameraBuffers()[renderer.getFrameIndex()].get();

                        sceneCamera.getView().get(0, cameraData);
                        sceneCamera.getProj().get(SizeUtil.MATRIX_SIZE_BYTES, cameraData);

                        sceneColorPass.setDrawBuffers(
                                staticMeshComponent.staticMeshBatch().getVertexBuffer(),
                                staticMeshComponent.staticMeshBatch().getIndexBuffer()
                        );
                        sceneColorPass.setShaderProgram(
                                staticMeshComponent.staticMeshBatch().getShaderProgram()
                        );
                        sceneColorPass.drawIndexed(staticMeshComponent.staticMeshBatch().getIndexCount());
                    });
                    scene.getEngine().findEntitiesWith(TransformComponent.class, DynamicMeshComponent.class).stream().forEach(components -> {


                        TransformComponent transformComponent = components.comp1();
                        DynamicMeshComponent dynamicMeshComponent = components.comp2();

                        ByteBuffer transformsData = dynamicMeshComponent.dynamicMesh().getTransformsBuffers()[renderer.getFrameIndex()].get();
                        transformComponent.transform().get(0, transformsData);
                        ByteBuffer cameraData = dynamicMeshComponent.dynamicMesh().getCameraBuffers()[renderer.getFrameIndex()].get();

                        sceneCamera.getView().get(0, cameraData);
                        sceneCamera.getProj().get(SizeUtil.MATRIX_SIZE_BYTES, cameraData);

                        sceneColorPass.setDrawBuffers(
                                dynamicMeshComponent.dynamicMesh().getVertexBuffer(),
                                dynamicMeshComponent.dynamicMesh().getIndexBuffer()
                        );
                        sceneColorPass.setShaderProgram(
                                dynamicMeshComponent.dynamicMesh().getShaderProgram()
                        );
                        sceneColorPass.drawIndexed(dynamicMeshComponent.dynamicMesh().getIndexCount());
                    });
                }
                sceneColorPass.endRendering();

            }
            sceneColorPass.endRecording();
        });
        swapchainPass.setPassExecuteCallback(() -> {
            swapchainPass.startRecording(renderer.getFrameIndex());
            {

                swapchainPass.resolveBarriers();

                swapchainPass.startRendering(renderer.getSwapchainRenderTarget(), true);
                {
                    swapchainPass.setDrawBuffers(swapchainPassVertexBuffer, swapchainPassIndexBuffer);
                    swapchainPass.setShaderProgram(swapchainPassShaderProgram);
                    swapchainPass.drawIndexed(6);
                }
                swapchainPass.endRendering();



            }
            swapchainPass.endRecording();
        });

        renderGraph.setTargetPass(swapchainPass);
        renderer.render(renderGraph);
    }

    @Override
    public void dispose() {

    }
}
