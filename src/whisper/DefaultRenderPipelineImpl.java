package whisper;

import dev.dominion.ecs.api.Results;
import fori.asset.AssetPacks;
import fori.ecs.*;
import fori.graphics.*;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.Iterator;

import static fori.graphics.ShaderRes.ShaderStage.*;
import static fori.graphics.ShaderRes.Type.*;
import static fori.graphics.ShaderRes.Type.CombinedSampler;
import static fori.graphics.VertexAttributes.Type.*;

public class DefaultRenderPipelineImpl extends RenderPipeline {



    private Camera sceneCamera;
    private GraphicsPass shadowMapGenPass;
    private int shadowMapGenPassLightIndex = 0;


    private GraphicsPass scenePass;
    private RenderTarget scenePassRT;
    private Resource<Texture[]> sceneColorTextures;

    private ComputePass shadowMapPass;
    private RenderTarget shadowMapPassRT;
    private Resource<Texture[]> shadowMapPassColorTextures;
    private ShaderProgram shadowMapPassShaderProgram;


    private GraphicsPass swapchainPass;
    private RenderTarget swapchainRT;
    private Resource<Texture[]> swapchainColorTextures;

    private ShaderProgram swapchainPassShaderProgram;
    private Camera swapchainPassCamera;
    private Buffer swapchainPassVertexBuffer, swapchainPassIndexBuffer;
    private Buffer[] swapchainPassCameraBuffers;




    private RenderGraph renderGraph;


    @Override
    public void init(Renderer renderer, Scene scene) {



        //Scene Color Pass Resources
        {
            scenePassRT = new RenderTarget(renderer);
            sceneColorTextures = new Resource<>(new Texture[]{
                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR8G8B8A8, Texture.Filter.Nearest, Texture.Filter.Nearest),
                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR8G8B8A8, Texture.Filter.Nearest, Texture.Filter.Nearest)
            });

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentTypes.Color, sceneColorTextures.get())
            );

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentTypes.Depth, new Texture[]{
                            Texture.newDepthTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.Depth32, Texture.Filter.Nearest, Texture.Filter.Nearest)
                    })
            );
        }

        //Shadow Pass Resources
        {
            shadowMapPassRT = new RenderTarget(renderer);
            shadowMapPassColorTextures = new Resource<>(new Texture[]{
                    Texture.newStorageTexture(shadowMapPassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32, Texture.Filter.Nearest, Texture.Filter.Nearest),
                    Texture.newStorageTexture(shadowMapPassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32, Texture.Filter.Nearest, Texture.Filter.Nearest)
            });

            shadowMapPassRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentTypes.Color, shadowMapPassColorTextures.get())
            );

            ShaderReader.ShaderSources shaderSources = ShaderReader.read(
                    AssetPacks.<String>getAsset("core:assets/shaders/vulkan/ShadowMapPass.glsl").asset
            );

            shadowMapPassShaderProgram = ShaderProgram.newComputeShaderProgram(renderer, 2);
            shadowMapPassShaderProgram.addShader(
                    ShaderType.Compute,
                    Shader.newShader(shadowMapPassShaderProgram, ShaderType.Compute, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Compute), ShaderType.Compute))
            );


            shadowMapPassShaderProgram.bind(
                    new ShaderResSet(
                            0,

                            new ShaderRes(
                                    "inputTexture",
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
                                    "inputShadowMaps",
                                    2,
                                    CombinedSampler,
                                    ComputeStage
                            ).count(2)
                    )

            );


        }

        //Swapchain Pass Resources
        {
            swapchainRT = renderer.getSwapchainRenderTarget();
            swapchainColorTextures = new Resource<>(
                    swapchainRT.getAttachment(RenderTargetAttachmentTypes.Color).getTextures()
            );

            swapchainPassCamera = new Camera(
                    new Matrix4f().identity(),
                    new Matrix4f().ortho(0, renderer.getWidth(), 0, renderer.getHeight(), 0, 1, true),
                    false
            );

            ShaderReader.ShaderSources shaderSources = ShaderReader.read(
                    AssetPacks.<String>getAsset("core:assets/shaders/vulkan/SwapchainPass.glsl").asset
            );


            swapchainPassShaderProgram = ShaderProgram.newGraphicsShaderProgram(renderer, 1);
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
                        SizeUtil.MATRIX_SIZE_BYTES,
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

        shadowMapGenPass = Pass.newGraphicsPass(renderGraph, "ShadowMapGen", renderer.getMaxFramesInFlight());
        {
            shadowMapGenPass.addResourceDependencies(
                    new ResourceDependency(
                            "OutputShadowMaps",
                            null,
                            ResourceDependencyTypes.RenderTargetWrite
                    )
            );
        }
        scenePass = Pass.newGraphicsPass(renderGraph, "Scene", renderer.getMaxFramesInFlight());
        {
            scenePass.addResourceDependencies(
                    new ResourceDependency(
                            "OutputColorTextures",
                            sceneColorTextures,
                            ResourceDependencyTypes.RenderTargetWrite
                    )
            );

        }


        shadowMapPass = Pass.newComputePass(renderer, "ShadowMap", renderer.getMaxFramesInFlight());
        {
            shadowMapPass.addResourceDependencies(

                    new ResourceDependency(
                            "InputTextures",
                            sceneColorTextures,
                            ResourceDependencyTypes.ComputeShaderRead
                    ),
                    new ResourceDependency(
                            "InputShadowMaps",
                            null,
                            ResourceDependencyTypes.ComputeShaderRead
                    ),
                    new ResourceDependency(
                            "OutputTextures",
                            shadowMapPassColorTextures,
                            ResourceDependencyTypes.ComputeShaderWrite
                    )
            );
        }
        swapchainPass = Pass.newGraphicsPass(renderGraph, "Swapchain", renderer.getMaxFramesInFlight());
        {
            swapchainPass.addResourceDependencies(

                    new ResourceDependency(
                            "InputTextures",
                            shadowMapPassColorTextures,
                            ResourceDependencyTypes.FragmentShaderRead
                    ),
                    new ResourceDependency(
                            "SwapchainColorTextures",
                            swapchainColorTextures,
                            ResourceDependencyTypes.RenderTargetWrite
                    ),
                    new ResourceDependency(
                            "SwapchainColorTextures",
                            swapchainColorTextures,
                            ResourceDependencyTypes.Present
                    )
            );
        }

        renderGraph.addPasses(
                shadowMapGenPass,
                scenePass,
                shadowMapPass,
                swapchainPass
        );
    }

    private void updateSceneDesc(ByteBuffer sceneDescData, Camera camera, Scene scene) {
        sceneDescData.clear();
        camera.getView().get(sceneDescData);
        camera.getProj().get(SizeUtil.MATRIX_SIZE_BYTES, sceneDescData);

        int offset = 2 * SizeUtil.MATRIX_SIZE_BYTES;

        int lightIndex = 0;

        for(Iterator<Results.With1<LightComponent>> iterator = scene.getEngine().findEntitiesWith(LightComponent.class).stream().iterator(); iterator.hasNext();){
            LightComponent lightComponent = iterator.next().comp();

            lightComponent.view.get(offset + ((2 * lightIndex) * SizeUtil.MATRIX_SIZE_BYTES), sceneDescData);
            lightComponent.proj.get(offset + ((2 * lightIndex) + 1) * SizeUtil.MATRIX_SIZE_BYTES, sceneDescData);
            lightIndex++;
        }
    }


    @Override
    public void render(Renderer renderer, Scene scene) {

        if(swapchainRT != renderer.getSwapchainRenderTarget()) {
            swapchainRT = renderer.getSwapchainRenderTarget();
            RenderTargetAttachment colorAttachment = swapchainRT.getAttachment(RenderTargetAttachmentTypes.Color);
            swapchainColorTextures = new Resource<>(colorAttachment.getTextures());
        }

        scene.getEngine().findEntitiesWith(CameraComponent.class).stream().forEach(components1 -> {
            CameraComponent cameraComponent = components1.comp();
            sceneCamera = cameraComponent.camera();
        });




        //ShadowMapGen and ShadowMap pass Dynamic Resource Dependencies
        {

            int lightCount = (int) scene.getEngine().findEntitiesWith(LightComponent.class).stream().count();
            Texture[] shadowMapTextures = new Texture[lightCount];

            int lightIndex = 0;
            for(Iterator<Results.With1<LightComponent>> iterator = scene.getEngine().findEntitiesWith(LightComponent.class).stream().iterator(); iterator.hasNext();){
                LightComponent lightComponent = iterator.next().comp();

                shadowMapTextures[lightIndex] = lightComponent.renderTarget
                        .getAttachment(RenderTargetAttachmentTypes.Color)
                        .getTextures()[renderer.getFrameIndex()];

                lightIndex++;
            }

            Resource<Texture[]> shadowMapTexturesResource = new Resource<>(shadowMapTextures);

            shadowMapGenPass.getResourceDependencyByNameAndType(
                    "OutputShadowMaps",
                    ResourceDependencyTypes.RenderTargetWrite
            ).setDependency(shadowMapTexturesResource);

            shadowMapPass.getResourceDependencyByNameAndType(
                    "InputShadowMaps",
                    ResourceDependencyTypes.ComputeShaderRead
            ).setDependency(shadowMapTexturesResource);
        }

        //Update all in-memory scene desc/transform buffers before their descriptors are updated inside the passes
        {
            scene.getEngine().findEntitiesWith(TransformComponent.class, StaticMeshComponent.class).stream().forEach(components -> {

                TransformComponent transformComponent = components.comp1();
                StaticMeshComponent staticMeshComponent = components.comp2();

                ByteBuffer transformsData = staticMeshComponent.staticMeshBatch().getTransformsBuffers()[renderer.getFrameIndex()].get();
                transformComponent.transform().get(transformComponent.transformIndex() * SizeUtil.MATRIX_SIZE_BYTES, transformsData);
                ByteBuffer sceneDescData = staticMeshComponent.staticMeshBatch().getSceneDescBuffers()[renderer.getFrameIndex()].get();
                updateSceneDesc(sceneDescData, sceneCamera, scene);
            });
            scene.getEngine().findEntitiesWith(TransformComponent.class, DynamicMeshComponent.class).stream().forEach(components -> {
                TransformComponent transformComponent = components.comp1();
                DynamicMeshComponent dynamicMeshComponent = components.comp2();

                ByteBuffer transformsData = dynamicMeshComponent.dynamicMesh().getTransformsBuffers()[renderer.getFrameIndex()].get();
                transformComponent.transform().get(0, transformsData);
                ByteBuffer sceneDescData = dynamicMeshComponent.dynamicMesh().getSceneDescBuffers()[renderer.getFrameIndex()].get();
                updateSceneDesc(sceneDescData, sceneCamera, scene);
            });
        }

        shadowMapGenPassLightIndex = 0;
        shadowMapGenPass.setPassExecuteCallback(() -> {
            shadowMapGenPass.startRecording(renderer.getFrameIndex());
            {
                shadowMapGenPass.resolveBarriers();

                //Shadow Map Gen (mode 1)
                {
                    int mode = 1;



                    scene.getEngine().findEntitiesWith(LightComponent.class).stream().forEach(lightEntity -> {

                        LightComponent lightComponent = lightEntity.comp();


                        int width, height;
                        {
                            Texture texture = lightComponent.renderTarget.getAttachmentByIndex(0).getTextures()[0];
                            width = texture.getWidth();
                            height = texture.getHeight();
                        }


                        shadowMapGenPass.startRendering(lightComponent.renderTarget, width, height, true, Color.BLACK);
                        {




                            scene.getEngine().findEntitiesWith(TransformComponent.class, StaticMeshComponent.class).stream().forEach(components -> {

                                StaticMeshComponent staticMeshComponent = components.comp2();
                                shadowMapGenPass.setDrawBuffers(
                                        staticMeshComponent.staticMeshBatch().getVertexBuffer(),
                                        staticMeshComponent.staticMeshBatch().getIndexBuffer()
                                );
                                shadowMapGenPass.setShaderProgram(
                                        staticMeshComponent.staticMeshBatch().getShaderProgram()
                                );
                                shadowMapGenPass.drawIndexed(staticMeshComponent.staticMeshBatch().getIndexCount(), new int[]{mode, shadowMapGenPassLightIndex});
                            });
                            scene.getEngine().findEntitiesWith(TransformComponent.class, DynamicMeshComponent.class).stream().forEach(components -> {
                                DynamicMeshComponent dynamicMeshComponent = components.comp2();

                                shadowMapGenPass.setDrawBuffers(
                                        dynamicMeshComponent.dynamicMesh().getVertexBuffer(),
                                        dynamicMeshComponent.dynamicMesh().getIndexBuffer()
                                );
                                shadowMapGenPass.setShaderProgram(
                                        dynamicMeshComponent.dynamicMesh().getShaderProgram()
                                );
                                shadowMapGenPass.drawIndexed(dynamicMeshComponent.dynamicMesh().getIndexCount(), new int[]{mode, shadowMapGenPassLightIndex});
                            });
                        }
                        shadowMapGenPass.endRendering();
                        shadowMapGenPassLightIndex++;
                    });

                }

            }
            shadowMapGenPass.endRecording();

        });


        scenePass.setPassExecuteCallback(() -> {
            scenePass.startRecording(renderer.getFrameIndex());
            {

                scenePass.resolveBarriers();

                //Default Rendering (mode 0)
                {
                    int mode = 0;

                    scenePass.startRendering(scenePassRT, renderer.getWidth(), renderer.getHeight(), true, Color.BLACK);
                    {
                        scene.getEngine().findEntitiesWith(TransformComponent.class, StaticMeshComponent.class).stream().forEach(components -> {
                            StaticMeshComponent staticMeshComponent = components.comp2();

                            scenePass.setDrawBuffers(
                                    staticMeshComponent.staticMeshBatch().getVertexBuffer(),
                                    staticMeshComponent.staticMeshBatch().getIndexBuffer()
                            );
                            scenePass.setShaderProgram(
                                    staticMeshComponent.staticMeshBatch().getShaderProgram()
                            );
                            scenePass.drawIndexed(staticMeshComponent.staticMeshBatch().getIndexCount(), new int[]{mode, -1});
                        });
                        scene.getEngine().findEntitiesWith(TransformComponent.class, DynamicMeshComponent.class).stream().forEach(components -> {

                            DynamicMeshComponent dynamicMeshComponent = components.comp2();
                            scenePass.setDrawBuffers(
                                    dynamicMeshComponent.dynamicMesh().getVertexBuffer(),
                                    dynamicMeshComponent.dynamicMesh().getIndexBuffer()
                            );
                            scenePass.setShaderProgram(
                                    dynamicMeshComponent.dynamicMesh().getShaderProgram()
                            );
                            scenePass.drawIndexed(dynamicMeshComponent.dynamicMesh().getIndexCount(), new int[]{mode, -1});
                        });
                    }
                    scenePass.endRendering();
                }


            }
            scenePass.endRecording();
        });




        shadowMapPass.setPassExecuteCallback(() -> {


            shadowMapPassShaderProgram.updateTextures(
                    renderer.getFrameIndex(),
                    new ShaderUpdate<>(
                            "inputTexture",
                            0,
                            0,
                            ((Texture[]) shadowMapPass.getResourceDependencyByNameAndType("InputTextures", ResourceDependencyTypes.ComputeShaderRead).getDependency().get())[renderer.getFrameIndex()]
                    )
            );

            shadowMapPassShaderProgram.updateTextures(
                    renderer.getFrameIndex(),
                    new ShaderUpdate<>(
                            "outputTexture",
                            0,
                            1,
                            ((Texture[]) shadowMapPass.getResourceDependencyByNameAndType("OutputTextures", ResourceDependencyTypes.ComputeShaderWrite).getDependency().get())[renderer.getFrameIndex()]
                    )
            );


            //Update shadow maps
            {
                Texture[] shadowMapTextures = ((Texture[]) shadowMapPass.getResourceDependencyByNameAndType("InputShadowMaps", ResourceDependencyTypes.ComputeShaderRead).getDependency().get());

                for (int i = 0; i < shadowMapTextures.length; i++) {
                    shadowMapPassShaderProgram.updateTextures(
                            renderer.getFrameIndex(),
                            new ShaderUpdate<>(
                                    "inputShadowMaps",
                                    0,
                                    2,
                                    shadowMapTextures[i]
                            ).arrayIndex(i)
                    );
                }
            }

            shadowMapPass.startRecording(renderer.getFrameIndex());
            {
                shadowMapPass.resolveBarriers();
                shadowMapPass.setShaderProgram(shadowMapPassShaderProgram);
                shadowMapPass.dispatch(1920, 1080, 1, new int[]{1, -1});
            }
            shadowMapPass.endRecording();
        });


        swapchainPass.setPassExecuteCallback(() -> {

            swapchainPassShaderProgram.updateTextures(
                    renderer.getFrameIndex(),
                    new ShaderUpdate<>(
                            "inputTexture",
                            0,
                            1,
                            ((Texture[]) swapchainPass.getResourceDependencyByNameAndType("InputTextures", ResourceDependencyTypes.FragmentShaderRead).getDependency().get())[renderer.getFrameIndex()])
            );

            swapchainPass.startRecording(renderer.getFrameIndex());
            {

                swapchainPass.resolveBarriers();

                swapchainPass.startRendering(renderer.getSwapchainRenderTarget(), renderer.getWidth(), renderer.getHeight(), true, Color.BLACK);
                {
                    swapchainPass.setDrawBuffers(swapchainPassVertexBuffer, swapchainPassIndexBuffer);
                    swapchainPass.setShaderProgram(swapchainPassShaderProgram);
                    swapchainPass.drawIndexed(6, new int[]{1});
                }
                swapchainPass.endRendering();



            }
            swapchainPass.endRecording();
        });


        renderGraph.setTargetPass(swapchainPass);
        renderer.render(renderGraph);
    }
}
