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
    private RenderTarget scenePassRT;
    private Texture[] sceneColorTextures;



    private ComputePass shadowMapPass;
    private RenderTarget shadowMapPassRT;
    private Texture[] shadowMapPassColorTextures;
    private ShaderProgram shadowMapPassShaderProgram;


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
            scenePassRT = new RenderTarget(renderer);
            sceneColorTextures = new Texture[]{
                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR8G8B8A8, Texture.Filter.Nearest, Texture.Filter.Nearest),
                    Texture.newColorTexture(scenePassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR8G8B8A8, Texture.Filter.Nearest, Texture.Filter.Nearest)
            };

            scenePassRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentTypes.Color, sceneColorTextures)
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
            shadowMapPassColorTextures = new Texture[]{
                    Texture.newStorageTexture(shadowMapPassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32, Texture.Filter.Nearest, Texture.Filter.Nearest),
                    Texture.newStorageTexture(shadowMapPassRT, renderer.getWidth(), renderer.getHeight(), TextureFormatType.ColorR32G32B32A32, Texture.Filter.Nearest, Texture.Filter.Nearest)
            };

            shadowMapPassRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentTypes.Color, shadowMapPassColorTextures)
            );

            ShaderReader.ShaderSources shaderSources = ShaderReader.read(
                    AssetPacks.<String>getAsset("core:assets/shaders/vulkan/ShadowPass.glsl").asset
            );

            shadowMapPassShaderProgram = ShaderProgram.newComputeShaderProgram(renderer, 1);
            shadowMapPassShaderProgram.addShader(
                    ShaderType.Compute,
                    Shader.newShader(shadowMapPassShaderProgram, ShaderType.Compute, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Compute), ShaderType.Compute))
            );


            shadowMapPassShaderProgram.bind(
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
                    )
            );

        }


        shadowMapPass = Pass.newComputePass(renderer, "Shadow", renderer.getMaxFramesInFlight());
        {
            shadowMapPass.setResourceDependencies(

                    new ResourceDependency<>(
                            "InputColorTextures",
                            sceneColorTextures,
                            ResourceDependencyTypes.ComputeShaderRead
                    ),
                    new ResourceDependency<>(
                            "OutputTextures",
                            shadowMapPassColorTextures,
                            ResourceDependencyTypes.ComputeShaderWrite
                    )
            );
        }
        swapchainPass = Pass.newGraphicsPass(renderGraph, "Swapchain", renderer.getMaxFramesInFlight());
        {
            swapchainPass.setResourceDependencies(

                    new ResourceDependency<>(
                            "InputTextures",
                            shadowMapPassColorTextures,
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
                shadowMapPass,
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

                /*
                Shader invocations only happen when the command buffer actually executes, not when it's being recorded lmao
                This means that during recording only the light transform gets sent to the shader because it's the last
                thing to get written during CB recording

                Push descriptors maybe?

                A proper solution is to actually like, create a pass entirely for generating shadow maps, shove
                EVERY SINGLE light transform into one SSBO and just:

                - Set light transform index via push constants
                - Draw
                - Repeat for all lights

                 */

                /*
                - Push constants data
                - camera/light pov ssbo { n view matrices, n proj matrices}
                - ShadowMapGenPass



                 */

                //Default Rendering (mode 0)
                {
                    scenePass.startRendering(scenePassRT, renderer.getWidth(), renderer.getHeight(), true, Color.BLACK);
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
                            scenePass.drawIndexed(staticMeshComponent.staticMeshBatch().getIndexCount(), new int[]{0});
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
                            scenePass.drawIndexed(dynamicMeshComponent.dynamicMesh().getIndexCount(), new int[]{0});
                        });
                    }
                    scenePass.endRendering();
                }

                //Shadow Map Gen (mode 1)
                /*
                {
                    scene.getEngine().findEntitiesWith(LightComponent.class).stream().forEach(lightEntityComponent -> {

                        int width, height;
                        {
                            Texture texture = lightEntityComponent.comp().renderTarget.getAttachmentByIndex(0).getTextures()[0];
                            width = texture.getWidth();
                            height = texture.getHeight();
                        }


                        scenePass.startRendering(lightEntityComponent.comp().renderTarget, width, height, true, Color.RED);
                        {


                            scene.getEngine().findEntitiesWith(TransformComponent.class, StaticMeshComponent.class).stream().forEach(components -> {


                                TransformComponent transformComponent = components.comp1();
                                StaticMeshComponent staticMeshComponent = components.comp2();

                                ByteBuffer transformsData = staticMeshComponent.staticMeshBatch().getTransformsBuffers()[renderer.getFrameIndex()].get();
                                transformComponent.transform().get(transformComponent.transformIndex() * SizeUtil.MATRIX_SIZE_BYTES, transformsData);
                                ByteBuffer cameraData = staticMeshComponent.staticMeshBatch().getCameraBuffers()[renderer.getFrameIndex()].get();


                                lightEntityComponent.comp().view.get(0, cameraData);
                                lightEntityComponent.comp().proj.get(SizeUtil.MATRIX_SIZE_BYTES, cameraData);


                                scenePass.setDrawBuffers(
                                        staticMeshComponent.staticMeshBatch().getVertexBuffer(),
                                        staticMeshComponent.staticMeshBatch().getIndexBuffer()
                                );
                                scenePass.setShaderProgram(
                                        staticMeshComponent.staticMeshBatch().getShaderProgram()
                                );
                                scenePass.drawIndexed(staticMeshComponent.staticMeshBatch().getIndexCount(), new int[]{1});
                            });
                            scene.getEngine().findEntitiesWith(TransformComponent.class, DynamicMeshComponent.class).stream().forEach(components -> {


                                TransformComponent transformComponent = components.comp1();
                                DynamicMeshComponent dynamicMeshComponent = components.comp2();

                                ByteBuffer transformsData = dynamicMeshComponent.dynamicMesh().getTransformsBuffers()[renderer.getFrameIndex()].get();
                                transformComponent.transform().get(0, transformsData);
                                ByteBuffer cameraData = dynamicMeshComponent.dynamicMesh().getCameraBuffers()[renderer.getFrameIndex()].get();


                                lightEntityComponent.comp().view.get(0, cameraData);
                                lightEntityComponent.comp().proj.get(SizeUtil.MATRIX_SIZE_BYTES, cameraData);

                                scenePass.setDrawBuffers(
                                        dynamicMeshComponent.dynamicMesh().getVertexBuffer(),
                                        dynamicMeshComponent.dynamicMesh().getIndexBuffer()
                                );
                                scenePass.setShaderProgram(
                                        dynamicMeshComponent.dynamicMesh().getShaderProgram()
                                );
                                scenePass.drawIndexed(dynamicMeshComponent.dynamicMesh().getIndexCount(), new int[]{1});
                            });
                        }
                        scenePass.endRendering();
                    });

                }

                 */

            }
            scenePass.endRecording();
        });



        shadowMapPass.setPassExecuteCallback(() -> {


            shadowMapPassShaderProgram.updateTextures(
                    renderer.getFrameIndex(),
                    new ShaderUpdate<>(
                            "inputColorTexture",
                            0,
                            0,
                            ((Texture[]) shadowMapPass.getResourceDependencyByNameAndType("InputColorTextures", ResourceDependencyTypes.ComputeShaderRead).getDependency())[renderer.getFrameIndex()]
                    )
            );


            shadowMapPassShaderProgram.updateTextures(
                    renderer.getFrameIndex(),
                    new ShaderUpdate<>(
                            "outputTexture",
                            0,
                            1,
                            ((Texture[]) shadowMapPass.getResourceDependencyByNameAndType("OutputTextures", ResourceDependencyTypes.ComputeShaderWrite).getDependency())[renderer.getFrameIndex()]
                    )
            );

            shadowMapPass.startRecording(renderer.getFrameIndex());
            {
                shadowMapPass.resolveBarriers();
                shadowMapPass.setShaderProgram(shadowMapPassShaderProgram);
                shadowMapPass.dispatch(1920, 1080, 1, new int[]{0});
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
                            ((Texture[]) swapchainPass.getResourceDependencyByNameAndType("InputTextures", ResourceDependencyTypes.FragmentShaderRead).getDependency())[renderer.getFrameIndex()])
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
