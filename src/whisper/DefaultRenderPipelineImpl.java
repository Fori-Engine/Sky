package whisper;

import dev.dominion.ecs.api.Results;
import fori.asset.AssetPacks;
import fori.ecs.*;
import fori.graphics.*;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.system.MemoryStack.*;

import java.nio.ByteBuffer;
import java.util.Iterator;


public class DefaultRenderPipelineImpl extends RenderPipeline {



    private Camera sceneCamera;
    private GraphicsPass shadowMapGenPass;
    private int shadowMapGenPassLightIndex = 0;


    private GraphicsPass scenePass;
    private RenderTarget scenePassRT;
    private Resource<Texture[]> sceneColorTextures;
    private Resource<Texture[]> scenePosTextures;

    private ComputePass shadowMapPass;
    private RenderTarget shadowMapPassRT;
    private Resource<Texture[]> shadowMapPassColorTextures;
    private ShaderProgram shadowMapPassShaderProgram;
    private Buffer[] shadowMapPassSceneDescBuffers;


    private GraphicsPass swapchainPass;
    private RenderTarget swapchainRT;
    private Resource<Texture[]> swapchainColorTextures;

    private ShaderProgram swapchainPassShaderProgram;
    private Camera swapchainPassCamera;
    private Buffer swapchainPassVertexBuffer, swapchainPassIndexBuffer;
    private Buffer[] swapchainPassCameraBuffers;

    private int lightCount = 0;


    private RenderGraph renderGraph;


    @Override
    public void init(Renderer renderer, Scene scene) {



        //Scene Color Pass Resources
        {
            scenePassRT = new RenderTarget(renderer);
            sceneColorTextures = new Resource<>(new Texture[]{
                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32, Texture.Filter.Nearest, Texture.Filter.Nearest),
                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32, Texture.Filter.Nearest, Texture.Filter.Nearest)
            });

            scenePosTextures = new Resource<>(new Texture[]{
                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32, Texture.Filter.Nearest, Texture.Filter.Nearest),
                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32, Texture.Filter.Nearest, Texture.Filter.Nearest)
            });

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentTypes.Color, sceneColorTextures.get())
            );

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentTypes.Pos, scenePosTextures.get())
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

            shadowMapPassShaderProgram = ShaderProgram.newShaderProgram(renderer);
            shadowMapPassShaderProgram.add(AssetPacks.getAsset("core:assets/shaders/ShadowMapPass_compute.spv"), ShaderType.ComputeShader);
            shadowMapPassShaderProgram.assemble();

            shadowMapPassSceneDescBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                shadowMapPassSceneDescBuffers[i] = Buffer.newBuffer(
                        renderer,
                        shadowMapPassShaderProgram.getDescriptorByName("sceneDesc").getSizeBytes(),
                        Buffer.Usage.ShaderStorageBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
            }

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


            swapchainPassShaderProgram = ShaderProgram.newShaderProgram(renderer);
            swapchainPassShaderProgram.add(AssetPacks.getAsset("core:assets/shaders/SwapchainPass_vertex.spv"), ShaderType.VertexShader);
            swapchainPassShaderProgram.add(AssetPacks.getAsset("core:assets/shaders/SwapchainPass_fragment.spv"), ShaderType.FragmentShader);
            swapchainPassShaderProgram.assemble();

            swapchainPassVertexBuffer = Buffer.newBuffer(
                    renderer,
                    swapchainPassShaderProgram.getVertexAttributesSize() * Float.BYTES * 4,
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
                        swapchainPassShaderProgram.getDescriptorByName("cameraProj").getSizeBytes(),
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
                        new DescriptorUpdate<>("cameraProj", swapchainPassCameraBuffers[frameIndex])
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
                    )
            );

        }


        shadowMapPass = Pass.newComputePass(renderer, "ShadowMap", renderer.getMaxFramesInFlight());
        {
            shadowMapPass.addDependencies(

                    new Dependency(
                            "InputTextures",
                            sceneColorTextures,
                            DependencyTypes.ComputeShaderRead
                    ),
                    new Dependency(
                            "InputShadowMaps",
                            null,
                            DependencyTypes.ComputeShaderRead
                    ),
                    new Dependency(
                            "OutputTextures",
                            shadowMapPassColorTextures,
                            DependencyTypes.ComputeShaderWrite
                    ),
                    new Dependency(
                            "InputPosTextures",
                            scenePosTextures,
                            DependencyTypes.ComputeShaderRead
                    )
            );
        }
        swapchainPass = Pass.newGraphicsPass(renderGraph, "Swapchain", renderer.getMaxFramesInFlight());
        {
            swapchainPass.addDependencies(

                    new Dependency(
                            "InputTextures",
                            shadowMapPassColorTextures,
                            DependencyTypes.FragmentShaderRead
                    ),
                    new Dependency(
                            "SwapchainColorTextures",
                            swapchainColorTextures,
                            DependencyTypes.RenderTargetWrite
                    ),
                    new Dependency(
                            "SwapchainColorTexturesPresent",
                            swapchainColorTextures,
                            DependencyTypes.Present
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

        if(swapchainRT != renderer.getSwapchainRenderTarget()) {
            swapchainRT = renderer.getSwapchainRenderTarget();
            RenderTargetAttachment colorAttachment = swapchainRT.getAttachment(RenderTargetAttachmentTypes.Color);
            swapchainColorTextures = new Resource<>(colorAttachment.getTextures());

            swapchainPass.getDependency(
                    "SwapchainColorTextures"
            ).setDependency(swapchainColorTextures);

            swapchainPass.getDependency(
                    "SwapchainColorTexturesPresent"
            ).setDependency(swapchainColorTextures);

        }

        scene.getEngine().findEntitiesWith(CameraComponent.class).stream().forEach(components1 -> {
            CameraComponent cameraComponent = components1.comp();
            sceneCamera = cameraComponent.camera();
        });




        //ShadowMapGen and ShadowMap pass Dynamic Resource Dependencies
        {

            lightCount = (int) scene.getEngine().findEntitiesWith(LightComponent.class).stream().count();
            Texture[] shadowMapTextures = new Texture[lightCount * renderer.getMaxFramesInFlight()];

            int lightIndex = 0;
            for(Iterator<Results.With1<LightComponent>> iterator = scene.getEngine().findEntitiesWith(LightComponent.class).stream().iterator(); iterator.hasNext();){
                LightComponent lightComponent = iterator.next().comp();

                shadowMapTextures[renderer.getMaxFramesInFlight() * lightIndex] = lightComponent.renderTarget
                        .getAttachment(RenderTargetAttachmentTypes.Pos)
                        .getTextures()[0];

                shadowMapTextures[(renderer.getMaxFramesInFlight() * lightIndex) + 1] = lightComponent.renderTarget
                        .getAttachment(RenderTargetAttachmentTypes.Pos)
                        .getTextures()[1];

                lightIndex++;
            }

            Resource<Texture[]> shadowMapTexturesResource = new Resource<>(shadowMapTextures);

            shadowMapGenPass.getDependency(
                    "OutputShadowMaps"
            ).setDependency(shadowMapTexturesResource);

            shadowMapPass.getDependency(
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
                ByteBuffer sceneDescData = shadowMapPassSceneDescBuffers[renderer.getFrameIndex()].get();
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


                        shadowMapGenPass.startRendering(lightComponent.renderTarget, width, height, true, Color.BLACK);
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

                    scenePass.startRendering(scenePassRT, renderer.getWidth(), renderer.getHeight(), true, Color.BLACK);
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




        shadowMapPass.setPassExecuteCallback(() -> {

            shadowMapPassShaderProgram.updateBuffers(
                    renderer.getFrameIndex(),
                    new DescriptorUpdate<>(
                            "sceneDesc",
                            shadowMapPassSceneDescBuffers[renderer.getFrameIndex()]
                    )
            );

            shadowMapPassShaderProgram.updateTextures(
                    renderer.getFrameIndex(),
                    new DescriptorUpdate<>(
                            "inputTexture",
                            ((Texture[]) shadowMapPass.getDependency("InputTextures").getDependency().get())[renderer.getFrameIndex()]
                    )
            );

            shadowMapPassShaderProgram.updateTextures(
                    renderer.getFrameIndex(),
                    new DescriptorUpdate<>(
                            "outputTexture",
                            ((Texture[]) shadowMapPass.getDependency("OutputTextures").getDependency().get())[renderer.getFrameIndex()]
                    )
            );

            shadowMapPassShaderProgram.updateTextures(
                    renderer.getFrameIndex(),
                    new DescriptorUpdate<>(
                            "inputPosTexture",
                            ((Texture[]) shadowMapPass.getDependency("InputPosTextures").getDependency().get())[renderer.getFrameIndex()]
                    )
            );



            //Update shadow maps
            {
                Texture[] shadowMapTextures = ((Texture[]) shadowMapPass.getDependency("InputShadowMaps").getDependency().get());

                for (int i = 0; i < lightCount; i++) {
                    shadowMapPassShaderProgram.updateTextures(
                            renderer.getFrameIndex(),
                            new DescriptorUpdate<>(
                                    "inputShadowMaps",
                                    shadowMapTextures[renderer.getMaxFramesInFlight() * i + renderer.getFrameIndex()]
                            ).arrayIndex(i)
                    );
                }
            }

            shadowMapPass.startRecording(renderer.getFrameIndex());
            {
                shadowMapPass.resolveBarriers();
                shadowMapPass.setShaderProgram(shadowMapPassShaderProgram);
                shadowMapPass.dispatch(1920, 1080, 1);
            }
            shadowMapPass.endRecording();
        });


        swapchainPass.setPassExecuteCallback(() -> {

            swapchainPassShaderProgram.updateTextures(
                    renderer.getFrameIndex(),
                    new DescriptorUpdate<>(
                            "inputTexture",
                            ((Texture[]) swapchainPass.getDependency("InputTextures").getDependency().get())[renderer.getFrameIndex()])
            );

            swapchainPass.startRecording(renderer.getFrameIndex());
            {

                swapchainPass.resolveBarriers();

                swapchainPass.startRendering(renderer.getSwapchainRenderTarget(), renderer.getWidth(), renderer.getHeight(), true, Color.BLACK);
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
}
