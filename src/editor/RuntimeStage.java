package editor;

import fori.*;
import fori.asset.AssetPack;
import fori.asset.AssetPacks;

import fori.graphics.*;

import fori.graphics.aurora.StaticMeshBatch;
import org.apache.commons.cli.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.File;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.util.Objects;

import static fori.graphics.Attributes.Type.*;
import static fori.graphics.ShaderRes.ShaderStage.FragmentStage;
import static fori.graphics.ShaderRes.ShaderStage.VertexStage;
import static fori.graphics.ShaderRes.Type.*;

public class RuntimeStage extends Stage {
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
                surface.getRef(),
                surface,
                surface.getWidth(),
                surface.getHeight(),
                new RendererSettings(RenderAPI.Vulkan)
                        .validation(validation)
                        .vsync(vsync)
        );



        ShaderProgram shaderProgram;
        {
            ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                    AssetPacks.<String> getAsset("core:assets/shaders/vulkan/Default.glsl").asset
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

        Camera camera = new Camera(
                new Matrix4f().lookAt(
                        new Vector3f(0.0f, 2.0f, 3.0f),
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



        Mesh mesh = Mesh.newMesh(MeshType.Static, AssetPacks.getAsset("core:assets/models/viking_room.obj"));
        StaticMeshBatch staticMeshBatch = renderer.newStaticMeshBatch(100000, 100000, 2, shaderProgram);

        renderer.submitStaticMesh(staticMeshBatch, mesh, 0);
        renderer.submitStaticMesh(staticMeshBatch, mesh, 1);


        Texture texture = Texture.newTexture(renderer.getRef(), AssetPacks.getAsset("core:assets/textures/viking_room.png"), Texture.Filter.Linear, Texture.Filter.Linear);
        Matrix4f transform1 = new Matrix4f().identity();
        Matrix4f transform2 = new Matrix4f().translate(1, 1, 0);






        for(int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
            staticMeshBatch.getShaderProgram().updateTextures(frameIndex, new ShaderUpdate<>("textures", 0, 2, texture).arrayIndex(0));
            ByteBuffer transformsBufferData = staticMeshBatch.getTransformsBuffers()[frameIndex].get();
            ByteBuffer cameraBufferData = staticMeshBatch.getCameraBuffers()[frameIndex].get();

            transform1.get(0, transformsBufferData);
            transform2.get(SizeUtil.MATRIX_SIZE_BYTES, transformsBufferData);


            camera.getView().get(0, cameraBufferData);
            camera.getProj().get(4 * 4 * Float.BYTES, cameraBufferData);

            staticMeshBatch.getShaderProgram().updateBuffers(
                    frameIndex,
                    new ShaderUpdate<>("camera", 0, 0, staticMeshBatch.getCameraBuffers()[frameIndex]),
                    new ShaderUpdate<>("transforms", 0, 1, staticMeshBatch.getTransformsBuffers()[frameIndex])
            );
        }


    }

    public boolean update(){
        renderer.update(surface.update());

        return !surface.shouldClose();
    }

    public void dispose(){

    }
}
