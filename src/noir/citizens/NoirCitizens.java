package noir.citizens;

import fori.Logger;
import fori.Stage;
import fori.asset.AssetPack;
import fori.asset.AssetPacks;
import fori.ecs.Engine;
import fori.ecs.Entity;
import fori.graphics.*;
import org.apache.commons.cli.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.Objects;

import static fori.graphics.Attributes.Type.*;
import static fori.graphics.ShaderRes.ShaderStage.FragmentStage;
import static fori.graphics.ShaderRes.ShaderStage.VertexStage;
import static fori.graphics.ShaderRes.Type.*;

public class NoirCitizens extends Stage {
    private PlatformWindow window;
    private Renderer renderer;
    private Engine ecs;


    public void init(String[] cliArgs){
        Options options = new Options();

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


        System.out.println("Vsync " + vsync);







        AssetPacks.open("core", AssetPack.openLocal(new File("assets")));

        if(logDstPath == null) Logger.setConsoleTarget(System.out);
        else Logger.setFileTarget(new File(logDstPath));

        window = new PlatformWindow(width, height, "Noir Citizens", true);
        renderer = Renderer.newRenderer(getStageRef(), window, window.getWidth(), window.getHeight(), new RendererSettings(RenderAPI.Vulkan).validation(validation).vsync(vsync));
        ecs = new Engine(
                new InputSystem(window),
                new RenderSystem(renderer),
                new UISystem(renderer)
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
                            ).sizeBytes(1 * SizeUtil.MATRIX_SIZE_BYTES),
                            new ShaderRes(
                                    "materials",
                                    2,
                                    CombinedSampler,
                                    FragmentStage
                            ).count(3)
                    )
            );

        }

        Entity player = new Entity("Player");
        {

            MeshComponent meshComponent = new MeshComponent(
                    Mesh.newMesh(AssetPacks.getAsset("core:assets/models/colt9.fbx")),
                    shaderProgram,
                    Texture.newTexture(
                            renderer.getRef(),
                            AssetPacks.getAsset("core:assets/textures/CC9_bolt_BaseColor.png"),
                            Texture.Filter.Linear,
                            Texture.Filter.Linear
                    ),
                    Texture.newTexture(
                            renderer.getRef(),
                            AssetPacks.getAsset("core:assets/textures/CC9_frame_BaseColor.png"),
                            Texture.Filter.Linear,
                            Texture.Filter.Linear
                    ),
                    Texture.newTexture(
                            renderer.getRef(),
                            AssetPacks.getAsset("core:assets/textures/CC9_mag_BaseColor.png"),
                            Texture.Filter.Linear,
                            Texture.Filter.Linear
                    )
            );



            ecs.addComponents(
                    player,
                    meshComponent
            );

        }





        Entity camera = new Entity("Camera");
        {

            ecs.addComponents(
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


        ecs.update();
        renderer.update();
        window.update();

        return !window.shouldClose();
    }

    public void dispose(){
        window.close();
    }
}
