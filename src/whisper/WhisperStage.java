package whisper;

import dev.dominion.ecs.api.Entity;
import fori.*;
import fori.asset.AssetPack;
import fori.asset.AssetPacks;

import fori.graphics.*;

import fori.graphics.StaticMeshBatch;
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

import static fori.graphics.VertexAttributes.Type.*;
import static fori.graphics.ShaderRes.ShaderStage.FragmentStage;
import static fori.graphics.ShaderRes.ShaderStage.VertexStage;
import static fori.graphics.ShaderRes.Type.*;

public class WhisperStage extends Stage {

    private float startTime;
    private Scene scene;
    private Entity shopEntity;
    private Entity cameraEntity;
    private Entity playerEntity;
    private Entity levelEntity;
    private Renderer renderer;



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
        scene.addSystem(new RenderSystem(renderer, scene));
        scene.addSystem(new NVPhysXSystem(scene, 4, 1f/60f));
        scene.addSystem(new ScriptSystem(scene));



        Camera camera = new Camera(
                new Matrix4f().lookAt(
                        new Vector3f(0.0f, 0.0f, 6.0f),
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


            shaderProgram = ShaderProgram.newGraphicsShaderProgram(renderer, TextureFormatType.ColorR8G8B8A8StandardRGB, TextureFormatType.Depth32Float);
            shaderProgram.setShaders(
                    Shader.newShader(shaderProgram, ShaderType.Vertex, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Vertex), ShaderType.Vertex)),
                    Shader.newShader(shaderProgram, ShaderType.Fragment, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Fragment), ShaderType.Fragment))
            );

            shaderProgram.bind(
                    Optional.of(
                    new VertexAttributes.Type[]{
                            PositionFloat3,
                            TransformIndexFloat1,
                            UVFloat2,
                    }),
                    new ShaderResSet(
                            0,
                            new ShaderRes(
                                    "camera",
                                    0,
                                    UniformBuffer,
                                    VertexStage
                            ).sizeBytes(2 * SizeUtil.MATRIX_SIZE_BYTES),
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


            mesh = Mesh.newMesh(shaderProgram.getAttributes().get(), AssetPacks.getAsset("core:assets/models/viking_room.obj"));
            StaticMeshBatch shopStaticMeshBatch = renderer.newStaticMeshBatch(100000, 100000, 1, shaderProgram);

            shopStaticMeshBatch.submitMesh(mesh, new MeshUploaderWithTransform(0));
            shopStaticMeshBatch.finish();


            scene.registerStaticMeshBatch("Shops", shopStaticMeshBatch);

            Texture texture = Texture.newColorTextureFromAsset(renderer, AssetPacks.getAsset("core:assets/textures/viking_room.png"), TextureFormatType.ColorR8G8B8A8StandardRGB, Texture.Filter.Linear, Texture.Filter.Linear);


            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                shopStaticMeshBatch.getShaderProgram().updateTextures(frameIndex, new ShaderUpdate<>("textures", 0, 2, texture).arrayIndex(0));

                shopStaticMeshBatch.getShaderProgram().updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("camera", 0, 0, shopStaticMeshBatch.getCameraBuffers()[frameIndex]),
                        new ShaderUpdate<>("transforms", 0, 1, shopStaticMeshBatch.getTransformsBuffers()[frameIndex])
                );
            }


            shopEntity = scene.createEntity(
                    new StaticMeshComponent(shopStaticMeshBatch, mesh),
                    new ShaderComponent(shaderProgram),
                    new TransformComponent(0, new Matrix4f().identity()),
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


                shaderProgram = ShaderProgram.newGraphicsShaderProgram(renderer, TextureFormatType.ColorR8G8B8A8StandardRGB, TextureFormatType.Depth32Float);
                shaderProgram.setShaders(
                        Shader.newShader(shaderProgram, ShaderType.Vertex, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Vertex), ShaderType.Vertex)),
                        Shader.newShader(shaderProgram, ShaderType.Fragment, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Fragment), ShaderType.Fragment))
                );

                shaderProgram.bind(
                        Optional.of(
                        new VertexAttributes.Type[]{
                                PositionFloat3,
                                TransformIndexFloat1,
                                ColorFloat4
                        }),
                        new ShaderResSet(
                                0,
                                new ShaderRes(
                                        "camera",
                                        0,
                                        UniformBuffer,
                                        VertexStage
                                ).sizeBytes(2 * SizeUtil.MATRIX_SIZE_BYTES),
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
            DynamicMesh dynamicMesh = renderer.newDynamicMesh(100000, 100000, shaderProgram);
            dynamicMesh.submit(mesh, new MeshUploaderWithTransform(0));
            scene.registerDynamicMesh(dynamicMesh);


            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                dynamicMesh.getShaderProgram().updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("camera", 0, 0, dynamicMesh.getCameraBuffers()[frameIndex]),
                        new ShaderUpdate<>("transforms", 0, 1, dynamicMesh.getTransformsBuffers()[frameIndex])
                );
            }

            playerEntity = scene.createEntity(
                    new DynamicMeshComponent(dynamicMesh, mesh),
                    new ShaderComponent(shaderProgram),
                    new TransformComponent(new Matrix4f().identity().translate(0, 0, 1).rotate((float) Math.toRadians(45.0f), 1, 0, 1)),
                    new NVPhysXComponent(new BoxCollider(1.0f, 1.0f, 1.0f), new Material(0.05f, 0.05f, 0.99f), ActorType.Dynamic),
                    new ScriptComponent(new Script() {
                        @Override
                        public void init(Entity entity) {

                        }

                        @Override
                        public void update(Entity entity) {


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


                shaderProgram = ShaderProgram.newGraphicsShaderProgram(renderer, TextureFormatType.ColorR8G8B8A8StandardRGB, TextureFormatType.Depth32Float);
                shaderProgram.setShaders(
                        Shader.newShader(shaderProgram, ShaderType.Vertex, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Vertex), ShaderType.Vertex)),
                        Shader.newShader(shaderProgram, ShaderType.Fragment, ShaderCompiler.compile(shaderSources.getShaderSource(ShaderType.Fragment), ShaderType.Fragment))
                );


                shaderProgram.bind(
                        Optional.of(
                        new VertexAttributes.Type[]{
                                PositionFloat3,
                                TransformIndexFloat1,
                                ColorFloat4
                        }),
                        new ShaderResSet(
                                0,
                                new ShaderRes(
                                        "camera",
                                        0,
                                        UniformBuffer,
                                        VertexStage
                                ).sizeBytes(2 * SizeUtil.MATRIX_SIZE_BYTES),
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
            DynamicMesh dynamicMesh = renderer.newDynamicMesh(100000, 100000, shaderProgram);
            dynamicMesh.submit(mesh, new MeshUploaderWithTransform(0));
            scene.registerDynamicMesh(dynamicMesh);


            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                dynamicMesh.getShaderProgram().updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("camera", 0, 0, dynamicMesh.getCameraBuffers()[frameIndex]),
                        new ShaderUpdate<>("transforms", 0, 1, dynamicMesh.getTransformsBuffers()[frameIndex])
                );
            }

            levelEntity = scene.createEntity(
                    new DynamicMeshComponent(dynamicMesh, mesh),
                    new ShaderComponent(shaderProgram),
                    new TransformComponent(new Matrix4f().identity().translate(-5.3f, -2, 0).rotate((float) Math.toRadians(180), 0, 0, 1)),
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

        System.out.println(Time.framesPerSecond());


        return !surface.shouldClose();
    }

    @Override
    public void closing() {
        scene.getEngine().findEntitiesWith(NVPhysXComponent.class).stream().forEach(components -> {
            NVPhysXComponent nvPhysXComponent = components.comp();
            nvPhysXComponent.release();
        });

        scene.close(renderer);
    }


    public void dispose(){

    }
}
