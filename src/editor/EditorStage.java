package editor;

import fori.*;
import fori.asset.AssetPack;
import fori.asset.AssetPacks;
import fori.ecs.*;
import fori.graphics.*;

import org.apache.commons.cli.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.File;
import java.lang.Math;
import java.util.Objects;

import static fori.graphics.Attributes.Type.*;
import static fori.graphics.ShaderRes.ShaderStage.FragmentStage;
import static fori.graphics.ShaderRes.ShaderStage.VertexStage;
import static fori.graphics.ShaderRes.Type.*;

public class EditorStage extends Stage {
    private Renderer renderer;
    private Engine engine;
    private Scene scene;

    public void init(String[] cliArgs, Surface surface){
        super.init(cliArgs, surface);


        Options options = new Options();
        {

            Option widthOption = new Option("width", true, "The width of the window");
            {
                widthOption.setRequired(false);
                options.addOption(widthOption);
            }
            Option heightOption = new Option("height", true, "The height of the window");
            {
                heightOption.setRequired(false);
                options.addOption(heightOption);
            }
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

        int width = Integer.parseInt(Objects.requireNonNullElse(cmd.getOptionValue("width"), "1920"));
        int height = Integer.parseInt(Objects.requireNonNullElse(cmd.getOptionValue("height"), "1080"));
        boolean vsync = Boolean.parseBoolean(Objects.requireNonNullElse(cmd.getOptionValue("vsync"), "true"));
        boolean validation = Boolean.parseBoolean(Objects.requireNonNullElse(cmd.getOptionValue("validation"), "false"));
        String logDstPath = cmd.getOptionValue("logdst");


        AssetPacks.open("core", AssetPack.openLocal(new File("assets")));


        surface.display();

        renderer = Renderer.newRenderer(surface.getRef(), surface, width, height, new RendererSettings(RenderAPI.Vulkan).validation(validation).vsync(vsync));
        engine = new Engine(
                new RenderSystem(renderer)
        );

        scene = new Scene();

        ShaderProgram shaderProgram;
        {
            ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                    AssetPacks.<String> getAsset("core:assets/shaders/vulkan/Default.glsl").asset
            );



            shaderProgram = ShaderProgram.newShaderProgram(renderer.getRef(), shaderSources.vertexShader, shaderSources.fragmentShader);

            shaderProgram.bind(
                    new Attributes.Type[]{
                            PositionFloat3,
                            RenderQueuePosFloat1,
                            TransformIndexFloat1,
                            UVFloat2,
                            MaterialBaseIndexFloat1

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
                            ).sizeBytes(10 * SizeUtil.MATRIX_SIZE_BYTES),
                            new ShaderRes(
                                    "materials",
                                    2,
                                    CombinedSampler,
                                    FragmentStage
                            ).count((3 * 4))
                    )
            );

        }


        //Entity colt9 = Mesh.separateMeshesToEntities(AssetPacks.getAsset("core:assets/models/colt9.fbx"), shaderProgram, "Colt9", scene);
        //scene.get(colt9, MeshComponent.class).transform.scale(0.15f);





        Entity bowser2 = new Entity("Bowser2");
        {

            MeshComponent meshComponent = new MeshComponent(
                    Mesh.newMesh(AssetPacks.getAsset("core:assets/models/bowser.obj")),
                    shaderProgram,
                    null
            );

            meshComponent.transform.scale(0.15f);
            meshComponent.transform.translate(-12, 0, 0);


            scene.addEntity(
                    bowser2,
                    meshComponent
            );
            //scene.addChildEntity(bowser1, bowser2);

        }





        Entity camera = new Entity("Camera");
        {

            scene.addEntity(
                    camera,
                    new CameraComponent(
                            new Camera(
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
                            )
                    )
            );
        }
    }

    public boolean update(){


        //System.out.println(Time.framesPerSecond());


        engine.update(scene);

        if(surface.getWidth() != renderer.getWidth() || surface.getHeight() != renderer.getHeight()) {
            renderer.onSurfaceResized(surface.getWidth(), surface.getHeight());
        }



        renderer.update();
        surface.update();

        return !surface.shouldClose();
    }

    public void dispose(){

    }
}
