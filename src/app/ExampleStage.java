package app;

import dev.dominion.ecs.api.Entity;
import engine.*;
import engine.asset.AssetPackage;
import engine.asset.AssetRegistry;

import engine.graphics.*;

import engine.ecs.*;
import engine.graphics.pipelines.ComposeFeatures;
import engine.graphics.pipelines.DeferredPBRRenderPipeline;
import engine.graphics.pipelines.SceneFeatures;
import engine.physx.ActorType;
import engine.physx.BoxCollider;
import engine.physx.Material;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public class ExampleStage extends Stage {

    private float startTime;
    private Scene scene;
    private Entity modelEntity;
    private Entity cameraEntity;
    private Entity defaultCubeEntity;
    private Entity floorEntity;
    private Entity spotlightEntity;
    private RenderPipeline renderPipeline;
    private Renderer renderer;


    public void init(String[] cliArgs, Surface surface){
        super.init(cliArgs, surface);

        Path assetPackPath = Path.of("assets.pkg");

        AssetPackage.createPackage(assetPackPath, AssetPackage.openLocal("core", Path.of("assets")));
        AssetRegistry.addPackage(AssetPackage.openPackage("core", assetPackPath));


        surface.display();

        renderer = Renderer.newRenderer(
                surface,
                surface.getWidth(),
                surface.getHeight(),
                new RendererSettings(RenderAPI.Vulkan)
                        .validation(true)
                        .vsync(true)
        );
        renderPipeline = new DeferredPBRRenderPipeline();


        scene = new Scene("Example_Scene");
        scene.addSystem(new RenderSystem(renderer, renderPipeline));
        scene.addSystem(new NVPhysXSystem(scene, 4, 1f/60f));
        scene.addSystem(new ScriptSystem(scene));



        Camera camera = new Camera(
                new Matrix4f().lookAt(
                        new Vector3f(0.0f, 6.0f, 0.5f),
                        new Vector3f(0, 0, 0),
                        new Vector3f(0.0f, 1.0f, 0.0f)
                ),
                new Matrix4f().perspective(
                        (float) Math.toRadians(45.0f),
                        (float) renderer.getWidth() / renderer.getHeight(),
                        0.01f,
                        100.0f,
                        true
                ),
                true
        );

        //Camera
        cameraEntity = scene.createEntity(new CameraComponent(camera));


        //Default Cube
        {


            ShaderProgram shaderProgram = ShaderProgram.newShaderProgram(renderer);
            shaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/deferred_pbr_pipeline/Default_vertex.spv"), ShaderType.VertexShader);
            shaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/deferred_pbr_pipeline/Default_fragment.spv"), ShaderType.FragmentShader);
            shaderProgram.assemble();



            MeshData meshData = MeshGenerator.newBox(1.0f, 1.0f, 1.0f);




            ActorMeshComponent actorMeshComponent = new ActorMeshComponent(renderer, renderer, 100000, 100000, shaderProgram);
            actorMeshComponent.setMesh(meshData, new EntityShaderIndex(0));




            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                actorMeshComponent.shaderProgram.setBuffers(
                        frameIndex,
                        new DescriptorUpdate<>("sceneDesc", actorMeshComponent.sceneDescBuffers[frameIndex]),
                        new DescriptorUpdate<>("transforms", actorMeshComponent.transformsBuffers[frameIndex])
                );
            }

            defaultCubeEntity = scene.createEntity(
                    actorMeshComponent,
                    new ShaderComponent(shaderProgram),
                    new TransformComponent(new Matrix4f().identity().translate(0.2f, 10, 0).rotate((float) Math.toRadians(45.0f), 1, 0, 1)),
                    new NVPhysXComponent(new BoxCollider(1.0f, 1.0f, 1.0f), new Material(0.5f, 0.5f, 0.3f), ActorType.Dynamic)
            );
        }

        //Default Cube
        {


            ShaderProgram shaderProgram = ShaderProgram.newShaderProgram(renderer);
            shaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/deferred_pbr_pipeline/Default_vertex.spv"), ShaderType.VertexShader);
            shaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/deferred_pbr_pipeline/Default_fragment.spv"), ShaderType.FragmentShader);
            shaderProgram.assemble();



            MeshData meshData = MeshGenerator.newBox(1.0f, 1.0f, 1.0f);




            ActorMeshComponent actorMeshComponent = new ActorMeshComponent(renderer, renderer, 100000, 100000, shaderProgram);
            actorMeshComponent.setMesh(meshData, new EntityShaderIndex(0));




            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                actorMeshComponent.shaderProgram.setBuffers(
                        frameIndex,
                        new DescriptorUpdate<>("sceneDesc", actorMeshComponent.sceneDescBuffers[frameIndex]),
                        new DescriptorUpdate<>("transforms", actorMeshComponent.transformsBuffers[frameIndex])
                );
            }

            defaultCubeEntity = scene.createEntity(
                    actorMeshComponent,
                    new ShaderComponent(shaderProgram),
                    new TransformComponent(new Matrix4f().identity().translate(0.1f, 7, 0).rotate((float) Math.toRadians(45.0f), 1, 0, 1)),
                    new NVPhysXComponent(new BoxCollider(1.0f, 1.0f, 1.0f), new Material(0.5f, 0.5f, 0.3f), ActorType.Dynamic)
            );
        }




        //Floor Cube
        {


            ShaderProgram shaderProgram = ShaderProgram.newShaderProgram(renderer);
            shaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/deferred_pbr_pipeline/Default2_vertex.spv"), ShaderType.VertexShader);
            shaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/deferred_pbr_pipeline/Default2_fragment.spv"), ShaderType.FragmentShader);
            shaderProgram.assemble();



            MeshData meshData = MeshGenerator.newPlane(10, 10);

            ActorMeshComponent actorMeshComponent = new ActorMeshComponent(renderer, renderer, 100000, 100000, shaderProgram);
            actorMeshComponent.setMesh(meshData, new EntityShaderIndex(0));

            Texture texture = Texture.newColorTextureFromAsset(renderer, AssetRegistry.getAsset("core:assets/textures/woodfloor.png"), TextureFormatType.ColorR8G8B8A8);
            Sampler sampler = Sampler.newSampler(texture, Texture.Filter.Linear, Texture.Filter.Linear, true);



            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                actorMeshComponent.shaderProgram.setTextures(frameIndex, new DescriptorUpdate<>("texture", texture));
                actorMeshComponent.shaderProgram.setSamplers(frameIndex, new DescriptorUpdate<>("textureSampler", sampler));

                actorMeshComponent.shaderProgram.setBuffers(
                        frameIndex,
                        new DescriptorUpdate<>("sceneDesc", actorMeshComponent.sceneDescBuffers[frameIndex]),
                        new DescriptorUpdate<>("transforms", actorMeshComponent.transformsBuffers[frameIndex])
                );
            }

            floorEntity = scene.createEntity(
                    actorMeshComponent,
                    new ShaderComponent(shaderProgram),
                    new TransformComponent(new Matrix4f().identity().translate(0, -2, 0).rotate((float) Math.toRadians(0), 0, 0, 1)),
                    new NVPhysXComponent(new BoxCollider(10.0f, 1.0f, 10.0f), new Material(0.5f, 0.5f, 0.3f), ActorType.Static)
            );
        }






        //Spotlight
        {
            RenderTarget lightRT = new RenderTarget(renderer);

            Texture[] posTextures = new Texture[] {
                    Texture.newColorTexture(
                            lightRT,
                            1920 / 4,
                            1080 / 4,
                            TextureFormatType.ColorR32G32B32A32
                    ),
                    Texture.newColorTexture(
                            lightRT,
                            1920 / 4,
                            1080 / 4,
                            TextureFormatType.ColorR32G32B32A32
                    )
            };



            lightRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Pos,
                            posTextures,
                            new Sampler[]{
                                    Sampler.newSampler(lightRT, Texture.Filter.Linear, Texture.Filter.Linear, true),
                                    Sampler.newSampler(lightRT, Texture.Filter.Linear, Texture.Filter.Linear, true)
                            }

                    )
            );
            lightRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentTypes.Depth, new Texture[]{
                            Texture.newDepthTexture(lightRT, 1920 / 4, 1080 / 4, TextureFormatType.Depth32)
                    }, null)
            );


            float x = 0, y = 6, z = -0.5f;


            spotlightEntity = scene.createEntity(
                    new LightComponent(
                            new Matrix4f().lookAt(
                                    new Vector3f(x, y, z),
                                    new Vector3f(0, 0, 0),
                                    new Vector3f(0.0f, 1.0f, 0.0f)
                            ),
                            new Matrix4f().perspective(
                                    (float) Math.toRadians(45),
                                    (float) 1920 / 1080,
                                    0.01f,
                                    100.0f,
                                    true
                            ),

                            true,
                            lightRT
                    )
            );


        }


        startTime = (float) surface.getTime();
    }


    public boolean update(){
        renderPipeline.getFeatures(SceneFeatures.class).setScene(scene);

        //UI Rendering
        {
            ComposeFeatures composeFeatures = renderPipeline.getFeatures(ComposeFeatures.class);
            ByteBuffer vertexBufferData = composeFeatures.getVertexBuffer().get();
            vertexBufferData.clear();
            ByteBuffer indexBufferData = composeFeatures.getIndexBuffer().get();
            indexBufferData.clear();

            int quadIndex = 0;

            {
                float x = 0, y = 0, w = 1920, h = 1080;

                vertexBufferData.putFloat(x);
                vertexBufferData.putFloat(y);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(0);
                vertexBufferData.putFloat(0);
                vertexBufferData.putFloat(0);

                vertexBufferData.putFloat(x);
                vertexBufferData.putFloat(y + h);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(0);
                vertexBufferData.putFloat(1);
                vertexBufferData.putFloat(0);


                vertexBufferData.putFloat(x + w);
                vertexBufferData.putFloat(y + h);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1);
                vertexBufferData.putFloat(1);
                vertexBufferData.putFloat(0);


                vertexBufferData.putFloat(x + w);
                vertexBufferData.putFloat(y);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1);
                vertexBufferData.putFloat(0);
                vertexBufferData.putFloat(0);


                indexBufferData.putInt(0 + (4 * quadIndex));
                indexBufferData.putInt(1 + (4 * quadIndex));
                indexBufferData.putInt(2 + (4 * quadIndex));
                indexBufferData.putInt(2 + (4 * quadIndex));
                indexBufferData.putInt(3 + (4 * quadIndex));
                indexBufferData.putInt(0 + (4 * quadIndex));
                quadIndex++;
            }

            {
                float x = 100, y = 600, w = 300, h = 300;

                vertexBufferData.putFloat(x);
                vertexBufferData.putFloat(y);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(0);
                vertexBufferData.putFloat(0);
                vertexBufferData.putFloat(-1);

                vertexBufferData.putFloat(x);
                vertexBufferData.putFloat(y + h);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(0);
                vertexBufferData.putFloat(1);
                vertexBufferData.putFloat(-1);


                vertexBufferData.putFloat(x + w);
                vertexBufferData.putFloat(y + h);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1);
                vertexBufferData.putFloat(1);
                vertexBufferData.putFloat(-1);


                vertexBufferData.putFloat(x + w);
                vertexBufferData.putFloat(y);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1);
                vertexBufferData.putFloat(0);
                vertexBufferData.putFloat(-1);


                indexBufferData.putInt(0 + (4 * quadIndex));
                indexBufferData.putInt(1 + (4 * quadIndex));
                indexBufferData.putInt(2 + (4 * quadIndex));
                indexBufferData.putInt(2 + (4 * quadIndex));
                indexBufferData.putInt(3 + (4 * quadIndex));
                indexBufferData.putInt(0 + (4 * quadIndex));
                quadIndex++;
            }

            {
                float x = 1200, y = 600, w = 300, h = 300;

                vertexBufferData.putFloat(x);
                vertexBufferData.putFloat(y);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(0);
                vertexBufferData.putFloat(0);
                vertexBufferData.putFloat(-2);

                vertexBufferData.putFloat(x);
                vertexBufferData.putFloat(y + h);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(0);
                vertexBufferData.putFloat(1);
                vertexBufferData.putFloat(-2);


                vertexBufferData.putFloat(x + w);
                vertexBufferData.putFloat(y + h);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1);
                vertexBufferData.putFloat(1);
                vertexBufferData.putFloat(-2);


                vertexBufferData.putFloat(x + w);
                vertexBufferData.putFloat(y);
                vertexBufferData.putFloat(0f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1f);
                vertexBufferData.putFloat(1);
                vertexBufferData.putFloat(0);
                vertexBufferData.putFloat(-2);


                indexBufferData.putInt(0 + (4 * quadIndex));
                indexBufferData.putInt(1 + (4 * quadIndex));
                indexBufferData.putInt(2 + (4 * quadIndex));
                indexBufferData.putInt(2 + (4 * quadIndex));
                indexBufferData.putInt(3 + (4 * quadIndex));
                indexBufferData.putInt(0 + (4 * quadIndex));
                quadIndex++;
            }
        }

        renderer.updateRenderer(surface.update());
        scene.tick();



        Time.deltaTime = (float) (surface.getTime() - startTime);
        startTime = (float) surface.getTime();

        return !surface.shouldClose();
    }

    @Override
    public void closing() {


        //Explicitly free all Actors and Environments
        scene.getEngine().findEntitiesWith(ActorMeshComponent.class).stream().forEach(components -> {
            ActorMeshComponent actorMeshComponent = components.comp();

            for(int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                Buffer sceneDescBuffer = actorMeshComponent.sceneDescBuffers[frameIndex];
                Buffer transformsBuffer = actorMeshComponent.transformsBuffers[frameIndex];

                sceneDescBuffer.disposeAll();
                transformsBuffer.disposeAll();

                renderer.remove(sceneDescBuffer);
                renderer.remove(transformsBuffer);
            }


            actorMeshComponent.vertexBuffer.disposeAll();
            actorMeshComponent.indexBuffer.disposeAll();
            actorMeshComponent.shaderProgram.disposeAll();

            renderer.remove(actorMeshComponent.vertexBuffer);
            renderer.remove(actorMeshComponent.indexBuffer);
            renderer.remove(actorMeshComponent.shaderProgram);
        });

        scene.getEngine().findEntitiesWith(EnvironmentMeshComponent.class).stream().forEach(components -> {
            EnvironmentMeshComponent environmentMeshComponent = components.comp();

            for(int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                Buffer sceneDescBuffer = environmentMeshComponent.sceneDescBuffers[frameIndex];
                Buffer transformsBuffer = environmentMeshComponent.transformsBuffers[frameIndex];

                sceneDescBuffer.disposeAll();
                transformsBuffer.disposeAll();


                renderer.remove(sceneDescBuffer);
                renderer.remove(transformsBuffer);
            }


            environmentMeshComponent.vertexBuffer.disposeAll();
            environmentMeshComponent.indexBuffer.disposeAll();
            environmentMeshComponent.shaderProgram.disposeAll();

            renderer.remove(environmentMeshComponent.vertexBuffer);
            renderer.remove(environmentMeshComponent.indexBuffer);
            renderer.remove(environmentMeshComponent.shaderProgram);
        });




        scene.getEngine().findEntitiesWith(NVPhysXComponent.class).stream().forEach(components -> {
            NVPhysXComponent nvPhysXComponent = components.comp();
            nvPhysXComponent.release();
        });

        scene.close(renderer);
    }


    public void dispose(){

    }
}
