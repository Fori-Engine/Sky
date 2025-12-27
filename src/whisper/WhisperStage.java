package whisper;

import dev.dominion.ecs.api.Entity;
import fori.*;
import fori.asset.AssetPack;
import fori.asset.AssetPacks;

import fori.graphics.*;

import fori.ecs.*;
import fori.physx.ActorType;
import fori.physx.BoxCollider;
import fori.physx.Material;
import org.apache.commons.cli.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.File;
import java.lang.Math;
import java.util.*;

import static fori.graphics.ShaderRes.ShaderStage.*;
import static fori.graphics.VertexAttributes.Type.*;
import static fori.graphics.ShaderRes.Type.*;

public class WhisperStage extends Stage {

    private float startTime;
    private Scene scene;
    private Entity shopEntity;
    private Entity cameraEntity;
    private Entity playerEntity;
    private Entity levelEntity;
    private Entity spotlightEntity;
    private Renderer renderer;

    float x = 0, y = 6, z = -0.5f;




    public void init(String[] cliArgs, Surface surface){
        super.init(cliArgs, surface);


        Options options = new Options();
        {


            Option vsyncOption = new Option("vsync", true, "Enable or disable VSync");
            {
                vsyncOption.setRequired(false);
                options.addOption(vsyncOption);
            }
            Option validationOption = new Option("validation", true, "Enable or disable graphics API validation");
            {
                validationOption.setRequired(false);
                options.addOption(validationOption);
            }
            Option logDstOption = new Option("logdst", true, "Configure the destination file of logger output");
            {
                logDstOption.setRequired(false);
                options.addOption(logDstOption);
            }
        }

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, cliArgs);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Fori", options);
            System.exit(1);
        }

        boolean vsync = Boolean.parseBoolean(Objects.requireNonNullElse(cmd.getOptionValue("vsync"), "true"));
        boolean validation = Boolean.parseBoolean(Objects.requireNonNullElse(cmd.getOptionValue("validation"), "false"));
        String logDstPath = cmd.getOptionValue("logdst");


        AssetPacks.open("core", AssetPack.openLocal(new File("assets")));


        surface.display();

        renderer = Renderer.newRenderer(
                surface,
                surface.getWidth(),
                surface.getHeight(),
                new RendererSettings(RenderAPI.Vulkan)
                        .validation(validation)
                        .vsync(vsync)
        );

        scene = new Scene("Main_Scene");
        scene.addSystem(new RenderSystem(renderer, scene, new DefaultRenderPipelineImpl()));
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








        //Shop
        {
            ShaderProgram shaderProgram;
            Mesh mesh;

            ShaderReader.ShaderSources shaderSources = ShaderReader.read(
                    AssetPacks.<String>getAsset("core:assets/shaders/vulkan/Default.glsl").asset
            );


            shaderProgram = ShaderProgram.newGraphicsShaderProgram(renderer, 2);
            shaderProgram.addShader(
                    ShaderType.Vertex,
                    Shader.newShader(shaderProgram, ShaderType.Vertex, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Vertex), ShaderType.Vertex))
                            .setVertexAttributes(
                                    new VertexAttributes.Type[]{
                                            PositionFloat3,
                                            TransformIndexFloat1,
                                            UVFloat2,
                                    }
                            )

            );
            shaderProgram.addShader(
                    ShaderType.Fragment,
                    Shader.newShader(shaderProgram, ShaderType.Fragment, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Fragment), ShaderType.Fragment))
                            .setAttachmentTextureFormatTypes(TextureFormatType.ColorR32G32B32A32, TextureFormatType.ColorR32G32B32A32)
                            .setDepthAttachmentTextureFormatType(TextureFormatType.Depth32)
            );


            shaderProgram.bind(
                    new ShaderResSet(
                            0,
                            new ShaderRes(
                                    "sceneDesc",
                                    0,
                                    ShaderStorageBuffer,
                                    VertexStage
                            ).sizeBytes(SizeUtil.SCENE_DESC_SIZE_BYTES),
                            new ShaderRes(
                                    "transforms",
                                    1,
                                    ShaderStorageBuffer,
                                    VertexStage
                            ).sizeBytes(1 * SizeUtil.MATRIX_SIZE_BYTES),
                            new ShaderRes(
                                    "textures",
                                    2,
                                    CombinedSampler,
                                    FragmentStage
                            ).count(1)
                    )
            );


            mesh = Mesh.newMesh(shaderProgram.getShaderMap().get(ShaderType.Vertex).getVertexAttributes(), AssetPacks.getAsset("core:assets/models/viking_room.obj"));

            EnvironmentMeshComponent environmentMeshComponent = new EnvironmentMeshComponent(renderer, renderer, 100000, 100000, 1, shaderProgram);
            environmentMeshComponent.addMesh(mesh, new MeshUploaderWithTransform(0));
            environmentMeshComponent.close();

            Texture texture = Texture.newColorTextureFromAsset(renderer, AssetPacks.getAsset("core:assets/textures/viking_room.png"), TextureFormatType.ColorR8G8B8A8, Texture.Filter.Linear, Texture.Filter.Linear);


            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                environmentMeshComponent.shaderProgram.updateTextures(frameIndex, new ShaderUpdate<>("textures", 0, 2, texture).arrayIndex(0));

                environmentMeshComponent.shaderProgram.updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("sceneDesc", 0, 0, environmentMeshComponent.sceneDescBuffers[frameIndex]),
                        new ShaderUpdate<>("transforms", 0, 1, environmentMeshComponent.transformsBuffers[frameIndex])
                );
            }


            shopEntity = scene.createEntity(
                    environmentMeshComponent,
                    new ShaderComponent(shaderProgram),
                    new TransformComponent(0, new Matrix4f().identity().translate(1, 0, 0).rotate((float) Math.toRadians(-90), 1.0f, 0.0f, 0.0f)),
                    new NVPhysXComponent(new BoxCollider(1.5f, 1.5f, 1.5f), new Material(0.05f, 0.05f, 0.99f), ActorType.Dynamic)
            );


        }









        //Player
        {


            ShaderProgram shaderProgram;
            {
                ShaderReader.ShaderSources shaderSources = ShaderReader.read(
                        AssetPacks.<String>getAsset("core:assets/shaders/vulkan/PhysXTest.glsl").asset
                );


                shaderProgram = ShaderProgram.newGraphicsShaderProgram(renderer, 2);
                shaderProgram.addShader(
                        ShaderType.Vertex,
                        Shader.newShader(shaderProgram, ShaderType.Vertex, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Vertex), ShaderType.Vertex))
                                .setVertexAttributes(
                                        new VertexAttributes.Type[]{
                                        PositionFloat3,
                                        TransformIndexFloat1,
                                        ColorFloat4
                                })
                );
                shaderProgram.addShader(
                        ShaderType.Fragment,
                        Shader.newShader(shaderProgram, ShaderType.Fragment, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Fragment), ShaderType.Fragment))
                                .setAttachmentTextureFormatTypes(TextureFormatType.ColorR32G32B32A32, TextureFormatType.ColorR32G32B32A32)
                                .setDepthAttachmentTextureFormatType(TextureFormatType.Depth32)
                );

                shaderProgram.bind(
                        new ShaderResSet(
                                0,
                                new ShaderRes(
                                        "sceneDesc",
                                        0,
                                        ShaderStorageBuffer,
                                        AllStages
                                ).sizeBytes(SizeUtil.SCENE_DESC_SIZE_BYTES),
                                new ShaderRes(
                                        "transforms",
                                        1,
                                        ShaderStorageBuffer,
                                        VertexStage
                                ).sizeBytes(1 * SizeUtil.MATRIX_SIZE_BYTES)
                        )
                );

            }


            Mesh mesh = MeshGenerator.newBox(1.0f, 1.0f, 1.0f);

            ActorMeshComponent actorMeshComponent = new ActorMeshComponent(renderer, renderer, 100000, 100000, shaderProgram);
            actorMeshComponent.setMesh(mesh, new MeshUploaderWithTransform(0));


            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                actorMeshComponent.shaderProgram.updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("sceneDesc", 0, 0, actorMeshComponent.sceneDescBuffers[frameIndex]),
                        new ShaderUpdate<>("transforms", 0, 1, actorMeshComponent.transformsBuffers[frameIndex])
                );
            }

            playerEntity = scene.createEntity(
                    actorMeshComponent,
                    new ShaderComponent(shaderProgram),
                    new TransformComponent(new Matrix4f().identity().translate(-1, 10, 0).rotate((float) Math.toRadians(45.0f), 1, 0, 1)),
                    new NVPhysXComponent(new BoxCollider(1.0f, 1.0f, 1.0f), new Material(0.05f, 0.05f, 0.99f), ActorType.Dynamic)
            );
        }



        //Spotlight
        {
            RenderTarget lightRT = new RenderTarget(renderer);
            lightRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Color,
                            new Texture[]{
                                    Texture.newColorTexture(
                                            lightRT,
                                            1920,
                                            1080,
                                            TextureFormatType.ColorR32G32B32A32,
                                            Texture.Filter.Nearest,
                                            Texture.Filter.Nearest
                                    ),
                                    Texture.newColorTexture(
                                            lightRT,
                                            1920,
                                            1080,
                                            TextureFormatType.ColorR32G32B32A32,
                                            Texture.Filter.Nearest,
                                            Texture.Filter.Nearest
                                    )
                            }
                    )
            );
            lightRT.addAttachment(
                    new RenderTargetAttachment(
                            RenderTargetAttachmentTypes.Pos,
                            new Texture[]{
                                    Texture.newColorTexture(
                                            lightRT,
                                            1920,
                                            1080,
                                            TextureFormatType.ColorR32G32B32A32,
                                            Texture.Filter.Nearest,
                                            Texture.Filter.Nearest
                                    ),
                                    Texture.newColorTexture(
                                            lightRT,
                                            1920,
                                            1080,
                                            TextureFormatType.ColorR32G32B32A32,
                                            Texture.Filter.Nearest,
                                            Texture.Filter.Nearest
                                    )
                            }
                    )
            );
            lightRT.addAttachment(
                    new RenderTargetAttachment(RenderTargetAttachmentTypes.Depth, new Texture[]{
                            Texture.newDepthTexture(lightRT, 1920, 1080, TextureFormatType.Depth32, Texture.Filter.Nearest, Texture.Filter.Nearest)
                    })
            );



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
                    ),
                    new ScriptComponent(new Script() {
                        @Override
                        public void init(Entity entity) {

                        }

                        @Override
                        public void update(Entity entity) {
                            LightComponent lightComponent = entity.get(LightComponent.class);
                            if(surface.getKeyPressed(Input.KEY_W)) z -= 1 * Time.deltaTime();
                            if(surface.getKeyPressed(Input.KEY_A)) x -= 1 * Time.deltaTime();
                            if(surface.getKeyPressed(Input.KEY_S)) z += 1 * Time.deltaTime();
                            if(surface.getKeyPressed(Input.KEY_D)) x += 1 * Time.deltaTime();

                            lightComponent.setView(new Matrix4f().lookAt(
                                    new Vector3f(x, y, z),
                                    new Vector3f(0, 0, 0),
                                    new Vector3f(0.0f, 1.0f, 0.0f)
                            ));

                        }
                    })
            );


        }

        //Level
        {


            ShaderProgram shaderProgram;
            {
                ShaderReader.ShaderSources shaderSources = ShaderReader.read(
                        AssetPacks.<String>getAsset("core:assets/shaders/vulkan/PhysXTest.glsl").asset
                );


                shaderProgram = ShaderProgram.newGraphicsShaderProgram(renderer, 2);
                shaderProgram.addShader(
                        ShaderType.Vertex,
                        Shader.newShader(shaderProgram, ShaderType.Vertex, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Vertex), ShaderType.Vertex))
                                .setVertexAttributes(new VertexAttributes.Type[]{
                                        PositionFloat3,
                                        TransformIndexFloat1,
                                        ColorFloat4
                                })
                );
                shaderProgram.addShader(
                        ShaderType.Fragment,
                        Shader.newShader(shaderProgram, ShaderType.Fragment, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Fragment), ShaderType.Fragment))
                                .setAttachmentTextureFormatTypes(TextureFormatType.ColorR32G32B32A32, TextureFormatType.ColorR32G32B32A32)
                                .setDepthAttachmentTextureFormatType(TextureFormatType.Depth32)
                );

                shaderProgram.bind(
                        new ShaderResSet(
                                0,
                                new ShaderRes(
                                        "sceneDesc",
                                        0,
                                        ShaderStorageBuffer,
                                        AllStages
                                ).sizeBytes(SizeUtil.SCENE_DESC_SIZE_BYTES),
                                new ShaderRes(
                                        "transforms",
                                        1,
                                        ShaderStorageBuffer,
                                        VertexStage
                                ).sizeBytes(1 * SizeUtil.MATRIX_SIZE_BYTES)
                        )
                );

            }


            Mesh mesh = MeshGenerator.newBox(10.0f, 1.0f, 10.0f);
            ActorMeshComponent actorMeshComponent = new ActorMeshComponent(renderer, renderer, 100000, 100000, shaderProgram);
            actorMeshComponent.setMesh(mesh, new MeshUploaderWithTransform(0));

            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                actorMeshComponent.shaderProgram.updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("sceneDesc", 0, 0, actorMeshComponent.sceneDescBuffers[frameIndex]),
                        new ShaderUpdate<>("transforms", 0, 1, actorMeshComponent.transformsBuffers[frameIndex])
                );
            }

            levelEntity = scene.createEntity(
                    actorMeshComponent,
                    new ShaderComponent(shaderProgram),
                    new TransformComponent(new Matrix4f().identity().translate(0, -2, 0).rotate((float) Math.toRadians(180), 0, 0, 1)),
                    new NVPhysXComponent(new BoxCollider(10f, 1f, 10f), new Material(0.05f, 0.05f, 0.99f), ActorType.Static)
            );
        }







        startTime = (float) surface.getTime();
    }


    public boolean update(){

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
