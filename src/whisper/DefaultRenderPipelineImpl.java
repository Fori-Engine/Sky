package whisper;

import fori.asset.AssetPacks;
import fori.ecs.*;
import fori.graphics.*;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

import static fori.graphics.ShaderRes.ShaderStage.*;
import static fori.graphics.ShaderRes.Type.*;
import static fori.graphics.ShaderRes.Type.CombinedSampler;
import static fori.graphics.VertexAttributes.Type.*;

public class DefaultRenderPipelineImpl extends RenderPipeline {

    private Camera sceneCamera;

    private GraphicsPass scenePass;
    private RenderTarget sceneRT;
    private Texture[] sceneColorTextures;
    private Texture[] scenePosTextures;

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


    @Override
    public void init(Renderer renderer, Scene scene) {
        //Scene Color Pass Resources
        {
            sceneRT = new RenderTarget(renderer);
            sceneColorTextures = new Texture[]{
                    Texture.newColorTexture(sceneRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR8G8B8A8, Texture.Filter.Nearest, Texture.Filter.Nearest),
                    Texture.newColorTexture(sceneRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR8G8B8A8, Texture.Filter.Nearest, Texture.Filter.Nearest)
            };
            scenePosTextures = new Texture[]{
                    Texture.newColorTexture(sceneRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR8G8B8A8, Texture.Filter.Nearest, Texture.Filter.Nearest),
                    Texture.newColorTexture(sceneRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR8G8B8A8, Texture.Filter.Nearest, Texture.Filter.Nearest)
            };


            sceneRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentTypes.Color, sceneColorTextures)
            );
            sceneRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentTypes.Pos, scenePosTextures)
            );

            sceneRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentTypes.Depth, new Texture[]{
                            Texture.newDepthTexture(sceneRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.Depth32, Texture.Filter.Nearest, Texture.Filter.Nearest)
                    })
            );
        }

        //Manga Pass Resources
        {
            mangaColorRT = new RenderTarget(renderer);
            mangaColorTextures = new Texture[]{
                    Texture.newStorageTexture(mangaColorRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32, Texture.Filter.Nearest, Texture.Filter.Nearest),
                    Texture.newStorageTexture(mangaColorRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32, Texture.Filter.Nearest, Texture.Filter.Nearest)
            };

            mangaColorRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentTypes.Color, mangaColorTextures)
            );

            ShaderReader.ShaderSources shaderSources = ShaderReader.read(
                    AssetPacks.<String>getAsset("core:assets/shaders/vulkan/MangaPass.glsl").asset
            );

            mangaPassShaderProgram = ShaderProgram.newComputeShaderProgram(renderer);
            mangaPassShaderProgram.addShader(
                    ShaderType.Compute,
                    Shader.newShader(mangaPassShaderProgram, ShaderType.Compute, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Compute), ShaderType.Compute))
            );


            mangaPassShaderProgram.bind(
                    new ShaderResSet(
                            0,
                            new ShaderRes(
                                    "inputColorTexture",
                                    0,
                                    CombinedSampler,
                                    ComputeStage
                            ).count(1),
                            new ShaderRes(
                                    "outputTexture",
                                    1,
                                    StorageImage,
                                    ComputeStage
                            ).count(1),
                            new ShaderRes(
                                    "inputPosTexture",
                                    2,
                                    CombinedSampler,
                                    ComputeStage
                            ).count(1)
                    )

            );


        }

        //Swapchain Pass Resources
        {
            swapchainRT = renderer.getSwapchainRenderTarget();
            swapchainColorTextures = swapchainRT.getAttachment(RenderTargetAttachmentTypes.Color).getTextures();

            swapchainPassCamera = new Camera(
                    new Matrix4f().identity(),
                    new Matrix4f().ortho(0, renderer.getWidth(), 0, renderer.getHeight(), 0, 1, true),
                    false
            );

            ShaderReader.ShaderSources shaderSources = ShaderReader.read(
                    AssetPacks.<String>getAsset("core:assets/shaders/vulkan/SwapchainPass.glsl").asset
            );


            swapchainPassShaderProgram = ShaderProgram.newGraphicsShaderProgram(renderer);
            swapchainPassShaderProgram.addShader(
                    ShaderType.Vertex,
                    Shader.newShader(swapchainPassShaderProgram, ShaderType.Vertex, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Vertex), ShaderType.Vertex))
                            .setVertexAttributes(new VertexAttributes.Type[]{
                                    PositionFloat2,
                                    ColorFloat4,
                                    UVFloat2
                            })
            );
            swapchainPassShaderProgram.addShader(
                    ShaderType.Fragment,
                    Shader.newShader(swapchainPassShaderProgram, ShaderType.Fragment, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Fragment), ShaderType.Fragment))
                            .setAttachmentTextureFormatTypes(TextureFormatType.ColorR8G8B8A8)
                            .setDepthAttachmentTextureFormatType(TextureFormatType.Depth32)
            );


            swapchainPassShaderProgram.bind(
                    new ShaderResSet(
                            0,
                            new ShaderRes(
                                    "camera",
                                    0,
                                    UniformBuffer,
                                    VertexStage
                            ).sizeBytes(SizeUtil.MATRIX_SIZE_BYTES),
                            new ShaderRes(
                                    "inputTexture",
                                    1,
                                    CombinedSampler,
                                    FragmentStage
                            ).count(1)
                    )

            );

            swapchainPassVertexBuffer = Buffer.newBuffer(
                    renderer,
                    VertexAttributes.getSize(swapchainPassShaderProgram.getShaderMap().get(ShaderType.Vertex).getVertexAttributes()) * Float.BYTES * 4,
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

        scenePass = Pass.newGraphicsPass(renderGraph, "SceneColor", renderer.getMaxFramesInFlight());
        {
            scenePass.setResourceDependencies(
                    new ResourceDependency<>(
                            "OutputColorTextures",
                            sceneColorTextures,
                            ResourceDependencyTypes.RenderTargetWrite
                    ),
                    new ResourceDependency<>(
                            "OutputPosTextures",
                            scenePosTextures,
                            ResourceDependencyTypes.RenderTargetWrite
                    )
            );

        }
        mangaPass = Pass.newComputePass(renderer, "Manga", renderer.getMaxFramesInFlight());
        {
            mangaPass.setResourceDependencies(
                    new ResourceDependency<>(
                            "InputColorTextures",
                            sceneColorTextures,
                            ResourceDependencyTypes.ComputeShaderRead
                    ),
                    new ResourceDependency<>(
                            "InputPosTextures",
                            scenePosTextures,
                            ResourceDependencyTypes.ComputeShaderRead
                    ),
                    new ResourceDependency<>(
                            "OutputTextures",
                            mangaColorTextures,
                            ResourceDependencyTypes.ComputeShaderWrite
                    )
            );
        }


        swapchainPass = Pass.newGraphicsPass(renderGraph, "Swapchain", renderer.getMaxFramesInFlight());
        {
            swapchainPass.setResourceDependencies(
                    new ResourceDependency<>(
                            "InputTextures",
                            mangaColorTextures,
                            ResourceDependencyTypes.FragmentShaderRead
                    ),
                    new ResourceDependency<>(
                            "SwapchainColorTextures",
                            swapchainColorTextures,
                            ResourceDependencyTypes.RenderTargetWrite
                    ),
                    new ResourceDependency<>(
                            "SwapchainColorTextures",
                            swapchainColorTextures,
                            ResourceDependencyTypes.Present
                    )
            );
        }

        renderGraph.addPasses(
                scenePass,
                mangaPass,
                swapchainPass
        );
    }

    @Override
    public void render(Renderer renderer, Scene scene) {
        scene.getEngine().findEntitiesWith(CameraComponent.class).stream().forEach(components1 -> {
            CameraComponent cameraComponent = components1.comp();
            sceneCamera = cameraComponent.camera();
        });


        if(swapchainRT != renderer.getSwapchainRenderTarget()) {
            swapchainRT = renderer.getSwapchainRenderTarget();

            RenderTargetAttachment colorAttachment = swapchainRT.getAttachment(RenderTargetAttachmentTypes.Color);
            Texture[] colorAttachmentTextures = colorAttachment.getTextures();
            for (int i = 0; i < colorAttachmentTextures.length; i++) {
                swapchainColorTextures[i] = colorAttachmentTextures[i];
            }

        }



        scenePass.setPassExecuteCallback(() -> {
            scenePass.startRecording(renderer.getFrameIndex());
            {

                scenePass.resolveBarriers();

                scenePass.startRendering(sceneRT, renderer.getWidth(), renderer.getHeight(),true, Color.BLACK);
                {
                    scene.getEngine().findEntitiesWith(TransformComponent.class, StaticMeshComponent.class).stream().forEach(components -> {


                        TransformComponent transformComponent = components.comp1();
                        StaticMeshComponent staticMeshComponent = components.comp2();

                        ByteBuffer transformsData = staticMeshComponent.staticMeshBatch().getTransformsBuffers()[renderer.getFrameIndex()].get();
                        transformComponent.transform().get(transformComponent.transformIndex() * SizeUtil.MATRIX_SIZE_BYTES, transformsData);
                        ByteBuffer cameraData = staticMeshComponent.staticMeshBatch().getCameraBuffers()[renderer.getFrameIndex()].get();

                        sceneCamera.getView().get(0, cameraData);
                        sceneCamera.getProj().get(SizeUtil.MATRIX_SIZE_BYTES, cameraData);

                        scenePass.setDrawBuffers(
                                staticMeshComponent.staticMeshBatch().getVertexBuffer(),
                                staticMeshComponent.staticMeshBatch().getIndexBuffer()
                        );
                        scenePass.setShaderProgram(
                                staticMeshComponent.staticMeshBatch().getShaderProgram()
                        );
                        scenePass.drawIndexed(staticMeshComponent.staticMeshBatch().getIndexCount(), 0);
                    });
                    scene.getEngine().findEntitiesWith(TransformComponent.class, DynamicMeshComponent.class).stream().forEach(components -> {


                        TransformComponent transformComponent = components.comp1();
                        DynamicMeshComponent dynamicMeshComponent = components.comp2();

                        ByteBuffer transformsData = dynamicMeshComponent.dynamicMesh().getTransformsBuffers()[renderer.getFrameIndex()].get();
                        transformComponent.transform().get(0, transformsData);
                        ByteBuffer cameraData = dynamicMeshComponent.dynamicMesh().getCameraBuffers()[renderer.getFrameIndex()].get();

                        sceneCamera.getView().get(0, cameraData);
                        sceneCamera.getProj().get(SizeUtil.MATRIX_SIZE_BYTES, cameraData);

                        scenePass.setDrawBuffers(
                                dynamicMeshComponent.dynamicMesh().getVertexBuffer(),
                                dynamicMeshComponent.dynamicMesh().getIndexBuffer()
                        );
                        scenePass.setShaderProgram(
                                dynamicMeshComponent.dynamicMesh().getShaderProgram()
                        );
                        scenePass.drawIndexed(dynamicMeshComponent.dynamicMesh().getIndexCount(), 0);
                    });
                }
                scenePass.endRendering();

            }
            scenePass.endRecording();
        });
        mangaPass.setPassExecuteCallback(() -> {
            mangaPassShaderProgram.updateTextures(
                    renderer.getFrameIndex(),
                    new ShaderUpdate<>(
                            "inputColorTexture",
                            0,
                            0,
                            ((Texture[]) mangaPass.getResourceDependencyByNameAndType("InputColorTextures", ResourceDependencyTypes.ComputeShaderRead).getDependency())[renderer.getFrameIndex()]
                    )
            );
            mangaPassShaderProgram.updateTextures(
                    renderer.getFrameIndex(),
                    new ShaderUpdate<>(
                            "outputTexture",
                            0,
                            1,
                            ((Texture[]) mangaPass.getResourceDependencyByNameAndType("OutputTextures", ResourceDependencyTypes.ComputeShaderWrite).getDependency())[renderer.getFrameIndex()]
                    )
            );
            mangaPassShaderProgram.updateTextures(
                    renderer.getFrameIndex(),
                    new ShaderUpdate<>(
                            "inputPosTexture",
                            0,
                            2,
                            ((Texture[]) mangaPass.getResourceDependencyByNameAndType("InputPosTextures", ResourceDependencyTypes.ComputeShaderRead).getDependency())[renderer.getFrameIndex()]
                    )
            );



            mangaPass.startRecording(renderer.getFrameIndex());
            {
                mangaPass.resolveBarriers();
                mangaPass.setShaderProgram(mangaPassShaderProgram);
                mangaPass.dispatch(1920, 1080, 1, 1);
            }
            mangaPass.endRecording();
        });
        swapchainPass.setPassExecuteCallback(() -> {
            swapchainPassShaderProgram.updateTextures(
                    renderer.getFrameIndex(),
                    new ShaderUpdate<>(
                            "inputTexture",
                            0,
                            1,
                            ((Texture[]) swapchainPass.getResourceDependencyByNameAndType("InputTextures", ResourceDependencyTypes.FragmentShaderRead).getDependency())[renderer.getFrameIndex()])
            );

            swapchainPass.startRecording(renderer.getFrameIndex());
            {

                swapchainPass.resolveBarriers();

                swapchainPass.startRendering(renderer.getSwapchainRenderTarget(), renderer.getWidth(), renderer.getHeight(), true, Color.BLACK);
                {
                    swapchainPass.setDrawBuffers(swapchainPassVertexBuffer, swapchainPassIndexBuffer);
                    swapchainPass.setShaderProgram(swapchainPassShaderProgram);
                    swapchainPass.drawIndexed(6, 1);
                }
                swapchainPass.endRendering();



            }
            swapchainPass.endRecording();
        });

        renderGraph.setTargetPass(swapchainPass);
        renderer.render(renderGraph);
    }
}
