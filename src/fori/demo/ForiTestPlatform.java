package fori.demo;

import fori.Logger;
import fori.Time;
import fori.asset.AssetPack;
import fori.asset.AssetPacks;
import fori.graphics.*;
import fori.graphics.RenderQueue;
import org.joml.Matrix4f;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import static fori.graphics.ShaderRes.ShaderStage.FragmentStage;
import static fori.graphics.ShaderRes.ShaderStage.VertexStage;
import static fori.graphics.ShaderRes.Type.*;
import static fori.graphics.Attributes.Type.*;
import static org.lwjgl.assimp.Assimp.*;

import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

public class ForiTestPlatform {

    public static void main(String[] args) {

        long start = System.currentTimeMillis();

        AssetPacks.open("core", AssetPack.openLocal(new File("assets")));

        Logger.setConsoleTarget(System.out);

        Logger.meltdown(ForiTestPlatform.class, "I'm lazy");

        PlatformWindow window = new PlatformWindow(1200, 700, "ForiEngine", true);
        Renderer renderer = Renderer.newRenderer(window, window.getWidth(), window.getHeight(), new RendererSettings(RenderAPI.Vulkan).validation(true).vsync(false));
        window.setIcon(AssetPacks.getAsset("core:assets/ForiEngine.png"));


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


        ShaderProgram uiShaderProgram;
        {
            ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                    AssetPacks.<String> getAsset("core:assets/shaders/vulkan/Default.glsl").asset
            );

            int matrixSizeBytes = 4 * 4 * Float.BYTES;

            uiShaderProgram = ShaderProgram.newShaderProgram(shaderSources.vertexShader, shaderSources.fragmentShader);

            uiShaderProgram.bind(
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




        Texture texture1 = Texture.newTexture(AssetPacks.getAsset("core:assets/viking_room.png"), Texture.Filter.Nearest, Texture.Filter.Nearest);
        Texture texture2 = Texture.newTexture(AssetPacks.getAsset("core:assets/ForiEngine.png"), Texture.Filter.Nearest, Texture.Filter.Nearest);



        Camera camera = new Camera(
                new Matrix4f().lookAt(new Vector3f(0.0f, 2.0f, 3.0f), new Vector3f(0, 0, 0), new Vector3f(0.0f, 1.0f, 0.0f)),
                new Matrix4f().perspective((float) Math.toRadians(35.0f), (float) renderer.getWidth() / renderer.getHeight(), 0.01f, 100.0f, true),
                true
        );

        Camera uiCamera = new Camera(
                new Matrix4f().identity(),
                new Matrix4f().ortho(0, renderer.getWidth(), 0, renderer.getHeight(), 0, 1, true),
                false
        );





        RenderQueue renderQueue;
        {
            renderQueue = renderer.newRenderQueue(shaderProgram);//, vertices.size() / 7, indices.size());
            renderQueue.addTexture(0, texture1);
            renderQueue.addTexture(1, texture2);


            ByteBuffer vertexBuffer = renderQueue.getDefaultVertexBuffer().map();
            vertexBuffer.clear();

            for (float vertexPart : vertices) {
                vertexBuffer.putFloat(vertexPart);
            }

            ByteBuffer indexBuffer = renderQueue.getDefaultIndexBuffer().map();
            indexBuffer.clear();
            for (int index : indices) {
                indexBuffer.putInt(index);
            }

            renderQueue.updateQueue(vertices.size() / 7, indices.size());
        }


        RenderQueue uiRenderQueue;
        {
            uiRenderQueue = renderer.newRenderQueue(uiShaderProgram);//, 4, 6);
            uiRenderQueue.addTexture(0, texture1);
            uiRenderQueue.addTexture(1, texture2);


            float[] quadVertices = new float[]{
                    0.0f, 0.0f, 0.0f,
                    0.0f,
                    0.0f, 0.0f,
                    1.0f,

                    0.0f, 300.0f, 0.0f,
                    0.0f,
                    0.0f, 1.0f,
                    1.0f,

                    300.0f, 300.0f, 0.0f,
                    0.0f,
                    1.0f, 1.0f,
                    1.0f,

                    300.0f, 0.0f, 0.0f,
                    0.0f,
                    1.0f, 0.0f,
                    1.0f,

            };

            int[] quadIndices = new int[]{0, 1, 2, 2, 3, 0};

            ByteBuffer vertexBuffer = uiRenderQueue.getDefaultVertexBuffer().map();
            vertexBuffer.clear();
            for (float vertexPart : quadVertices) {
                vertexBuffer.putFloat(vertexPart);
            }

            ByteBuffer indexBuffer = uiRenderQueue.getDefaultIndexBuffer().map();
            indexBuffer.clear();
            for (int index : quadIndices) {
                indexBuffer.putInt(index);
            }







            uiRenderQueue.updateQueue(4, 6);
        }








        ArrayList<ByteBuffer> transformsBufferData = new ArrayList<>();
        ArrayList<ByteBuffer> camerasBufferData = new ArrayList<>();


        for(Buffer transformsBuffer : renderQueue.transformsBuffer){
            transformsBufferData.add(transformsBuffer.map());
        }
        for(Buffer cameraBuffer : renderQueue.cameraBuffer){
            camerasBufferData.add(cameraBuffer.map());
        }


        ArrayList<ByteBuffer> uiTransformsBufferData = new ArrayList<>();
        ArrayList<ByteBuffer> uiCamerasBufferData = new ArrayList<>();


        for(Buffer transformsBuffer : uiRenderQueue.transformsBuffer){
            uiTransformsBufferData.add(transformsBuffer.map());
        }
        for(Buffer cameraBuffer : uiRenderQueue.cameraBuffer){
            uiCamerasBufferData.add(cameraBuffer.map());
        }




        float rotation = 0f;


        Logger.info(ForiTestPlatform.class, "Entering main loop took " + (System.currentTimeMillis() - start) / 1000.0f + " seconds");



        
        while(!window.shouldClose()){

            {

                ByteBuffer transformsBuffer = transformsBufferData.get(renderer.getFrameIndex());
                transformsBuffer.clear();

                Matrix4f transform0 = new Matrix4f().rotate((float) Math.toRadians(360 * Math.cos(0.02f * rotation)), 0.0f, 1.0f, 0.0f).rotate((float) Math.toRadians(-90), new Vector3f(1f, 0f, 0f));
                transform0.get(0, transformsBuffer);

                rotation += 10 * Time.deltaTime();

                ByteBuffer cameraBufferData = camerasBufferData.get(renderer.getFrameIndex());
                {
                    camera.getView().get(0, cameraBufferData);
                    camera.getProj().get(4 * 4 * Float.BYTES, cameraBufferData);
                }

            }


            {

                ByteBuffer transformsBuffer = uiTransformsBufferData.get(renderer.getFrameIndex());
                transformsBuffer.clear();

                Matrix4f transform0 = new Matrix4f().identity();
                transform0.get(0, transformsBuffer);

                ByteBuffer cameraBufferData = uiCamerasBufferData.get(renderer.getFrameIndex());
                {
                    uiCamera.getView().get(0, cameraBufferData);
                    uiCamera.getProj().get(4 * 4 * Float.BYTES, cameraBufferData);
                }

            }




            renderer.update();
            window.update();
        }

        renderer.removeQueue(renderQueue);

        window.close();
    }
}
