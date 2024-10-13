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

            int matrixSizeBytes = 4 * 4 * Float.BYTES;

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
                            ).sizeBytes(2 * matrixSizeBytes),
                            new ShaderRes(
                                    "transforms",
                                    1,
                                    ShaderStorageBuffer,
                                    VertexStage
                            ).sizeBytes(1 * matrixSizeBytes),
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
            Texture texture1 = Texture.newTexture(AssetPacks.getAsset("core:assets/viking_room.png"), Texture.Filter.Nearest, Texture.Filter.Nearest);


            ArrayList<Float> vertices = new ArrayList<>();
            ArrayList<Integer> indices = new ArrayList<>();

            {

                AIScene aiScene = Assimp.aiImportFile("assets/viking_room.obj", aiProcess_Triangulate | aiProcess_FlipUVs);
                int numMaterials = aiScene.mNumMaterials();
                PointerBuffer aiMaterials = aiScene.mMaterials();

                for (int i = 0; i < numMaterials; i++) {
                    AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
                }

                int numMeshes = aiScene.mNumMeshes();

                PointerBuffer aiMeshes = aiScene.mMeshes();

                for (int i = 0; i < numMeshes; i++) {
                    AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
                    AIVector3D.Buffer aiVertices = aiMesh.mVertices();

                    int vertexIndex = 0;

                    while (aiVertices.remaining() > 0) {
                        AIVector3D aiVertex = aiVertices.get();
                        AIVector3D aiTextureCoords = aiMesh.mTextureCoords(0).get(vertexIndex);

                        vertices.add(aiVertex.x());
                        vertices.add(aiVertex.y());
                        vertices.add(aiVertex.z());
                        vertices.add(0f);

                        vertices.add(aiTextureCoords.x());
                        vertices.add(aiTextureCoords.y());

                        vertices.add(0f);

                        vertexIndex++;
                    }


                    for (int j = 0; j < aiMesh.mNumFaces(); j++) {
                        AIFace aiFace = aiMesh.mFaces().get(j);

                        IntBuffer indicesBuffer = aiFace.mIndices();
                        for (int k = 0; k < indicesBuffer.capacity(); k++) {
                            indices.add(indicesBuffer.get(k));
                        }
                    }

                }
            }
            System.out.println("Indices: " + indices.size());


            ecs.addComponents(
                    player,
                    new MeshComponent(vertices, indices, shaderProgram, texture1)
            );

        }

        Entity jimbob = new Entity("JimBob");
        {
            Texture texture2 = Texture.newTexture(AssetPacks.getAsset("core:assets/ForiEngine.png"), Texture.Filter.Nearest, Texture.Filter.Nearest);


            ArrayList<Float> vertices = new ArrayList<>();
            ArrayList<Integer> indices = new ArrayList<>();

            {

                AIScene aiScene = Assimp.aiImportFile("assets/models/Untitled.glb", aiProcess_Triangulate | aiProcess_FlipUVs);
                int numMaterials = aiScene.mNumMaterials();
                PointerBuffer aiMaterials = aiScene.mMaterials();

                for (int i = 0; i < numMaterials; i++) {
                    AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
                }

                int numMeshes = aiScene.mNumMeshes();

                PointerBuffer aiMeshes = aiScene.mMeshes();

                for (int i = 0; i < numMeshes; i++) {
                    AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));
                    AIVector3D.Buffer aiVertices = aiMesh.mVertices();

                    int vertexIndex = 0;

                    while (aiVertices.remaining() > 0) {
                        AIVector3D aiVertex = aiVertices.get();
                        //AIVector3D aiTextureCoords = aiMesh.mTextureCoords(0).get(vertexIndex);

                        AIVector3D aiTextureCoords = AIVector3D.create().set(1f, 1f, 1f);


                        //Both the Material and Transform Index are going to depend on our queueIndex in the Mesh

                        vertices.add(aiVertex.x());
                        vertices.add(aiVertex.y());
                        vertices.add(aiVertex.z());
                        vertices.add(1f);

                        vertices.add(aiVertex.x());
                        vertices.add(aiVertex.y());

                        vertices.add(1f);

                        vertexIndex++;
                    }


                    for (int j = 0; j < aiMesh.mNumFaces(); j++) {
                        AIFace aiFace = aiMesh.mFaces().get(j);

                        IntBuffer indicesBuffer = aiFace.mIndices();
                        for (int k = 0; k < indicesBuffer.capacity(); k++) {
                            indices.add(indicesBuffer.get(k));
                        }
                    }

                }
            }

            MeshComponent meshComponent = new MeshComponent(vertices, indices, shaderProgram, texture2);
            meshComponent.transform.rotate((float) Math.toRadians(-90), 1.0f, 0.0f, 0.0f);
            meshComponent.transform.scale(5f);

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
                                    new Matrix4f().lookAt(new Vector3f(0.0f, 2.0f, 3.0f), new Vector3f(0, 0, 0), new Vector3f(0.0f, 1.0f, 0.0f)),
                                    new Matrix4f().perspective((float) Math.toRadians(35.0f), (float) renderer.getWidth() / renderer.getHeight(), 0.01f, 100.0f, true),
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
