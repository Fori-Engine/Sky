package engine.graphics.pipelines;

import dev.dominion.ecs.api.Results;
import engine.Pair;
import engine.asset.AssetRegistry;
import engine.ecs.*;
import engine.graphics.*;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import static engine.graphics.Texture.Filter.Linear;
import static org.lwjgl.system.MemoryStack.*;

import java.nio.ByteBuffer;
import java.util.Iterator;


public class DeferredPBRRenderPipeline extends RenderPipeline {



    private Camera sceneCamera;
    private GraphicsPass shadowMapGenPass;
    private int shadowMapGenPassLightIndex = 0;


    private GraphicsPass scenePass;
    private RenderTarget scenePassRT;
    private Resource<Pair<Texture[], Sampler[]>> sceneColorTextures;
    private Resource<Pair<Texture[], Sampler[]>> scenePosTextures;
    private Resource<Pair<Texture[], Sampler[]>> sceneNormalTextures;
    private Resource<Texture> sceneDepthTexture;

    private ComputePass lightingPass;
    private RenderTarget lightingPassRT;
    private Resource<Pair<Texture[], Sampler[]>> lightingPassColorTextures;
    private ShaderProgram lightingPassShaderProgram;
    private Buffer[] lightingPassSceneDescBuffers;


    private GraphicsPass composePass;
    private RenderTarget composePassRT;
    private Resource<Texture[]> composeColorTextures;

    private ShaderProgram composePassShaderProgram;
    private Camera composePassCamera;
    private Buffer composePassVertexBuffer, composePassIndexBuffer;
    private Buffer[] composePassCameraBuffers;

    private int lightCount = 0;
    private final int COMPUTE_THREAD_GROUP_SIZE = 32;





    @Override
    public void init(Renderer renderer, Scene scene) {



        //Scene Color Pass Resources
        {
            scenePassRT = new RenderTarget(renderer);
            sceneColorTextures = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32),
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32)
                            },
                            null
                    )
            );

            scenePosTextures = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32),
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32)
                            },
                            null
                    )
            );

            sceneNormalTextures = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32),
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32)
                            },
                            null
                    )
            );

            sceneDepthTexture = new Resource<>(
                    Texture.newDepthTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.Depth32)
            );

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Color,
                            sceneColorTextures.get().key,
                            sceneColorTextures.get().value
                    )
            );

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Pos,
                            scenePosTextures.get().key,
                            scenePosTextures.get().value

                    )
            );

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Normal,
                            sceneNormalTextures.get().key,
                            sceneNormalTextures.get().value

                    )
            );

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Depth,
                            new Texture[]{ sceneDepthTexture.get() },
                            null
                    )
            );
        }

        //Lighting Pass Resources
        {
            lightingPassRT = new RenderTarget(renderer);
            lightingPassColorTextures = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newStorageTexture(lightingPassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32),
                                    Texture.newStorageTexture(lightingPassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32)
                            },
                            new Sampler[]{
                                    Sampler.newSampler(lightingPassRT, Linear, Linear, true),
                                    Sampler.newSampler(lightingPassRT, Linear, Linear, true)
                            }
                    )
            );

            lightingPassRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Color,
                            lightingPassColorTextures.get().key,
                            lightingPassColorTextures.get().value
                    )
            );

            lightingPassShaderProgram = ShaderProgram.newShaderProgram(renderer);
            lightingPassShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/deferred_pbr_pipeline/Lighting_compute.spv"), ShaderType.ComputeShader);
            lightingPassShaderProgram.assemble();

            lightingPassSceneDescBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                lightingPassSceneDescBuffers[i] = Buffer.newBuffer(
                        renderer,
                        lightingPassShaderProgram.getDescriptorByName("sceneDesc").getSizeBytes(),
                        Buffer.Usage.ShaderStorageBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
            }

        }

        //Swapchain Pass Resources
        {
            composePassRT = renderer.getSwapchainRenderTarget();
            composeColorTextures = new Resource<>(
                    composePassRT.getAttachment(RenderTargetAttachmentTypes.Color).getTextures()
            );

            composePassCamera = new Camera(
                    new Matrix4f().identity(),
                    new Matrix4f().ortho(0, renderer.getWidth(), 0, renderer.getHeight(), 0, 1, true),
                    false
            );


            composePassShaderProgram = ShaderProgram.newShaderProgram(renderer);
            composePassShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/deferred_pbr_pipeline/Compose_vertex.spv"), ShaderType.VertexShader);
            composePassShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/deferred_pbr_pipeline/Compose_fragment.spv"), ShaderType.FragmentShader);
            composePassShaderProgram.assemble();

            composePassVertexBuffer = Buffer.newBuffer(
                    renderer,
                    composePassShaderProgram.getVertexAttributesSize() * Float.BYTES * 4,
                    Buffer.Usage.VertexBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );
            composePassIndexBuffer = Buffer.newBuffer(
                    renderer,
                    6 * Integer.BYTES,
                    Buffer.Usage.IndexBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );

            composePassCameraBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                composePassCameraBuffers[i] = Buffer.newBuffer(
                        renderer,
                        composePassShaderProgram.getDescriptorByName("camera").getSizeBytes(),
                        Buffer.Usage.UniformBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
            }

            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {

                ByteBuffer swapchainPassCameraBufferData = composePassCameraBuffers[frameIndex].get();

                composePassCamera.getProj().get(0, swapchainPassCameraBufferData);
                composePassShaderProgram.setBuffers(
                        frameIndex,
                        new DescriptorUpdate<>("camera", composePassCameraBuffers[frameIndex])
                );
            }

            composePassVertexBuffer.get().clear();
            composePassIndexBuffer.get().clear();


            {
                float x = 100, y = 50, w = 1920 / 2f, h = 1080 / 2f;

                ByteBuffer swapchainPassVertexBufferData = composePassVertexBuffer.get();
                ByteBuffer swapchainPassIndexBufferData = composePassIndexBuffer.get();


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
            shadowMapGenPass.addDependencies(
                    new Dependency(
                            "OutputShadowMaps",
                            null,
                            DependencyTypes.RenderTargetWrite
                    )
            );
        }
        scenePass = Pass.newGraphicsPass(renderGraph, "Scene", renderer.getMaxFramesInFlight());
        {
            scenePass.addDependencies(
                    new Dependency(
                            "OutputColorTextures",
                            sceneColorTextures,
                            DependencyTypes.RenderTargetWrite
                    ),
                    new Dependency(
                            "OutputPosTextures",
                            scenePosTextures,
                            DependencyTypes.RenderTargetWrite
                    ),
                    new Dependency(
                            "OutputNormalTextures",
                            sceneNormalTextures,
                            DependencyTypes.RenderTargetWrite
                    )
            );

        }


        lightingPass = Pass.newComputePass(renderer, "Lighting", renderer.getMaxFramesInFlight());
        {
            lightingPass.addDependencies(

                    new Dependency(
                            "InputColorTextures",
                            sceneColorTextures,
                            DependencyTypes.ComputeShaderRead
                    ),
                    new Dependency(
                            "InputPosTextures",
                            scenePosTextures,
                            DependencyTypes.ComputeShaderRead
                    ),
                    new Dependency(
                            "InputNormalTextures",
                            sceneNormalTextures,
                            DependencyTypes.ComputeShaderRead
                    ),
                    new Dependency(
                            "InputShadowMaps",
                            null,
                            DependencyTypes.ComputeShaderRead
                    ),
                    new Dependency(
                            "OutputColorTextures",
                            lightingPassColorTextures,
                            DependencyTypes.ComputeShaderWrite
                    )
            );
        }
        composePass = Pass.newGraphicsPass(renderGraph, "Compose", renderer.getMaxFramesInFlight());
        {
            composePass.addDependencies(

                    new Dependency(
                            "InputColorTextures",
                            lightingPassColorTextures,
                            DependencyTypes.FragmentShaderRead
                    ),
                    new Dependency(
                            "SwapchainColorTextures",
                            composeColorTextures,
                            DependencyTypes.RenderTargetWrite
                    ),
                    new Dependency(
                            "SwapchainColorTexturesPresent",
                            composeColorTextures,
                            DependencyTypes.Present
                    )
            );
        }

        renderGraph.addPasses(
                shadowMapGenPass,
                scenePass,
                lightingPass,
                composePass
        );
    }

    private void updateSceneDesc(ByteBuffer sceneDescData, Camera camera, Scene scene) {
        sceneDescData.clear();
        camera.getView().get(sceneDescData);
        camera.getProj().get(SizeUtil.MATRIX_SIZE_BYTES, sceneDescData);
        camera.getInvView().get(2 * SizeUtil.MATRIX_SIZE_BYTES, sceneDescData);
        camera.getInvProj().get(3 * SizeUtil.MATRIX_SIZE_BYTES, sceneDescData);


        int offset = 4 * SizeUtil.MATRIX_SIZE_BYTES;

        int lightIndex = 0;

        for(Iterator<Results.With1<LightComponent>> iterator = scene.getEngine().findEntitiesWith(LightComponent.class).stream().iterator(); iterator.hasNext();){
            LightComponent lightComponent = iterator.next().comp();

            lightComponent.view.get(offset + ((4 * lightIndex) * SizeUtil.MATRIX_SIZE_BYTES), sceneDescData);
            lightComponent.proj.get(offset + ((4 * lightIndex) + 1) * SizeUtil.MATRIX_SIZE_BYTES, sceneDescData);
            lightComponent.invView.get(offset + ((4 * lightIndex) + 2) * SizeUtil.MATRIX_SIZE_BYTES, sceneDescData);
            lightComponent.invProj.get(offset + ((4 * lightIndex) + 3) * SizeUtil.MATRIX_SIZE_BYTES, sceneDescData);

            lightIndex++;
        }
    }


    @Override
    public void render(Renderer renderer, Scene scene) {

        if(composePassRT != renderer.getSwapchainRenderTarget()) {
            composePassRT = renderer.getSwapchainRenderTarget();
            RenderTargetAttachment colorAttachment = composePassRT.getAttachment(RenderTargetAttachmentTypes.Color);
            composeColorTextures = new Resource<>(colorAttachment.getTextures());

            composePass.getDependency(
                    "SwapchainColorTextures"
            ).setDependency(composeColorTextures);

            composePass.getDependency(
                    "SwapchainColorTexturesPresent"
            ).setDependency(composeColorTextures);

        }

        scene.getEngine().findEntitiesWith(CameraComponent.class).stream().forEach(components1 -> {
            CameraComponent cameraComponent = components1.comp();
            sceneCamera = cameraComponent.camera();
        });




        //ShadowMapGen and ShadowMap pass Dynamic Resource Dependencies
        {

            lightCount = (int) scene.getEngine().findEntitiesWith(LightComponent.class).stream().count();
            Texture[] shadowMapTextures = new Texture[lightCount * renderer.getMaxFramesInFlight()];
            Sampler[] shadowMapSamplers = new Sampler[shadowMapTextures.length];

            int lightIndex = 0;
            for (Iterator<Results.With1<LightComponent>> iterator = scene.getEngine().findEntitiesWith(LightComponent.class).stream().iterator(); iterator.hasNext(); ) {
                LightComponent lightComponent = iterator.next().comp();

                int i1 = renderer.getMaxFramesInFlight() * lightIndex;
                int i2 = renderer.getMaxFramesInFlight() * lightIndex + 1;

                shadowMapTextures[i1] = lightComponent.renderTarget
                        .getAttachment(RenderTargetAttachmentTypes.Pos)
                        .getTextures()[0];

                shadowMapSamplers[i1] = lightComponent.renderTarget
                        .getAttachment(RenderTargetAttachmentTypes.Pos)
                        .getSamplers()[0];

                shadowMapTextures[i2] = lightComponent.renderTarget
                        .getAttachment(RenderTargetAttachmentTypes.Pos)
                        .getTextures()[1];

                shadowMapSamplers[i2] = lightComponent.renderTarget
                        .getAttachment(RenderTargetAttachmentTypes.Pos)
                        .getSamplers()[1];

                lightIndex++;
            }

            Resource<Pair<Texture[], Sampler[]>> shadowMapTexturesResource = new Resource<>(new Pair<>(shadowMapTextures, shadowMapSamplers));

            shadowMapGenPass.getDependency(
                    "OutputShadowMaps"
            ).setDependency(shadowMapTexturesResource);

            lightingPass.getDependency(
                    "InputShadowMaps"
            ).setDependency(shadowMapTexturesResource);
        }

        //Update all in-memory scene desc/transform buffers before their descriptors are updated inside the passes
        {
            //Update entity shaders
            {
                scene.getEngine().findEntitiesWith(TransformComponent.class, EnvironmentMeshComponent.class).stream().forEach(components -> {

                    TransformComponent transformComponent = components.comp1();
                    EnvironmentMeshComponent environmentMeshComponent = components.comp2();

                    ByteBuffer transformsData = environmentMeshComponent.transformsBuffers[renderer.getFrameIndex()].get();
                    transformComponent.transform().get(transformComponent.transformIndex() * SizeUtil.MATRIX_SIZE_BYTES, transformsData);
                    ByteBuffer sceneDescData = environmentMeshComponent.sceneDescBuffers[renderer.getFrameIndex()].get();
                    updateSceneDesc(sceneDescData, sceneCamera, scene);
                });
                scene.getEngine().findEntitiesWith(TransformComponent.class, ActorMeshComponent.class).stream().forEach(components -> {
                    TransformComponent transformComponent = components.comp1();
                    ActorMeshComponent actorMeshComponent = components.comp2();

                    ByteBuffer transformsData = actorMeshComponent.transformsBuffers[renderer.getFrameIndex()].get();
                    transformComponent.transform().get(0, transformsData);
                    ByteBuffer sceneDescData = actorMeshComponent.sceneDescBuffers[renderer.getFrameIndex()].get();
                    updateSceneDesc(sceneDescData, sceneCamera, scene);
                });
            }

            //Update ShadowMapPass shaders
            {
                ByteBuffer sceneDescData = lightingPassSceneDescBuffers[renderer.getFrameIndex()].get();
                updateSceneDesc(sceneDescData, sceneCamera, scene);
            }


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


                        shadowMapGenPass.startRendering(lightComponent.renderTarget, 2, width, height, true, Color.BLACK);
                        {




                            scene.getEngine().findEntitiesWith(TransformComponent.class, EnvironmentMeshComponent.class).stream().forEach(components -> {

                                EnvironmentMeshComponent environmentMeshComponent = components.comp2();
                                shadowMapGenPass.setDrawBuffers(
                                        environmentMeshComponent.vertexBuffer,
                                        environmentMeshComponent.indexBuffer
                                );
                                shadowMapGenPass.setShaderProgram(
                                        environmentMeshComponent.shaderProgram
                                );
                                try(MemoryStack stack = stackPush()) {
                                    ByteBuffer pPushConstants = stack.calloc(2 * Integer.BYTES);
                                    pPushConstants.putInt(mode);
                                    pPushConstants.putInt(shadowMapGenPassLightIndex);
                                    shadowMapGenPass.setPushConstants(pPushConstants);
                                }
                                shadowMapGenPass.drawIndexed(environmentMeshComponent.indexCount);
                            });
                            scene.getEngine().findEntitiesWith(TransformComponent.class, ActorMeshComponent.class).stream().forEach(components -> {
                                ActorMeshComponent actorMeshComponent = components.comp2();

                                shadowMapGenPass.setDrawBuffers(
                                        actorMeshComponent.vertexBuffer,
                                        actorMeshComponent.indexBuffer
                                );
                                shadowMapGenPass.setShaderProgram(
                                        actorMeshComponent.shaderProgram
                                );
                                try(MemoryStack stack = stackPush()) {
                                    ByteBuffer pPushConstants = stack.calloc(2 * Integer.BYTES);
                                    pPushConstants.putInt(mode);
                                    pPushConstants.putInt(shadowMapGenPassLightIndex);
                                    shadowMapGenPass.setPushConstants(pPushConstants);
                                }
                                shadowMapGenPass.drawIndexed(actorMeshComponent.indexCount);
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

                    scenePass.startRendering(scenePassRT, 0, renderer.getWidth(), renderer.getHeight(), true, Color.LIGHT_GRAY);
                    {
                        scene.getEngine().findEntitiesWith(TransformComponent.class, EnvironmentMeshComponent.class).stream().forEach(components -> {
                            EnvironmentMeshComponent environmentMeshComponent = components.comp2();

                            scenePass.setDrawBuffers(
                                    environmentMeshComponent.vertexBuffer,
                                    environmentMeshComponent.indexBuffer
                            );
                            scenePass.setShaderProgram(
                                    environmentMeshComponent.shaderProgram
                            );
                            try(MemoryStack stack = stackPush()) {
                                ByteBuffer pPushConstants = stack.calloc(2 * Integer.BYTES);
                                pPushConstants.putInt(mode);
                                pPushConstants.putInt(-1);
                                scenePass.setPushConstants(pPushConstants);
                            }
                            scenePass.drawIndexed(environmentMeshComponent.indexCount);
                        });
                        scene.getEngine().findEntitiesWith(TransformComponent.class, ActorMeshComponent.class).stream().forEach(components -> {

                            ActorMeshComponent actorMeshComponent = components.comp2();
                            scenePass.setDrawBuffers(
                                    actorMeshComponent.vertexBuffer,
                                    actorMeshComponent.indexBuffer
                            );
                            scenePass.setShaderProgram(
                                    actorMeshComponent.shaderProgram
                            );
                            try(MemoryStack stack = stackPush()) {
                                ByteBuffer pPushConstants = stack.calloc(2 * Integer.BYTES);
                                pPushConstants.putInt(mode);
                                pPushConstants.putInt(-1);
                                scenePass.setPushConstants(pPushConstants);
                            }
                            scenePass.drawIndexed(actorMeshComponent.indexCount);
                        });
                    }
                    scenePass.endRendering();
                }


            }
            scenePass.endRecording();
        });


        lightingPass.setPassExecuteCallback(() -> {

            lightingPassShaderProgram.setBuffers(
                    renderer.getFrameIndex(),
                    new DescriptorUpdate<>(
                            "sceneDesc",
                            lightingPassSceneDescBuffers[renderer.getFrameIndex()]
                    )
            );

            Resource<Pair<Texture[], Sampler[]>> inputColorTexturesDependency = lightingPass.getDependency("InputColorTextures").getResource();
            Resource<Pair<Texture[], Sampler[]>> outputColorTexturesDependency = lightingPass.getDependency("OutputColorTextures").getResource();
            Resource<Pair<Texture[], Sampler[]>> inputPosTexturesDependency = lightingPass.getDependency("InputPosTextures").getResource();
            Resource<Pair<Texture[], Sampler[]>> inputNormalTexturesDependency = lightingPass.getDependency("InputNormalTextures").getResource();




            lightingPassShaderProgram.setTextures(
                    renderer.getFrameIndex(),
                    new DescriptorUpdate<>(
                            "inputColorTexture",
                            inputColorTexturesDependency.get().key[renderer.getFrameIndex()]
                    ),
                    new DescriptorUpdate<>(
                            "inputPosTexture",
                            inputPosTexturesDependency.get().key[renderer.getFrameIndex()]
                    ),
                    new DescriptorUpdate<>(
                            "inputNormalTexture",
                            inputNormalTexturesDependency.get().key[renderer.getFrameIndex()]
                    ),
                    new DescriptorUpdate<>(
                            "outputColorTexture",
                            outputColorTexturesDependency.get().key[renderer.getFrameIndex()]
                    )
            );



            //Update shadow maps
            {
                Resource<Pair<Texture[], Sampler[]>> shadowMapTexturesResource = lightingPass.getDependency("InputShadowMaps").getResource();


                for (int i = 0; i < lightCount; i++) {
                    lightingPassShaderProgram.setTextures(
                            renderer.getFrameIndex(),
                            new DescriptorUpdate<>(
                                    "inputShadowMaps",
                                    shadowMapTexturesResource.get().key[renderer.getMaxFramesInFlight() * i + renderer.getFrameIndex()]
                            ).arrayIndex(i)
                    );
                    lightingPassShaderProgram.setSamplers(
                            renderer.getFrameIndex(),
                            new DescriptorUpdate<>(
                                    "inputShadowMapsSamplers",
                                    shadowMapTexturesResource.get().value[renderer.getMaxFramesInFlight() * i + renderer.getFrameIndex()]
                            ).arrayIndex(i)
                    );

                }
            }

            lightingPass.startRecording(renderer.getFrameIndex());
            {
                lightingPass.resolveBarriers();
                lightingPass.setShaderProgram(lightingPassShaderProgram);

                int groupCountX = (int) Math.ceil((float) renderer.getWidth() / COMPUTE_THREAD_GROUP_SIZE);
                int groupCountY = (int) Math.ceil((float) renderer.getHeight() / COMPUTE_THREAD_GROUP_SIZE);

                lightingPass.dispatch(groupCountX, groupCountY, 1);
            }
            lightingPass.endRecording();
        });


        composePass.setPassExecuteCallback(() -> {

            Resource<Pair<Texture[], Sampler[]>> inputTexturesResource = composePass.getDependency("InputColorTextures").getResource();

            composePassShaderProgram.setTextures(
                    renderer.getFrameIndex(),
                    new DescriptorUpdate<>(
                            "inputColorTexture",
                            inputTexturesResource.get().key[renderer.getFrameIndex()])
            );

            composePassShaderProgram.setSamplers(
                    renderer.getFrameIndex(),
                    new DescriptorUpdate<>(
                            "inputColorTextureSampler",
                            inputTexturesResource.get().value[renderer.getFrameIndex()])
            );

            composePass.startRecording(renderer.getFrameIndex());
            {

                composePass.resolveBarriers();

                composePass.startRendering(renderer.getSwapchainRenderTarget(), 0, renderer.getWidth(), renderer.getHeight(), true, Color.LIGHT_GRAY);
                {
                    composePass.setDrawBuffers(composePassVertexBuffer, composePassIndexBuffer);
                    composePass.setShaderProgram(composePassShaderProgram);
                    composePass.drawIndexed(6);
                }
                composePass.endRendering();



            }
            composePass.endRecording();
        });


        renderGraph.setTargetPass(composePass);
        renderer.render(renderGraph);
    }
}
