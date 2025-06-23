package editor;

import dev.dominion.ecs.api.Entity;
import fori.*;
import fori.asset.AssetPack;
import fori.asset.AssetPacks;

import fori.graphics.*;

import fori.graphics.DynamicMesh;
import fori.graphics.StaticMeshBatch;
import fori.graphics.ecs.*;
import org.apache.commons.cli.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.File;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.util.*;

import static fori.graphics.VertexAttributes.Type.*;
import static fori.graphics.ShaderRes.ShaderStage.FragmentStage;
import static fori.graphics.ShaderRes.ShaderStage.VertexStage;
import static fori.graphics.ShaderRes.Type.*;

public class RuntimeStage extends Stage {
    private Renderer renderer;

    private StaticMeshBatch playerStaticMeshBatch, terrainStaticMeshBatch;
    private Scene scene;

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

        Matrix4f proj = new Matrix4f().perspective(
                (float) Math.toRadians(45.0f),
                (float) renderer.getWidth() / renderer.getHeight(),
                0.01f,
                100.0f,
                true
        );

        Camera camera = new Camera(
                new Matrix4f().lookAt(
                        new Vector3f(0.0f, 0.0f, 6.0f),
                        new Vector3f(0, 0, 0),
                        new Vector3f(0.0f, 1.0f, 0.0f)
                ),
                proj,
                true
        );

        scene = new Scene("Main_Scene");


        Entity player;
        ShaderProgram playerShaderProgram;
        Mesh playerMesh;
        Matrix4f playerTransform;

        {


            {
                ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                        AssetPacks.<String>getAsset("core:assets/shaders/vulkan/Default.glsl").asset
                );


                playerShaderProgram = ShaderProgram.newShaderProgram(renderer.getRef(), shaderSources.vertexShader, shaderSources.fragmentShader);

                playerShaderProgram.bind(
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


            playerMesh = Mesh.newMesh(MeshType.Static, playerShaderProgram.getAttributes(), AssetPacks.getAsset("core:assets/models/viking_room.obj"));
            playerStaticMeshBatch = renderer.newStaticMeshBatch(100000, 100000, 1, playerShaderProgram);

            renderer.submitStaticMesh(playerStaticMeshBatch, playerMesh, new MeshUploaderWithTransform(0));

            playerStaticMeshBatch.uploadsFinished();

            Texture texture = Texture.newTexture(renderer.getRef(), AssetPacks.getAsset("core:assets/textures/viking_room.png"), Texture.Filter.Linear, Texture.Filter.Linear);
            playerTransform = new Matrix4f().identity().translate(0, -1, 0).rotate((float) (-Math.PI / 2), 1, 0, 0);


            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                playerStaticMeshBatch.getShaderProgram().updateTextures(frameIndex, new ShaderUpdate<>("textures", 0, 2, texture).arrayIndex(0));
                ByteBuffer transformsBufferData = playerStaticMeshBatch.getTransformsBuffers()[frameIndex].get();
                ByteBuffer cameraBufferData = playerStaticMeshBatch.getCameraBuffers()[frameIndex].get();

                playerTransform.get(0, transformsBufferData);


                camera.getView().get(0, cameraBufferData);
                camera.getProj().get(4 * 4 * Float.BYTES, cameraBufferData);

                playerStaticMeshBatch.getShaderProgram().updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("camera", 0, 0, playerStaticMeshBatch.getCameraBuffers()[frameIndex]),
                        new ShaderUpdate<>("transforms", 0, 1, playerStaticMeshBatch.getTransformsBuffers()[frameIndex])
                );
            }

        }
        player = scene.createEntity(
                new MeshComponent(playerMesh),
                new ShaderComponent(playerShaderProgram),
                new TransformComponent(playerTransform)
        );

        Entity terrain;
        ShaderProgram terrainShaderProgram;
        Mesh terrainMesh;
        Matrix4f terrainTransform;

        {


            {
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

            }

            int width = 32;
            int depth = 32;

            int vertexCount = (width + 1) * (depth + 1);

            Map<VertexAttributes.Type, List<Float>> vertexData = new HashMap<>();
            vertexData.put(PositionFloat3, new ArrayList<>());
            List<Integer> indices = new ArrayList<>();

            terrainMesh = new Mesh(MeshType.Static, vertexData, indices, vertexCount);////Mesh.newMesh(MeshType.Static, playerShaderProgram.getAttributes(), AssetPacks.getAsset("core:assets/models/viking_room.obj"));
            terrainStaticMeshBatch = renderer.newStaticMeshBatch(100000, 100000, 1, terrainShaderProgram);

            {
                List<Float> positions = vertexData.get(PositionFloat3);



                float wSpacing = 0.1f;
                float zSpacing = 0.1f;

                for(int z = 0; z < depth + 1; z++) {
                    for (int x = 0; x < width + 1; x++) {


                        float xc = (x * wSpacing) - (width * wSpacing) / 2.0f;
                        float yc = 0.1f * (float) (Math.cos(2 * Math.PI * (x / (width + 1f))) * (Math.cos(2 * Math.PI * (z / (depth + 1f)))));
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

            terrainTransform = new Matrix4f().identity().translate(0, -1, 0);


            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                ByteBuffer transformsBufferData = terrainStaticMeshBatch.getTransformsBuffers()[frameIndex].get();
                ByteBuffer cameraBufferData = terrainStaticMeshBatch.getCameraBuffers()[frameIndex].get();

                terrainTransform.get(0, transformsBufferData);

                camera.getView().get(0, cameraBufferData);
                camera.getProj().get(4 * 4 * Float.BYTES, cameraBufferData);

                terrainStaticMeshBatch.getShaderProgram().updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("camera", 0, 0, terrainStaticMeshBatch.getCameraBuffers()[frameIndex]),
                        new ShaderUpdate<>("transforms", 0, 1, terrainStaticMeshBatch.getTransformsBuffers()[frameIndex])
                );
            }

        }
        terrain = scene.createEntity(
                new MeshComponent(terrainMesh),
                new ShaderComponent(terrainShaderProgram),
                new TransformComponent(terrainTransform)
        );





        /*
        {


            ShaderProgram shaderProgram;
            {
                ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                        AssetPacks.<String>getAsset("core:assets/shaders/vulkan/Default.glsl").asset
                );


                shaderProgram = ShaderProgram.newShaderProgram(renderer.getRef(), shaderSources.vertexShader, shaderSources.fragmentShader);

                shaderProgram.bind(
                        new Attributes.Type[]{
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


            Mesh mesh = Mesh.newMesh(MeshType.Dynamic, AssetPacks.getAsset("core:assets/models/viking_room.obj"));
            dynamicMesh1 = renderer.submitDynamicMesh(mesh, 100000, 100000, shaderProgram);


            Texture texture = Texture.newTexture(renderer.getRef(), AssetPacks.getAsset("core:assets/textures/viking_room.png"), Texture.Filter.Linear, Texture.Filter.Linear);
            Matrix4f transform1 = new Matrix4f().identity().translate(0.5f, 0, 0).rotate((float) (Math.PI / 2), 1, 0, 0);


            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                dynamicMesh1.getShaderProgram().updateTextures(frameIndex, new ShaderUpdate<>("textures", 0, 2, texture).arrayIndex(0));
                ByteBuffer transformsBufferData = dynamicMesh1.getTransformsBuffers()[frameIndex].get();
                ByteBuffer cameraBufferData = dynamicMesh1.getCameraBuffers()[frameIndex].get();

                transform1.get(0, transformsBufferData);


                camera.getView().get(0, cameraBufferData);
                camera.getProj().get(4 * 4 * Float.BYTES, cameraBufferData);

                dynamicMesh1.getShaderProgram().updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("camera", 0, 0, dynamicMesh1.getCameraBuffers()[frameIndex]),
                        new ShaderUpdate<>("transforms", 0, 1, dynamicMesh1.getTransformsBuffers()[frameIndex])
                );
            }
        }



        {


            ShaderProgram shaderProgram;
            {
                ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                        AssetPacks.<String>getAsset("core:assets/shaders/vulkan/Terrain.glsl").asset
                );


                shaderProgram = ShaderProgram.newShaderProgram(renderer.getRef(), shaderSources.vertexShader, shaderSources.fragmentShader);

                shaderProgram.bind(
                        new Attributes.Type[]{
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

            }


            Mesh mesh;

            {
                ArrayList<Float> vertices = new ArrayList<>();
                ArrayList<Float> textureUVs = new ArrayList<>();
                ArrayList<Integer> indices = new ArrayList<>();
                ArrayList<Integer> textureIndices = new ArrayList<>();

                int width = 32;
                int depth = 32;

                float wSpacing = 0.5f;
                float zSpacing = 0.5f;

                for(int z = 0; z < depth + 1; z++) {
                    for (int x = 0; x < width + 1; x++) {

                        float xc = (x * wSpacing) - (width * wSpacing) / 2.0f;
                        float yc = 0.0f;
                        float zc = (z * zSpacing) - (depth * zSpacing) / 2.0f;


                        vertices.add(xc);
                        vertices.add(yc);
                        vertices.add(zc);

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

                mesh = new Mesh(MeshType.Static, vertices, textureUVs, indices, textureIndices, vertices.size() / 3);

            }



            staticMeshBatch2 = renderer.newStaticMeshBatch(100000, 100000, 1, shaderProgram);

            renderer.submitStaticMesh(staticMeshBatch2, mesh, 0);
            staticMeshBatch2.uploadsFinished();


            Matrix4f transform1 = new Matrix4f().identity().translate(0, -1, 0);


            for (int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                ByteBuffer transformsBufferData = staticMeshBatch2.getTransformsBuffers()[frameIndex].get();
                ByteBuffer cameraBufferData = staticMeshBatch2.getCameraBuffers()[frameIndex].get();

                transform1.get(0, transformsBufferData);


                camera.getView().get(0, cameraBufferData);
                camera.getProj().get(4 * 4 * Float.BYTES, cameraBufferData);

                staticMeshBatch2.getShaderProgram().updateBuffers(
                        frameIndex,
                        new ShaderUpdate<>("camera", 0, 0, staticMeshBatch2.getCameraBuffers()[frameIndex]),
                        new ShaderUpdate<>("transforms", 0, 1, staticMeshBatch2.getTransformsBuffers()[frameIndex])
                );
            }


        }


         */


        scene.addSystem(new RenderSystem(renderer, surface));
        scene.start(3);


    }

    public boolean update(){
        renderer.update(surface.update());

        return !surface.shouldClose();
    }

    @Override
    public void closing() {
        renderer.destroyStaticMeshBatch(playerStaticMeshBatch);
        renderer.destroyStaticMeshBatch(terrainStaticMeshBatch);
        scene.close();

        //renderer.destroyDynamicMesh(dynamicMesh1);
    }


    public void dispose(){

    }
}
