package noir.citizens;

import fori.Logger;
import fori.asset.AssetPack;
import fori.asset.AssetPacks;
import fori.demo.ForiTestPlatform;
import fori.ecs.Engine;
import fori.ecs.Entity;
import fori.ecs.Pair;
import fori.graphics.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static fori.graphics.Attributes.Type.*;
import static fori.graphics.Attributes.Type.MaterialBaseIndexFloat1;
import static fori.graphics.ShaderRes.ShaderStage.FragmentStage;
import static fori.graphics.ShaderRes.ShaderStage.VertexStage;
import static fori.graphics.ShaderRes.Type.*;
import static org.lwjgl.assimp.Assimp.aiProcess_FlipUVs;
import static org.lwjgl.assimp.Assimp.aiProcess_Triangulate;

public class Stage {
    private PlatformWindow window;
    private Renderer renderer;
    private Engine ecs;

    public void init(){
        AssetPacks.open("core", AssetPack.openLocal(new File("assets")));

        Logger.setConsoleTarget(System.out);

        window = new PlatformWindow(1200, 700, "Noir Citizens", true);
        renderer = Renderer.newRenderer(window, window.getWidth(), window.getHeight(), new RendererSettings(RenderAPI.Vulkan).validation(true).vsync(true));
        ecs = new Engine(
                new InputSystem(window),
                new RenderSystem(renderer)
        );

        ShaderProgram shaderProgram;
        {
            ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                    AssetPacks.<String> getAsset("core:assets/shaders/vulkan/Default.glsl").asset
            );



            shaderProgram = ShaderProgram.newShaderProgram(shaderSources.vertexShader, shaderSources.fragmentShader);

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
                            ).sizeBytes(2 * SizeUtil.MATRIX_SIZE_BYTES),
                            new ShaderRes(
                                    "materials",
                                    2,
                                    CombinedSampler,
                                    FragmentStage
                            ).count(2)
                    )
            );

        }



        Entity player = new Entity("Player");
        {

            MeshComponent meshComponent = new MeshComponent(
                    Mesh.newMesh(AssetPacks.getAsset("core:assets/models/bowser.obj")),
                    shaderProgram,
                    Texture.newTexture(
                            AssetPacks.getAsset("core:assets/bowser_grp.png"),
                            Texture.Filter.Nearest,
                            Texture.Filter.Nearest
                    )
            );

            meshComponent.transform.scale(0.1f);

            ecs.addComponents(
                    player,
                    meshComponent
            );

        }


        Entity jimbob = new Entity("JimBob");
        {

            MeshComponent meshComponent = new MeshComponent(
                    Mesh.newMesh(AssetPacks.getAsset("core:assets/models/viking_room.obj")),
                    shaderProgram,
                    Texture.newTexture(
                            AssetPacks.getAsset("core:assets/viking_room.png"),
                            Texture.Filter.Nearest,
                            Texture.Filter.Nearest
                    )
            );
            meshComponent.transform.translate(2, 0, 0);
            meshComponent.transform.scale(2f);

            ecs.addComponents(
                    jimbob,
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
                                            (float) Math.toRadians(35.0f),
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

        for(Pair<Integer, Integer> view : ecs.getViews()){

            System.out.println("[" + view.a + ", " + view.b + "]");
        }

        //System.exit(1);


    }

    public boolean update(){


        ecs.update();
        renderer.update();
        window.update();

        return !window.shouldClose();
    }

    public void dispose(){
        window.close();
    }
}
