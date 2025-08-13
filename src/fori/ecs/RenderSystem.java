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






    private GraphicsPass sceneColorPass;
    private RenderTarget sceneColorRT;
    private Texture[] sceneColorTextures;

    private ComputePass mangaPass;
    private RenderTarget mangaColorRT;
    private Texture[] mangaColorTextures;
    private ShaderProgram mangaPassShaderProgram;


    private GraphicsPass swapchainPass;
    private RenderTarget swapchainRT;
    private Texture[] swapchainColorTextures;

    private ShaderProgram swapchainPassShaderProgram;
    private Camera swapchainPassCamera;
    private Buffer swapchainPassVertexBuffer, swapchainPassIndexBuffer;
    private Buffer[] swapchainPassCameraBuffers;




    private RenderGraph renderGraph;

    public RenderSystem(Renderer renderer, Scene scene) {
        this.renderer = renderer;
        this.scene = scene;


        //Scene Color Pass Resources
        {
            sceneColorRT = new RenderTarget(renderer);
            sceneColorTextures = new Texture[]{
                    Texture.newColorTexture(sceneColorRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR8G8B8A8, Texture.Filter.Nearest, Texture.Filter.Nearest),
                    Texture.newColorTexture(sceneColorRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR8G8B8A8, Texture.Filter.Nearest, Texture.Filter.Nearest)
            };

            sceneColorRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentType.Color, sceneColorTextures)
            );

            sceneColorRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentType.Depth, new Texture[]{
                            Texture.newDepthTexture(sceneColorRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.Depth32, Texture.Filter.Nearest, Texture.Filter.Nearest)
                    })
            );
        }

        //Manga Pass Resources
        {
            mangaColorRT = new RenderTarget(renderer);
            mangaColorTextures = new Texture[]{
                    Texture.newTexture(mangaColorRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32, Texture.Filter.Nearest, Texture.Filter.Nearest),
                    Texture.newTexture(mangaColorRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32, Texture.Filter.Nearest, Texture.Filter.Nearest)
            };

            mangaColorRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentType.Color, mangaColorTextures)
            );

            ShaderReader.ShaderSources shaderSources = ShaderReader.read(
                    AssetPacks.<String>getAsset("core:assets/shaders/vulkan/MangaPass.glsl").asset
            );

            mangaPassShaderProgram = ShaderProgram.newComputeShaderProgram(renderer);
            mangaPassShaderProgram.setShaders(
                    Shader.newShader(mangaPassShaderProgram, ShaderType.Compute, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Compute), ShaderType.Compute))
            );


            mangaPassShaderProgram.bind(
                    Optional.empty(),
                    new ShaderResSet(
                            0,
                            new ShaderRes(
                                    "inputTextures",
                                    0,
                                    CombinedSampler,
                                    ComputeStage
                            ).count(1),
                            new ShaderRes(
                                    "outputTextures",
                                    1,
                                    StorageImage,
                                    ComputeStage
                            ).count(1)
                    )

            );


        }

        //Swapchain Pass Resources
        {
            swapchainRT = renderer.getSwapchainRenderTarget();
            swapchainColorTextures = swapchainRT.getAttachment(RenderTargetAttachmentType.Color).getTextures();

            swapchainPassCamera = new Camera(
                    new Matrix4f().identity(),
                    new Matrix4f().ortho(0, renderer.getWidth(), 0, renderer.getHeight(), 0, 1, true),
                    false
            );

            ShaderReader.ShaderSources shaderSources = ShaderReader.read(
                    AssetPacks.<String>getAsset("core:assets/shaders/vulkan/SwapchainPass.glsl").asset
            );


            swapchainPassShaderProgram = ShaderProgram.newGraphicsShaderProgram(renderer, TextureFormatType.ColorR8G8B8A8, TextureFormatType.Depth32);
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
        mangaPass = Pass.newComputePass(renderer, "Manga", renderer.getMaxFramesInFlight());
        {
            mangaPass.setDependsOn(
                new ResourceDependency<>(
                    sceneColorTextures,
                    ResourceDependencyType.ComputeShaderRead
                ),
                new ResourceDependency<>(
                    mangaColorTextures,
                    ResourceDependencyType.ComputeShaderWrite
                )
            );
        }


        swapchainPass = Pass.newGraphicsPass(renderGraph, "Swapchain", renderer.getMaxFramesInFlight());
        {
            swapchainPass.setDependsOn(
                new ResourceDependency<>(
                    mangaColorTextures,
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
                mangaPass,
                swapchainPass
        );
    }

    @Override
    public void run() {
        scene.getEngine().findEntitiesWith(CameraComponent.class).stream().forEach(components1 -> {
            CameraComponent cameraComponent = components1.comp();
            sceneCamera = cameraComponent.camera();
        });


        if(swapchainRT != renderer.getSwapchainRenderTarget()) {
            swapchainRT = renderer.getSwapchainRenderTarget();

            RenderTargetAttachment colorAttachment = swapchainRT.getAttachment(RenderTargetAttachmentType.Color);
            Texture[] colorAttachmentTextures = colorAttachment.getTextures();
            for (int i = 0; i < colorAttachmentTextures.length; i++) {
                swapchainColorTextures[i] = colorAttachmentTextures[i];
            }

        }

        mangaPassShaderProgram.updateTextures(renderer.getFrameIndex(), new ShaderUpdate<>("inputTextures", 0, 0, sceneColorTextures[renderer.getFrameIndex()]).arrayIndex(0));
        mangaPassShaderProgram.updateTextures(renderer.getFrameIndex(), new ShaderUpdate<>("outputTextures", 0, 1, mangaColorTextures[renderer.getFrameIndex()]).arrayIndex(0));

        swapchainPassShaderProgram.updateTextures(renderer.getFrameIndex(), new ShaderUpdate<>("textures", 0, 1, mangaColorTextures[renderer.getFrameIndex()]).arrayIndex(0));


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
        mangaPass.setPassExecuteCallback(() -> {
            mangaPass.startRecording(renderer.getFrameIndex());
            {
                mangaPass.resolveBarriers();
                mangaPass.setShaderProgram(mangaPassShaderProgram);
                mangaPass.dispatch(1920, 1080, 1);
            }
            mangaPass.endRecording();
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
