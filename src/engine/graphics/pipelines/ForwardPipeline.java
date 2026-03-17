package engine.graphics.pipelines;

import engine.Pair;
import engine.asset.AssetRegistry;
import engine.ecs.*;
import engine.graphics.*;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.List;

import static engine.graphics.Texture.Filter.Linear;
import static org.lwjgl.system.MemoryStack.stackPush;


public class ForwardPipeline extends RenderPipeline {



    private Camera sceneCamera;
    private GraphicsPass shadowMapGenPass;
    private int shadowMapGenPassLightIndex = 0;


    private GraphicsPass scenePass;
    private RenderTarget scenePassRT;
    private Resource<Pair<Texture[], Sampler[]>> sceneColor0Textures;
    private Resource<Pair<Texture[], Sampler[]>> sceneDepthStencilTextures;

    private GraphicsPass displayPass;
    private RenderTarget displayPassRT;
    private Resource<Texture[]> displayPassColorTextures;

    private ShaderProgram displayPassShaderProgram;
    private Camera displayPassCamera;
    private Buffer[] displayPassVertexBuffers, displayPassIndexBuffers;
    private Buffer[] displayPassCameraBuffers;

    private int lightCount = 0;


    @Override
    public void init(Renderer renderer) {

        //Features
        {
            supportedFeatures = List.of(
                    new SceneFeatures(true),
                    new ScreenSpaceFeatures(false)
            );
        }



        //Scene Color Pass Resources
        {
            scenePassRT = new RenderTarget(renderer);
            sceneColor0Textures = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32),
                                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32)
                            },
                            new Sampler[]{
                                    Sampler.newSampler(scenePassRT, Linear, Linear, true),
                                    Sampler.newSampler(scenePassRT, Linear, Linear, true)
                            }
                    )
            );



            sceneDepthStencilTextures = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newDepthTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.Depth32),
                                    Texture.newDepthTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.Depth32)
                            },
                            null
                    )
            );

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Color0,
                            sceneColor0Textures.get().key,
                            sceneColor0Textures.get().value
                    )
            );


            scenePassRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Depth,
                            sceneDepthStencilTextures.get().key,
                            sceneDepthStencilTextures.get().value

                    )
            );
        }

        //Display Pass Resources
        {
            displayPassRT = renderer.getSwapchainRenderTarget();
            displayPassColorTextures = new Resource<>(
                    displayPassRT.getAttachment(RenderTargetAttachmentTypes.Color0).getTextures()
            );

            displayPassCamera = new Camera(
                    new Matrix4f().identity(),
                    new Matrix4f().ortho(0, renderer.getWidth(), 0, renderer.getHeight(), 0, 1, true),
                    false
            );


            ScreenSpaceFeatures screenSpaceFeatures = getFeatures(ScreenSpaceFeatures.class);

            displayPassShaderProgram = ShaderProgram.newShaderProgram(renderer);
            displayPassShaderProgram.setDepthTestType(DepthTestType.Always);
            displayPassShaderProgram.setEnableBlending(true);
            displayPassShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/forward/Display_vertex.spv"), ShaderType.VertexShader);
            displayPassShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/forward/Display_fragment.spv"), ShaderType.FragmentShader);
            displayPassShaderProgram.assemble();

            displayPassVertexBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            displayPassIndexBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            displayPassCameraBuffers = new Buffer[renderer.getMaxFramesInFlight()];

            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                displayPassVertexBuffers[i] = Buffer.newBuffer(
                        renderer,
                        displayPassShaderProgram.getVertexAttributesSize() * Float.BYTES * 4 * screenSpaceFeatures.getMaxQuads(),
                        Buffer.Usage.VertexBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
                displayPassIndexBuffers[i] = Buffer.newBuffer(
                        renderer,
                        Integer.BYTES * 6 * screenSpaceFeatures.getMaxQuads(),
                        Buffer.Usage.IndexBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );

                displayPassCameraBuffers[i] = Buffer.newBuffer(
                        renderer,
                        displayPassShaderProgram.getDescriptorByName("camera").getSizeBytes(),
                        Buffer.Usage.UniformBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );

                ByteBuffer swapchainPassCameraBufferData = displayPassCameraBuffers[i].get();

                displayPassCamera.getProj().get(0, swapchainPassCameraBufferData);
                displayPassShaderProgram.setBuffers(
                        i,
                        new DescriptorUpdate<>("camera", displayPassCameraBuffers[i])
                );


            }


            screenSpaceFeatures.setVertexBuffers(displayPassVertexBuffers);
            screenSpaceFeatures.setIndexBuffers(displayPassIndexBuffers);
            screenSpaceFeatures.setShaderProgram(displayPassShaderProgram);




        }

        renderGraph = new RenderGraph(renderer);

        shadowMapGenPass = Pass.newGraphicsPass(renderGraph, "ShadowMapGen", renderer.getMaxFramesInFlight());
        {
            shadowMapGenPass.addDependencies(
                    new Dependency(
                            "OutputShadowMaps",
                            null,
                            DependencyTypes.RenderTargetDepthWrite
                    )
            );
        }
        scenePass = Pass.newGraphicsPass(renderGraph, "Scene", renderer.getMaxFramesInFlight());
        {
            scenePass.addDependencies(
                    new Dependency(
                            "OutputColorTextures",
                            sceneColor0Textures,
                            DependencyTypes.RenderTargetWrite
                    ),
                    new Dependency(
                            "OutputDepthStencilTextures",
                            sceneDepthStencilTextures,
                            DependencyTypes.RenderTargetDepthWrite
                    )
            );

        }



        displayPass = Pass.newGraphicsPass(renderGraph, "Compose", renderer.getMaxFramesInFlight());
        {
            displayPass.addDependencies(

                    new Dependency(
                            "InputColorTextures",
                            sceneColor0Textures,
                            DependencyTypes.FragmentShaderRead
                    ),
                    new Dependency(
                            "SwapchainColorTextures",
                            displayPassColorTextures,
                            DependencyTypes.RenderTargetWrite
                    ),
                    new Dependency(
                            "SwapchainColorTexturesPresent",
                            displayPassColorTextures,
                            DependencyTypes.Present
                    )
            );
        }

        renderGraph.addPasses(
                shadowMapGenPass,
                scenePass,
                displayPass
        );
    }

    private void updateSceneDesc(ByteBuffer sceneDescData, Camera camera, Scene scene) {
        sceneDescData.clear();

        camera.getView().get(sceneDescData);
        sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

        camera.getProj().get(sceneDescData);
        sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

        camera.getInvView().get(sceneDescData);
        sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

        camera.getInvProj().get(sceneDescData);
        sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

        scene.getRootActor().previsitAllActors(actor -> {
            if(actor.has(SpotlightComponent.class)) {
                SpotlightComponent spotlightComponent = actor.getComponent(SpotlightComponent.class);

                spotlightComponent.view.get(sceneDescData);
                sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

                spotlightComponent.proj.get(sceneDescData);
                sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

                spotlightComponent.invView.get(sceneDescData);
                sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

                spotlightComponent.invProj.get(sceneDescData);
                sceneDescData.position(sceneDescData.position() + SizeUtil.MATRIX_SIZE_BYTES);

                sceneDescData.putFloat(spotlightComponent.attenuationConstant);
                sceneDescData.putFloat(spotlightComponent.attenuationLinear);
                sceneDescData.putFloat(spotlightComponent.attenuationQuadratic);
                sceneDescData.putFloat(-1);
                spotlightComponent.color.get(sceneDescData);
                sceneDescData.position(sceneDescData.position() + SizeUtil.VEC3_SIZE_BYTES);
                sceneDescData.putFloat(spotlightComponent.shadowNormalOffsetBias);
            }
        });




    }


    @Override
    public void render(Renderer renderer) {
        Scene scene = getFeatures(SceneFeatures.class).getScene();




        if(displayPassRT != renderer.getSwapchainRenderTarget()) {
            displayPassRT = renderer.getSwapchainRenderTarget();
            RenderTargetAttachment colorAttachment = displayPassRT.getAttachment(RenderTargetAttachmentTypes.Color0);
            displayPassColorTextures = new Resource<>(colorAttachment.getTextures());

            displayPass.getDependency(
                    "SwapchainColorTextures"
            ).setDependency(displayPassColorTextures);

            displayPass.getDependency(
                    "SwapchainColorTexturesPresent"
            ).setDependency(displayPassColorTextures);

        }

        scene.getRootActor().previsitAllActors(actor -> {
            if(actor.has(CameraComponent.class)) {
                CameraComponent cameraComponent = actor.getComponent(CameraComponent.class);
                sceneCamera = cameraComponent.camera;
            }
        });




        //ShadowMapGen and ShadowMap pass Dynamic Resource Dependencies
        {

            lightCount = 0;

            scene.getRootActor().previsitAllActors(actor -> {
                if (actor.has(SpotlightComponent.class))
                    lightCount++;
            });


            Texture[] shadowMapTextures = new Texture[lightCount * renderer.getMaxFramesInFlight()];
            Sampler[] shadowMapSamplers = new Sampler[shadowMapTextures.length];

            final int[] lightIndex = {0};

            scene.getRootActor().previsitAllActors(actor -> {
                if (actor.has(SpotlightComponent.class)) {
                    SpotlightComponent spotlightComponent = actor.getComponent(SpotlightComponent.class);
                    int i1 = renderer.getMaxFramesInFlight() * lightIndex[0];
                    int i2 = renderer.getMaxFramesInFlight() * lightIndex[0] + 1;

                    shadowMapTextures[i1] = spotlightComponent.renderTarget
                            .getAttachment(RenderTargetAttachmentTypes.Depth)
                            .getTextures()[0];

                    shadowMapSamplers[i1] = spotlightComponent.renderTarget
                            .getAttachment(RenderTargetAttachmentTypes.Depth)
                            .getSamplers()[0];

                    shadowMapTextures[i2] = spotlightComponent.renderTarget
                            .getAttachment(RenderTargetAttachmentTypes.Depth)
                            .getTextures()[1];

                    shadowMapSamplers[i2] = spotlightComponent.renderTarget
                            .getAttachment(RenderTargetAttachmentTypes.Depth)
                            .getSamplers()[1];

                    lightIndex[0]++;
                }
            });


            Resource<Pair<Texture[], Sampler[]>> shadowMapTexturesResource = new Resource<>(new Pair<>(shadowMapTextures, shadowMapSamplers));


            shadowMapGenPass.getDependency(
                    "OutputShadowMaps"
            ).setDependency(shadowMapTexturesResource);

        }

        //Update all in-memory scene desc/transform buffers before their descriptors are updated inside the passes
        {
            //Update entity shaders
            {
                scene.getRootActor().previsitAllActors(actor -> {
                    if(actor.has(TransformComponent.class)) {
                        TransformComponent transformComponent = actor.getComponent(TransformComponent.class);

                        if(actor.has(MeshListComponent.class)) {
                            MeshListComponent meshListComponent = actor.getComponent(MeshListComponent.class);
                            ByteBuffer transformsData = meshListComponent.transformsBuffers[renderer.getFrameIndex()].get();
                            transformComponent.transform().get(transformComponent.transformIndex() * SizeUtil.MATRIX_SIZE_BYTES, transformsData);
                            ByteBuffer sceneDescData = meshListComponent.sceneDescBuffers[renderer.getFrameIndex()].get();
                            updateSceneDesc(sceneDescData, sceneCamera, scene);
                        }
                        if(actor.has(MeshComponent.class)) {
                            MeshComponent meshComponent = actor.getComponent(MeshComponent.class);
                            ByteBuffer transformsData = meshComponent.transformsBuffers[renderer.getFrameIndex()].get();
                            transformComponent.transform().get(0, transformsData);
                            ByteBuffer sceneDescData = meshComponent.sceneDescBuffers[renderer.getFrameIndex()].get();
                            updateSceneDesc(sceneDescData, sceneCamera, scene);
                        }


                    }
                });



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

                    scene.getRootActor().previsitAllActors(actor -> {
                        if(actor.has(SpotlightComponent.class)) {
                            SpotlightComponent spotlightComponent = actor.getComponent(SpotlightComponent.class);



                            int width, height;
                            {
                                Texture texture = spotlightComponent.renderTarget.getAttachmentByIndex(0).getTextures()[0];
                                width = texture.getWidth();
                                height = texture.getHeight();
                            }


                            shadowMapGenPass.startRendering(spotlightComponent.renderTarget, 3, width, height, true, Color.BLACK);
                            {
                                shadowMapGenPass.setCullMode(CullMode.Front);


                                scene.getRootActor().previsitAllActors(e -> {
                                    if (e.has(MeshListComponent.class)) {
                                        MeshListComponent meshListComponent = e.getComponent(MeshListComponent.class);
                                        shadowMapGenPass.setDrawBuffers(
                                                meshListComponent.vertexBuffer,
                                                meshListComponent.indexBuffer
                                        );
                                        shadowMapGenPass.setShaderProgram(
                                                meshListComponent.shaderProgram
                                        );
                                        try (MemoryStack stack = stackPush()) {
                                            ByteBuffer pPushConstants = stack.calloc(2 * Integer.BYTES);
                                            pPushConstants.putInt(mode);
                                            pPushConstants.putInt(shadowMapGenPassLightIndex);
                                            shadowMapGenPass.setPushConstants(pPushConstants);
                                        }
                                        shadowMapGenPass.drawIndexed(meshListComponent.indexCount);
                                    }
                                    if (e.has(MeshComponent.class)) {
                                        MeshComponent meshComponent = e.getComponent(MeshComponent.class);
                                        shadowMapGenPass.setDrawBuffers(
                                                meshComponent.vertexBuffer,
                                                meshComponent.indexBuffer
                                        );
                                        shadowMapGenPass.setShaderProgram(
                                                meshComponent.shaderProgram
                                        );
                                        try (MemoryStack stack = stackPush()) {
                                            ByteBuffer pPushConstants = stack.calloc(2 * Integer.BYTES);
                                            pPushConstants.putInt(mode);
                                            pPushConstants.putInt(shadowMapGenPassLightIndex);
                                            shadowMapGenPass.setPushConstants(pPushConstants);
                                        }
                                        shadowMapGenPass.drawIndexed(meshComponent.indexCount);
                                    }
                                });

                            }
                            shadowMapGenPass.endRendering();
                            shadowMapGenPassLightIndex++;
                        }


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

                    scenePass.startRendering(scenePassRT, 0, renderer.getWidth(), renderer.getHeight(), true, Color.BLACK);
                    {
                        scenePass.setCullMode(CullMode.Back);
                        scene.getRootActor().previsitAllActors(actor -> {
                            if (actor.has(MeshListComponent.class)) {
                                MeshListComponent meshListComponent = actor.getComponent(MeshListComponent.class);

                                scenePass.setDrawBuffers(
                                        meshListComponent.vertexBuffer,
                                        meshListComponent.indexBuffer
                                );
                                scenePass.setShaderProgram(
                                        meshListComponent.shaderProgram
                                );
                                try (MemoryStack stack = stackPush()) {
                                    ByteBuffer pPushConstants = stack.calloc(2 * Integer.BYTES);
                                    pPushConstants.putInt(mode);
                                    pPushConstants.putInt(-1);
                                    scenePass.setPushConstants(pPushConstants);
                                }
                                scenePass.drawIndexed(meshListComponent.indexCount);
                            }
                            if(actor.has(MeshComponent.class)) {
                                MeshComponent meshComponent = actor.getComponent(MeshComponent.class);
                                scenePass.setDrawBuffers(
                                        meshComponent.vertexBuffer,
                                        meshComponent.indexBuffer
                                );
                                scenePass.setShaderProgram(
                                        meshComponent.shaderProgram
                                );
                                try(MemoryStack stack = stackPush()) {
                                    ByteBuffer pPushConstants = stack.calloc(2 * Integer.BYTES);
                                    pPushConstants.putInt(mode);
                                    pPushConstants.putInt(-1);
                                    scenePass.setPushConstants(pPushConstants);
                                }
                                scenePass.drawIndexed(meshComponent.indexCount);
                            }

                        });



                    }
                    scenePass.endRendering();
                }


            }
            scenePass.endRecording();

        });



        displayPass.setPassExecuteCallback(() -> {

            Resource<Pair<Texture[], Sampler[]>> inputTexturesResource = displayPass.getDependency("InputColorTextures").getResource();

            displayPassShaderProgram.setTextures(
                    renderer.getFrameIndex(),
                    new DescriptorUpdate<>(
                            "input_textures",
                            inputTexturesResource.get().key[renderer.getFrameIndex()]
                    ).arrayIndex(0)
            );

            displayPassShaderProgram.setSamplers(
                    renderer.getFrameIndex(),
                    new DescriptorUpdate<>(
                            "input_samplers",
                            inputTexturesResource.get().value[renderer.getFrameIndex()]
                    ).arrayIndex(0)
            );

            displayPass.startRecording(renderer.getFrameIndex());
            {

                displayPass.resolveBarriers();

                displayPass.startRendering(renderer.getSwapchainRenderTarget(), 0, renderer.getWidth(), renderer.getHeight(), true, Color.BLACK);
                {
                    ScreenSpaceFeatures screenSpaceFeatures = getFeatures(ScreenSpaceFeatures.class);
                    screenSpaceFeatures.setShaderProgram(displayPassShaderProgram);


                    displayPass.setCullMode(CullMode.Back);
                    displayPass.setDrawBuffers(displayPassVertexBuffers[renderer.getFrameIndex()], displayPassIndexBuffers[renderer.getFrameIndex()]);
                    displayPass.setShaderProgram(displayPassShaderProgram);
                    displayPass.drawIndexed(screenSpaceFeatures.getIndexCount());
                }
                displayPass.endRendering();



            }
            displayPass.endRecording();
        });



        renderGraph.setTargetPass(displayPass);
        renderer.render(renderGraph);
    }
}
