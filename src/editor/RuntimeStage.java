package editor;

import dev.dominion.ecs.api.Entity;
import fori.*;
import fori.asset.AssetPack;
import fori.asset.AssetPacks;

import fori.graphics.*;

import fori.graphics.StaticMeshBatch;
import fori.ecs.*;
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

public class RuntimeStage extends Stage {
    private Renderer renderer;
    private Scene scene;

    Entity shopEntity;
    Entity terrainEntity;
    Entity cameraEntity;
    Entity playerEntity;



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
                surface.getRef(),
                surface,
                surface.getWidth(),
                surface.getHeight(),
                new RendererSettings(RenderAPI.Vulkan)
                        .validation(validation)
                        .vsync(vsync)
        );

        scene = new Scene("Main_Scene");


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
            ShaderProgram shopShaderProgram;
            Mesh shopMesh;

            ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                    AssetPacks.<String>getAsset("core:assets/shaders/vulkan/Default.glsl").asset
            );


            shopShaderProgram = ShaderProgram.newShaderProgram(renderer.getRef(), shaderSources.vertexShader, shaderSources.fragmentShader);

            shopShaderProgram.bind(
                    new VertexAttributes.Type[]{
                            PositionFloat3,
                            TransformIndexFloat1,
                            UVFloat2,
                    },
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


            shopMesh = Mesh.newMesh(shopShaderProgram.getAttributes(), AssetPacks.getAsset("core:assets/models/viking_room.obj"));
            StaticMeshBatch shopStaticMeshBatch = renderer.newStaticMeshBatch(100000, 100000, 1, shopShaderProgram);

            renderer.submitStaticMesh(shopStaticMeshBatch, shopMesh, new MeshUploaderWithTransform(0));

            shopStaticMeshBatch.uploadsFinished();
            scene.registerStaticMeshBatch("Shops", shopStaticMeshBatch);

            Texture texture = Texture.newTexture(renderer.getRef(), AssetPacks.getAsset("core:assets/textures/viking_room.png"), Texture.Filter.Linear, Texture.Filter.Linear);


            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                shopStaticMeshBatch.getShaderProgram().updateTextures(frameIndex, new ShaderUpdate<>("textures", 0, 2, texture).arrayIndex(0));

                shopStaticMeshBatch.getShaderProgram().updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("camera", 0, 0, shopStaticMeshBatch.getCameraBuffers()[frameIndex]),
                        new ShaderUpdate<>("transforms", 0, 1, shopStaticMeshBatch.getTransformsBuffers()[frameIndex])
                );
            }


            shopEntity = scene.createEntity(
                    new StaticMeshComponent(shopStaticMeshBatch, shopMesh),
                    new ShaderComponent(shopShaderProgram),
                    new TransformComponent(0, new Matrix4f().identity())
            );
        }

        //Terrain
        {
            ShaderProgram terrainShaderProgram;
            Mesh terrainMesh;

            ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                    AssetPacks.<String>getAsset("core:assets/shaders/vulkan/Terrain.glsl").asset
            );


            terrainShaderProgram = ShaderProgram.newShaderProgram(renderer.getRef(), shaderSources.vertexShader, shaderSources.fragmentShader);

            terrainShaderProgram.bind(
                    new VertexAttributes.Type[]{
                            PositionFloat3,
                            TransformIndexFloat1,
                    },
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

            int width = 128;
            int depth = 128;

            int vertexCount = (width + 1) * (depth + 1);

            Map<VertexAttributes.Type, List<Float>> vertexData = new HashMap<>();
            vertexData.put(PositionFloat3, new ArrayList<>());
            List<Integer> indices = new ArrayList<>();

            terrainMesh = new Mesh(vertexData, indices, vertexCount);
            StaticMeshBatch terrainStaticMeshBatch = renderer.newStaticMeshBatch(100000, 100000, 1, terrainShaderProgram);

            {
                List<Float> positions = vertexData.get(PositionFloat3);



                float wSpacing = 0.5f;
                float zSpacing = 0.5f;

                for(int z = 0; z < depth + 1; z++) {
                    for (int x = 0; x < width + 1; x++) {


                        float xc = (x * wSpacing) - (width * wSpacing) / 2.0f;
                        float yc = -0.5f * (float) (Math.sin(2 * Math.PI * (x / (width + 1f))) * (Math.cos(2 * Math.PI * (z / (depth + 1f)))));
                        float zc = (z * zSpacing) - (depth * zSpacing) / 2.0f;


                        positions.add(xc);
                        positions.add(yc);
                        positions.add(zc);

                    }
                }

                int index = 0;

                for(int r = 0; r < depth; r++) {
                    for (int i = 0; i < width; i++) {


                        int i0 = 0 + index;
                        int i1 = 1 + index;
                        int i2 = width + 2 + index;
                        int i3 = width + 1 + index;


                        indices.add(i0);
                        indices.add(i3);
                        indices.add(i2);
                        indices.add(i2);
                        indices.add(i1);
                        indices.add(i0);

                        index++;
                    }

                    index++;
                }
            }

            renderer.submitStaticMesh(terrainStaticMeshBatch, terrainMesh, new MeshUploaderWithTransform(0));


            terrainStaticMeshBatch.uploadsFinished();
            scene.registerStaticMeshBatch("Terrain", terrainStaticMeshBatch);

            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                terrainStaticMeshBatch.getShaderProgram().updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("camera", 0, 0, terrainStaticMeshBatch.getCameraBuffers()[frameIndex]),
                        new ShaderUpdate<>("transforms", 0, 1, terrainStaticMeshBatch.getTransformsBuffers()[frameIndex])
                );
            }

            terrainEntity = scene.createEntity(
                    new StaticMeshComponent(terrainStaticMeshBatch, terrainMesh),
                    new ShaderComponent(terrainShaderProgram),
                    new TransformComponent(0, new Matrix4f().identity().translate(0, -1, 0))
            );

        }

        //Player
        {


            ShaderProgram shaderProgram;
            {
                ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                        AssetPacks.<String>getAsset("core:assets/shaders/vulkan/Default.glsl").asset
                );


                shaderProgram = ShaderProgram.newShaderProgram(renderer.getRef(), shaderSources.vertexShader, shaderSources.fragmentShader);

                shaderProgram.bind(
                        new VertexAttributes.Type[]{
                                PositionFloat3,
                                TransformIndexFloat1,
                                UVFloat2,
                        },
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

            }


            Mesh mesh = Mesh.newTestQuad(); //Mesh.newMesh(shaderProgram.getAttributes(), AssetPacks.getAsset("core:assets/models/viking_room.obj"));
            DynamicMesh dynamicMesh = renderer.submitDynamicMesh(mesh, new MeshUploaderWithTransform(0), 100000, 100000, shaderProgram);
            Texture texture = Texture.newTexture(renderer.getRef(), AssetPacks.getAsset("core:assets/textures/viking_room.png"), Texture.Filter.Linear, Texture.Filter.Linear);

            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                dynamicMesh.getShaderProgram().updateTextures(frameIndex, new ShaderUpdate<>("textures", 0, 2, texture).arrayIndex(0));
                dynamicMesh.getShaderProgram().updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("camera", 0, 0, dynamicMesh.getCameraBuffers()[frameIndex]),
                        new ShaderUpdate<>("transforms", 0, 1, dynamicMesh.getTransformsBuffers()[frameIndex])
                );
            }

            playerEntity = scene.createEntity(
                    new DynamicMeshComponent(dynamicMesh, mesh),
                    new ShaderComponent(shaderProgram),
                    new TransformComponent(new Matrix4f().identity().translate(-2, 0, 0))
            );
        }










        scene.addSystem(new RenderSystem(renderer, scene, surface));


    }

    float rot = 0;

    public boolean update(){
        scene.tick();

        shopEntity.get(TransformComponent.class).transform().identity().rotate(rot, 0, 1, 0);
        playerEntity.get(TransformComponent.class).transform().identity().translate(-2, 0, 0).rotate(rot, 0, 1, 0);


        rot += 0.1f;


        renderer.update(surface.update());

        return !surface.shouldClose();
    }

    @Override
    public void closing() {
        scene.close(renderer);
    }


    public void dispose(){

    }
}
