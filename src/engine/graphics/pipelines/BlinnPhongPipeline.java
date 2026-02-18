package engine.graphics.pipelines;

import engine.Pair;
import engine.asset.AssetRegistry;
import engine.ecs.*;
import engine.graphics.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import static engine.graphics.Texture.Filter.Linear;
import static org.lwjgl.system.MemoryStack.*;

import java.nio.ByteBuffer;
import java.util.List;


public class BlinnPhongPipeline extends RenderPipeline {



    private Camera sceneCamera;
    private GraphicsPass shadowMapGenPass;
    private int shadowMapGenPassLightIndex = 0;


    private GraphicsPass scenePass;
    private RenderTarget scenePassRT;
    private Resource<Pair<Texture[], Sampler[]>> sceneColorTextures;
    private Resource<Pair<Texture[], Sampler[]>> scenePosTextures;
    private Resource<Pair<Texture[], Sampler[]>> sceneNormalTextures;
    private Resource<Pair<Texture[], Sampler[]>> sceneDepthStencilTexture;

    private ComputePass lightingPass;
    private RenderTarget lightingPassRT;
    private Resource<Pair<Texture[], Sampler[]>> lightingPassColorTextures;
    private ShaderProgram lightingPassShaderProgram;
    private Buffer[] lightingPassSceneDescBuffers;


    private GraphicsPass displayPass;
    private RenderTarget displayPassRT;
    private Resource<Texture[]> displayPassColorTextures;

    private ShaderProgram displayPassShaderProgram;
    private Camera displayPassCamera;
    private Buffer displayPassVertexBuffer, displayPassIndexBuffer;
    private Buffer[] displayPassCameraBuffers;

    private int lightCount = 0;
    private final int COMPUTE_THREAD_GROUP_SIZE = 16;





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

            sceneDepthStencilTexture = new Resource<>(
                    new Pair<>(
                            new Texture[]{
                                    Texture.newDepthTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.Depth32)
                            },
                            null
                    )
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
                            new Texture[]{ sceneDepthStencilTexture.get().key[0] },
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
            lightingPassShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/blinn_phong/Lighting_compute.spv"), ShaderType.ComputeShader);
            lightingPassShaderProgram.assemble();

            lightingPassSceneDescBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                lightingPassSceneDescBuffers[i] = Buffer.newBuffer(
                        renderer,
                        lightingPassShaderProgram.getDescriptorByName("scene_desc").getSizeBytes(),
                        Buffer.Usage.ShaderStorageBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
            }

        }

        //Display Pass Resources
        {
            displayPassRT = renderer.getSwapchainRenderTarget();
            displayPassColorTextures = new Resource<>(
                    displayPassRT.getAttachment(RenderTargetAttachmentTypes.Color).getTextures()
            );

            displayPassCamera = new Camera(
                    new Matrix4f().identity(),
                    new Matrix4f().ortho(0, renderer.getWidth(), 0, renderer.getHeight(), 0, 1, true),
                    false
            );


            displayPassShaderProgram = ShaderProgram.newShaderProgram(renderer);
            displayPassShaderProgram.setDepthTestType(DepthTestType.Always);
            displayPassShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/blinn_phong/Display_vertex.spv"), ShaderType.VertexShader);
            displayPassShaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/blinn_phong/Display_fragment.spv"), ShaderType.FragmentShader);
            displayPassShaderProgram.assemble();

            displayPassVertexBuffer = Buffer.newBuffer(
                    renderer,
                    displayPassShaderProgram.getVertexAttributesSize() * Float.BYTES * 4 * 1000,
                    Buffer.Usage.VertexBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );
            displayPassIndexBuffer = Buffer.newBuffer(
                    renderer,
                    Integer.BYTES * 6 * 1000,
                    Buffer.Usage.IndexBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );

            displayPassCameraBuffers = new Buffer[renderer.getMaxFramesInFlight()];
            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                displayPassCameraBuffers[i] = Buffer.newBuffer(
                        renderer,
                        displayPassShaderProgram.getDescriptorByName("camera").getSizeBytes(),
                        Buffer.Usage.UniformBuffer,
                        Buffer.Type.CPUGPUShared,
                        false
                );
            }

            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {

                ByteBuffer swapchainPassCameraBufferData = displayPassCameraBuffers[frameIndex].get();

                displayPassCamera.getProj().get(0, swapchainPassCameraBufferData);
                displayPassShaderProgram.setBuffers(
                        frameIndex,
                        new DescriptorUpdate<>("camera", displayPassCameraBuffers[frameIndex])
                );
            }

            displayPassVertexBuffer.get().clear();
            displayPassIndexBuffer.get().clear();
            ScreenSpaceFeatures screenSpaceFeatures = getFeatures(ScreenSpaceFeatures.class);
            screenSpaceFeatures.setVertexBuffer(displayPassVertexBuffer);
            screenSpaceFeatures.setIndexBuffer(displayPassIndexBuffer);




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
        displayPass = Pass.newGraphicsPass(renderGraph, "Compose", renderer.getMaxFramesInFlight());
        {
            displayPass.addDependencies(

                    new Dependency(
                            "InputColorTextures",
                            lightingPassColorTextures,
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
                lightingPass,
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



        for(Entity entity : scene.getEntities()) {
            if(entity.has(SpotlightComponent.class)) {
                SpotlightComponent spotlightComponent = entity.getComponent(SpotlightComponent.class);

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
                sceneDescData.putFloat(-1);
            }
        }


    }


    @Override
    public void render(Renderer renderer) {
        Scene scene = getFeatures(SceneFeatures.class).getScene();




        if(displayPassRT != renderer.getSwapchainRenderTarget()) {
            displayPassRT = renderer.getSwapchainRenderTarget();
            RenderTargetAttachment colorAttachment = displayPassRT.getAttachment(RenderTargetAttachmentTypes.Color);
            displayPassColorTextures = new Resource<>(colorAttachment.getTextures());

            displayPass.getDependency(
                    "SwapchainColorTextures"
            ).setDependency(displayPassColorTextures);

            displayPass.getDependency(
                    "SwapchainColorTexturesPresent"
            ).setDependency(displayPassColorTextures);

        }

        for(Entity entity : scene.getEntities()) {
            if(entity.has(CameraComponent.class)) {
                CameraComponent cameraComponent = entity.getComponent(CameraComponent.class);
                sceneCamera = cameraComponent.camera;
            }
        }



        //ShadowMapGen and ShadowMap pass Dynamic Resource Dependencies
        {

            lightCount = 0;
            for(Entity entity : scene.getEntities()) {
                if (entity.has(SpotlightComponent.class))
                    lightCount++;
            }


            Texture[] shadowMapTextures = new Texture[lightCount * renderer.getMaxFramesInFlight()];
            Sampler[] shadowMapSamplers = new Sampler[shadowMapTextures.length];

            int lightIndex = 0;

            for(Entity entity : scene.getEntities()) {
                if (entity.has(SpotlightComponent.class)) {
                    SpotlightComponent spotlightComponent = entity.getComponent(SpotlightComponent.class);
                    int i1 = renderer.getMaxFramesInFlight() * lightIndex;
                    int i2 = renderer.getMaxFramesInFlight() * lightIndex + 1;

                    shadowMapTextures[i1] = spotlightComponent.renderTarget
                            .getAttachment(RenderTargetAttachmentTypes.Pos)
                            .getTextures()[0];

                    shadowMapSamplers[i1] = spotlightComponent.renderTarget
                            .getAttachment(RenderTargetAttachmentTypes.Pos)
                            .getSamplers()[0];

                    shadowMapTextures[i2] = spotlightComponent.renderTarget
                            .getAttachment(RenderTargetAttachmentTypes.Pos)
                            .getTextures()[1];

                    shadowMapSamplers[i2] = spotlightComponent.renderTarget
                            .getAttachment(RenderTargetAttachmentTypes.Pos)
                            .getSamplers()[1];

                    lightIndex++;
                }
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
                for(Entity entity : scene.getEntities()) {
                    if(entity.has(TransformComponent.class)) {
                        TransformComponent transformComponent = entity.getComponent(TransformComponent.class);

                        if(entity.has(EnvironmentMeshComponent.class)) {
                            EnvironmentMeshComponent environmentMeshComponent = entity.getComponent(EnvironmentMeshComponent.class);
                            ByteBuffer transformsData = environmentMeshComponent.transformsBuffers[renderer.getFrameIndex()].get();
                            transformComponent.transform().get(transformComponent.transformIndex() * SizeUtil.MATRIX_SIZE_BYTES, transformsData);
                            ByteBuffer sceneDescData = environmentMeshComponent.sceneDescBuffers[renderer.getFrameIndex()].get();
                            updateSceneDesc(sceneDescData, sceneCamera, scene);
                        }
                        if(entity.has(ActorMeshComponent.class)) {
                            ActorMeshComponent actorMeshComponent = entity.getComponent(ActorMeshComponent.class);
                            ByteBuffer transformsData = actorMeshComponent.transformsBuffers[renderer.getFrameIndex()].get();
                            transformComponent.transform().get(0, transformsData);
                            ByteBuffer sceneDescData = actorMeshComponent.sceneDescBuffers[renderer.getFrameIndex()].get();
                            updateSceneDesc(sceneDescData, sceneCamera, scene);
                        }


                    }
                }

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

                    for(Entity entity : scene.getEntities()) {
                        if(entity.has(SpotlightComponent.class)) {
                            SpotlightComponent spotlightComponent = entity.getComponent(SpotlightComponent.class);



                            int width, height;
                            {
                                Texture texture = spotlightComponent.renderTarget.getAttachmentByIndex(0).getTextures()[0];
                                width = texture.getWidth();
                                height = texture.getHeight();
                            }


                            shadowMapGenPass.startRendering(spotlightComponent.renderTarget, 2, width, height, true, Color.BLACK);
                            {
                                shadowMapGenPass.setCullMode(CullMode.Front);

                                for(Entity e : scene.getEntities()) {

                                    if (e.has(EnvironmentMeshComponent.class)) {
                                        EnvironmentMeshComponent environmentMeshComponent = e.getComponent(EnvironmentMeshComponent.class);
                                        shadowMapGenPass.setDrawBuffers(
                                                environmentMeshComponent.vertexBuffer,
                                                environmentMeshComponent.indexBuffer
                                        );
                                        shadowMapGenPass.setShaderProgram(
                                                environmentMeshComponent.shaderProgram
                                        );
                                        try (MemoryStack stack = stackPush()) {
                                            ByteBuffer pPushConstants = stack.calloc(2 * Integer.BYTES);
                                            pPushConstants.putInt(mode);
                                            pPushConstants.putInt(shadowMapGenPassLightIndex);
                                            shadowMapGenPass.setPushConstants(pPushConstants);
                                        }
                                        shadowMapGenPass.drawIndexed(environmentMeshComponent.indexCount);
                                    }
                                    if (e.has(ActorMeshComponent.class)) {
                                        ActorMeshComponent actorMeshComponent = e.getComponent(ActorMeshComponent.class);
                                        shadowMapGenPass.setDrawBuffers(
                                                actorMeshComponent.vertexBuffer,
                                                actorMeshComponent.indexBuffer
                                        );
                                        shadowMapGenPass.setShaderProgram(
                                                actorMeshComponent.shaderProgram
                                        );
                                        try (MemoryStack stack = stackPush()) {
                                            ByteBuffer pPushConstants = stack.calloc(2 * Integer.BYTES);
                                            pPushConstants.putInt(mode);
                                            pPushConstants.putInt(shadowMapGenPassLightIndex);
                                            shadowMapGenPass.setPushConstants(pPushConstants);
                                        }
                                        shadowMapGenPass.drawIndexed(actorMeshComponent.indexCount);
                                    }
                                }

                            }
                            shadowMapGenPass.endRendering();
                            shadowMapGenPassLightIndex++;
                        }


                    }




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
                        scenePass.setCullMode(CullMode.Back);
                        for(Entity entity : scene.getEntities()) {
                            if (entity.has(EnvironmentMeshComponent.class)) {
                                EnvironmentMeshComponent environmentMeshComponent = entity.getComponent(EnvironmentMeshComponent.class);

                                scenePass.setDrawBuffers(
                                        environmentMeshComponent.vertexBuffer,
                                        environmentMeshComponent.indexBuffer
                                );
                                scenePass.setShaderProgram(
                                        environmentMeshComponent.shaderProgram
                                );
                                try (MemoryStack stack = stackPush()) {
                                    ByteBuffer pPushConstants = stack.calloc(2 * Integer.BYTES);
                                    pPushConstants.putInt(mode);
                                    pPushConstants.putInt(-1);
                                    scenePass.setPushConstants(pPushConstants);
                                }
                                scenePass.drawIndexed(environmentMeshComponent.indexCount);
                            }
                            if(entity.has(ActorMeshComponent.class)) {
                                ActorMeshComponent actorMeshComponent = entity.getComponent(ActorMeshComponent.class);
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
                            }

                        }
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
                            "scene_desc",
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
                            "input_color_texture",
                            inputColorTexturesDependency.get().key[renderer.getFrameIndex()]
                    ),
                    new DescriptorUpdate<>(
                            "input_pos_vs_texture",
                            inputPosTexturesDependency.get().key[renderer.getFrameIndex()]
                    ),
                    new DescriptorUpdate<>(
                            "input_normal_ws_texture",
                            inputNormalTexturesDependency.get().key[renderer.getFrameIndex()]
                    ),
                    new DescriptorUpdate<>(
                            "output_color_texture",
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
                                    "input_shadow_maps",
                                    shadowMapTexturesResource.get().key[renderer.getMaxFramesInFlight() * i + renderer.getFrameIndex()]
                            ).arrayIndex(i)
                    );
                    lightingPassShaderProgram.setSamplers(
                            renderer.getFrameIndex(),
                            new DescriptorUpdate<>(
                                    "input_shadow_maps_samplers",
                                    shadowMapTexturesResource.get().value[renderer.getMaxFramesInFlight() * i + renderer.getFrameIndex()]
                            ).arrayIndex(i)
                    );

                }
            }

            lightingPass.startRecording(renderer.getFrameIndex());
            {
                lightingPass.resolveBarriers();
                lightingPass.setShaderProgram(lightingPassShaderProgram);

                try(MemoryStack stack = stackPush()) {
                    ByteBuffer pPushConstants = stack.calloc(Integer.BYTES);
                    pPushConstants.putInt(lightCount);
                    lightingPass.setPushConstants(pPushConstants);
                }

                int groupCountX = (int) Math.ceil((float) renderer.getWidth() / COMPUTE_THREAD_GROUP_SIZE);
                int groupCountY = (int) Math.ceil((float) renderer.getHeight() / COMPUTE_THREAD_GROUP_SIZE);

                lightingPass.dispatch(groupCountX, groupCountY, 1);
            }
            lightingPass.endRecording();
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
                    displayPass.setDrawBuffers(displayPassVertexBuffer, displayPassIndexBuffer);
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
