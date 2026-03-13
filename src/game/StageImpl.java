package game;

import engine.*;
import engine.asset.AssetPackage;
import engine.asset.AssetRegistry;

import engine.gameui.*;
import engine.graphics.*;

import engine.ecs.*;
import engine.graphics.pipelines.BlinnPhongPipeline;
import engine.graphics.text.MsdfFont;
import engine.physics.Collider;
import engine.physics.Interface;
import game.scripts.FPCameraController;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.lang.Math;
import java.nio.file.Path;
import java.util.Optional;

import static engine.gameui.TextValue.text;

public class StageImpl extends Stage {

    private float startTime;
    private Scene scene;
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
                        .vsync(false)
        );
        renderPipeline = new BlinnPhongPipeline();
        {

            Actor.tryClassload(TransformComponent.class);
            Actor.tryClassload(MeshComponent.class);
            Actor.tryClassload(MeshListComponent.class);
            Actor.tryClassload(RigidBodyComponent.class);
            Actor.tryClassload(CameraComponent.class);
            Actor.tryClassload(SpotlightComponent.class);
            Actor.tryClassload(ShaderComponent.class);
            Actor.tryClassload(ScriptComponent.class);
        }

        scene = new Scene("RootScene");
        scene.addSystem(
                new UISystem(renderer, renderPipeline, surface, scene),
                new RenderSystem(renderer, renderPipeline, scene),
                new BulletSystem(),
                new ScriptSystem(scene)
        );



        Actor rootActor = scene.newRootActor("Root");
        {


            Actor uiActor ;
            //UI
            {

                MsdfFont msdfFont = new MsdfFont(
                        renderer,
                        AssetRegistry.getAsset("core:assets/fonts/AirbusB612/b612-atlas.png"),
                        AssetRegistry.getAsset("core:assets/fonts/AirbusB612/b612-atlas.json")
                );

                uiActor = (scene.newActor(
                        "UI",
                        new UIComponent(
                                new ContainerWidget()
                                        .setIgnore(true)
                                        .setName("W_Container")
                                        .setLayoutEngine(new LineLayoutEngine(LineLayoutEngine.Line.Vertical))
                                        .addWidgets(
                                                new Text(text("Text time"), msdfFont)
                                                        .setName("W_SelectedActorText"),
                                                new Text(text(""), msdfFont)
                                                        .setName("W_FPSText"),
                                                new Button(text("Click me!"), msdfFont)
                                                        .addEventHandler(new EventHandler() {
                                                            @Override
                                                            public void onClick() {
                                                                System.out.println("Foo");
                                                            }
                                                        })
                                        ),
                                Optional.of(new Rect2D(0, 0, renderer.getWidth(), renderer.getHeight()))
                        )
                ));

                rootActor.addActor(uiActor);
            }


            //Camera
            {
                rootActor.addActor(
                        scene.newActor(
                                "Camera",
                                new CameraComponent(
                                        new Camera(
                                                new Matrix4f().lookAt(
                                                        new Vector3f(0.0f, 15.0f, 0.5f),
                                                        new Vector3f(0, 0, 0),
                                                        new Vector3f(0.0f, 1.0f, 0.0f)
                                                ),
                                                new Matrix4f().perspective(
                                                        (float) Math.toRadians(75.0f),
                                                        (float) renderer.getWidth() / renderer.getHeight(),
                                                        0.1f,
                                                        10000,
                                                        true
                                                ),
                                                true
                                        )
                                ),
                                new ScriptComponent(
                                        new FPCameraController(surface, renderer, uiActor)
                                ),
                                new TransformComponent(new Matrix4f().identity().translate(0.0f, 15.0f, 0.5f)),
                                new RigidBodyComponent(Collider.newSphereCollider(1), 1.0f, new Interface(0.7f))

                        ));
            }

            //Floor
            {


                ShaderProgram shaderProgram = ShaderProgram.newShaderProgram(renderer);
                shaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/blinn_phong/Default2_vertex.spv"), ShaderType.VertexShader);
                shaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/blinn_phong/Default2_fragment.spv"), ShaderType.FragmentShader);
                shaderProgram.assemble();



                MeshData meshData = MeshGenerator.newBox(10, 1, 10);

                MeshListComponent meshListComponent = new MeshListComponent(renderer, renderer, 100, 100, 1, shaderProgram);
                meshListComponent.addMeshData(meshData, 0);

                rootActor.addActor(scene.newActor(
                        "Floor",
                        meshListComponent,
                        new MaterialComponent(new engine.graphics.Material(
                                Sampler.newSampler(renderer, Texture.Filter.Linear, Texture.Filter.Linear, true),
                                Texture.newColorTextureFromAsset(renderer, AssetRegistry.getAsset("core:assets/textures/default.png"), TextureFormatType.ColorR8G8B8A8),
                                Texture.newColorTextureFromAsset(renderer, AssetRegistry.getAsset("core:assets/textures/normal_map.png"), TextureFormatType.ColorR8G8B8A8),
                                Texture.newColorTextureFromAsset(renderer, AssetRegistry.getAsset("core:assets/textures/diffuse_map.png"), TextureFormatType.ColorR8G8B8A8),
                                Texture.newColorTextureFromAsset(renderer, AssetRegistry.getAsset("core:assets/textures/specular_map.png"), TextureFormatType.ColorR8G8B8A8)
                        )),
                        new ShaderComponent(shaderProgram),
                        new TransformComponent(new Matrix4f().identity().translate(0, 2, 0).rotate((float) Math.toRadians(0), 0, 0, 1)),
                        new RigidBodyComponent(Collider.newBoxCollider(10, 1, 10), 0.0f, new Interface(0.8f))

                ));
            }


            for(int x = 0; x < 5; x++) {
                for(int z = 0; z < 5; z++) {

                    //Cube
                    {


                        ShaderProgram shaderProgram = ShaderProgram.newShaderProgram(renderer);
                        shaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/blinn_phong/Default2_vertex.spv"), ShaderType.VertexShader);
                        shaderProgram.add(AssetRegistry.getAsset("core:assets/shaders/blinn_phong/Default2_fragment.spv"), ShaderType.FragmentShader);
                        shaderProgram.assemble();



                        MeshData meshData = MeshGenerator.newBox(0.5f, 0.5f, 0.5f);

                        MeshComponent meshComponent = new MeshComponent(renderer, renderer, 50, 50, shaderProgram);
                        meshComponent.setMeshData(meshData);


                        rootActor.addActor(scene.newActor(
                                "Cube (x: " + x + " z: " + z + ")",
                                new MaterialComponent(new engine.graphics.Material(
                                        Sampler.newSampler(renderer, Texture.Filter.Linear, Texture.Filter.Linear, true),
                                        Texture.newColorTextureFromAsset(renderer, AssetRegistry.getAsset("core:assets/textures/brickwall.jpg"), TextureFormatType.ColorR8G8B8A8),
                                        Texture.newColorTextureFromAsset(renderer, AssetRegistry.getAsset("core:assets/textures/normal_map.png"), TextureFormatType.ColorR8G8B8A8),
                                        Texture.newColorTextureFromAsset(renderer, AssetRegistry.getAsset("core:assets/textures/diffuse_map.png"), TextureFormatType.ColorR8G8B8A8),
                                        Texture.newColorTextureFromAsset(renderer, AssetRegistry.getAsset("core:assets/textures/specular_map.png"), TextureFormatType.ColorR8G8B8A8)
                                )),
                                meshComponent,
                                new ShaderComponent(shaderProgram),
                                new TransformComponent(new Matrix4f().identity().translate(x + 1, 5, z + 1).rotate((float) Math.toRadians(45), 0, 0, 1)),
                                new RigidBodyComponent(Collider.newBoxCollider(0.5f, 0.5f, 0.5f), 1.0f, new Interface(0.8f))

                        ));
                    }



                }
            }





            //Spotlight
            {

                float x = 0, y = 10, z = 1f;


                rootActor.addActor(scene.newActor(
                        "Spotlight1",
                        new SpotlightComponent(
                                renderer,
                                new Matrix4f().lookAt(
                                        new Vector3f(x, y, z),
                                        new Vector3f(0, 0, 0),
                                        new Vector3f(0.0f, 1.0f, 0.0f)
                                ),
                                new Matrix4f().perspective(
                                        (float) Math.toRadians(15),
                                        (float) 1,
                                        0.1f,
                                        10.0f,
                                        true
                                ),
                                true
                        )
                ));


            }

            //Spotlight
            {

                float x = 0, y = 10, z = 0f;


                SpotlightComponent spotlightComponent = new SpotlightComponent(
                        renderer,
                        new Matrix4f().lookAt(
                                new Vector3f(x, y, z),
                                new Vector3f(0.0f, -2, 0.0f),
                                new Vector3f(0.0f, 0.0f, 1.0f) //This is relative to the camera!
                        ),
                        new Matrix4f().perspective(
                                (float) Math.toRadians(45),
                                (float) 1,
                                0.1f,
                                10.0f,
                                true
                        ),

                        true
                );
                spotlightComponent.color = new Vector3f(1, 1, 1);

                rootActor.addActor(scene.newActor(
                        "Spotlight2",
                        spotlightComponent
                ));


            }


        }
        scene.setRootActor(rootActor);

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

        scene.getRootActor().previsitAllActors(actor -> {
            if(actor.has(MeshComponent.class)) {
                MeshComponent meshComponent = actor.getComponent(MeshComponent.class);
                for(int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                    Buffer sceneDescBuffer = meshComponent.sceneDescBuffers[frameIndex];
                    Buffer transformsBuffer = meshComponent.transformsBuffers[frameIndex];

                    sceneDescBuffer.disposeAll();
                    transformsBuffer.disposeAll();

                    renderer.remove(sceneDescBuffer);
                    renderer.remove(transformsBuffer);
                }


                meshComponent.vertexBuffer.disposeAll();
                meshComponent.indexBuffer.disposeAll();
                meshComponent.shaderProgram.disposeAll();

                renderer.remove(meshComponent.vertexBuffer);
                renderer.remove(meshComponent.indexBuffer);
                renderer.remove(meshComponent.shaderProgram);
            }
            if(actor.has(MeshListComponent.class)) {
                MeshListComponent meshListComponent = actor.getComponent(MeshListComponent.class);
                for(int frameIndex = 0; frameIndex < renderer.getMaxFramesInFlight(); frameIndex++) {
                    Buffer sceneDescBuffer = meshListComponent.sceneDescBuffers[frameIndex];
                    Buffer transformsBuffer = meshListComponent.transformsBuffers[frameIndex];

                    sceneDescBuffer.disposeAll();
                    transformsBuffer.disposeAll();


                    renderer.remove(sceneDescBuffer);
                    renderer.remove(transformsBuffer);
                }


                meshListComponent.vertexBuffer.disposeAll();
                meshListComponent.indexBuffer.disposeAll();
                meshListComponent.shaderProgram.disposeAll();

                renderer.remove(meshListComponent.vertexBuffer);
                renderer.remove(meshListComponent.indexBuffer);
                renderer.remove(meshListComponent.shaderProgram);
            }
        });


        scene.close();
    }


    public void dispose(){

    }
}
