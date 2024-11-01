package fori.graphics;

import fori.Logger;
import fori.Scene;
import fori.asset.Asset;
import fori.ecs.Entity;
import fori.ecs.MeshComponent;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;

public class Mesh {
    public List<Float> vertices;
    public List<Float> textureUVs;
    public List<Integer> indices;
    public List<Integer> textureIndices;
    public int vertexCount;


    public Mesh(List<Float> vertices, List<Float> textureUVs, List<Integer> indices, List<Integer> textureIndices, int vertexCount) {
        this.vertices = vertices;
        this.textureUVs = textureUVs;
        this.indices = indices;
        this.textureIndices = textureIndices;
        this.vertexCount = vertexCount;
    }


    public static Mesh newMesh(Asset<byte[]> asset){
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Float> textureUVs = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        ArrayList<Integer> textureIndices = new ArrayList<>();

        ArrayList<AIMesh> meshList = new ArrayList<>();


        ByteBuffer assetData = MemoryUtil.memAlloc(asset.asset.length);
        assetData.put(asset.asset);
        assetData.flip();
        AIScene scene = Assimp.aiImportFileFromMemory(assetData, aiProcess_FlipUVs | aiProcess_Triangulate | aiProcess_JoinIdenticalVertices, (ByteBuffer) null);





        int meshCount = scene.mNumMeshes();
        PointerBuffer meshes = scene.mMeshes();

        for (int i = 0; i < meshCount; i++) meshList.add(AIMesh.create(meshes.get(i)));



        StringBuilder loadData = new StringBuilder();
        loadData.append("Loading Mesh: " + asset.name);

        AINode root = scene.mRootNode();
        openNode(root, meshList, vertices, textureUVs, textureIndices, indices);
        loadData.append("\n\t" + "Vertex Count: " + vertices.size() / Attributes.Type.PositionFloat3.size);
        loadData.append("\n\t" + "Index Count: " + indices.size());
        loadData.append("\n\t" + "Mesh Count: " + meshCount);

        Logger.info(Mesh.class, loadData.toString());
        MemoryUtil.memFree(assetData);


        return new Mesh(vertices, textureUVs, indices, textureIndices, vertices.size() / 3);
    }
    public static Entity separateMeshesToEntities(Asset<byte[]> asset, ShaderProgram shaderProgram, String tag, Scene scene){
        ArrayList<AIMesh> meshList = new ArrayList<>();


        ByteBuffer assetData = MemoryUtil.memAlloc(asset.asset.length);
        assetData.put(asset.asset);
        assetData.flip();
        AIScene aiScene = Assimp.aiImportFileFromMemory(assetData, aiProcess_FlipUVs | aiProcess_Triangulate | aiProcess_JoinIdenticalVertices, (ByteBuffer) null);

        int meshCount = aiScene.mNumMeshes();
        PointerBuffer meshes = aiScene.mMeshes();

        for (int i = 0; i < meshCount; i++) meshList.add(AIMesh.create(meshes.get(i)));

        AINode root = aiScene.mRootNode();
        return createEntityFromNode(null, root, scene, meshList, shaderProgram, tag == null ? root.mName().dataString() : tag);
    }


    private static Entity createEntityFromNode(Entity parent, AINode aiNode, Scene scene, List<AIMesh> meshList, ShaderProgram shaderProgram, String name) {

        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Float> textureUVs = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        ArrayList<Integer> textureIndices = new ArrayList<>();

        IntBuffer meshes = aiNode.mMeshes();
        for (int i = 0; i < aiNode.mNumMeshes(); i++) {
            int meshIndex = meshes.get(i);
            AIMesh mesh = meshList.get(meshIndex);
            openMesh(mesh, vertices, textureUVs, textureIndices, indices);
        }


        Entity childEntity = new Entity(name);



        //Some AINode's (like the root) don't actually have vertices in some cases which causes a validation error
        scene.addEntity(
                childEntity,
                new MeshComponent(
                        new Mesh(vertices, textureUVs, indices, textureIndices, vertices.size() / 3),
                        shaderProgram,
                        null
                )
        );


        if(parent != null) scene.addChildEntity(parent, childEntity);


        for (int z = 0; z < aiNode.mNumChildren(); z++) {
            AINode child = AINode.create(aiNode.mChildren().get(z));

            createEntityFromNode(childEntity, child, scene, meshList, shaderProgram, child.mName().dataString());
        }

        return childEntity;
    }

    private static void openMesh(AIMesh mesh, List<Float> vertices, List<Float> textureUVs, List<Integer> textureIndices, List<Integer> indices){
        for (int faceIndex = 0; faceIndex < mesh.mNumFaces(); faceIndex++) {
            int vertexCount = vertices.size() / 3;

            AIFace aiFace = mesh.mFaces().get(faceIndex);
            IntBuffer indicesBuffer = aiFace.mIndices();

            for (int k = 0; k < aiFace.mNumIndices(); k++) {
                int index = indicesBuffer.get(k);
                indices.add(vertexCount + index);
            }
        }

        AIVector3D.Buffer aiVertices = mesh.mVertices();

        for (int vertexIndex = 0; vertexIndex < mesh.mNumVertices(); vertexIndex++) {
            AIVector3D aiVertex = aiVertices.get();
            AIVector3D aiTextureCoords = mesh.mTextureCoords(0).get(vertexIndex);

            vertices.add(aiVertex.x());
            vertices.add(aiVertex.y());
            vertices.add(aiVertex.z());

            textureUVs.add(aiTextureCoords.x());
            textureUVs.add(aiTextureCoords.y());

            textureIndices.add(mesh.mMaterialIndex());

        }
    }
    private static void openNode(AINode root, List<AIMesh> meshList, List<Float> vertices, List<Float> textureUVs, List<Integer> textureIndices, List<Integer> indices) {
        IntBuffer meshes = root.mMeshes();
        for (int i = 0; i < root.mNumMeshes(); i++) {
            int meshIndex = meshes.get(i);
            AIMesh mesh = meshList.get(meshIndex);
            openMesh(mesh, vertices, textureUVs, textureIndices, indices);
        }

        for (int z = 0; z < root.mNumChildren(); z++) {
            AINode child = AINode.create(root.mChildren().get(z));
            openNode(child, meshList, vertices, textureUVs, textureIndices, indices);
        }
    }







}
