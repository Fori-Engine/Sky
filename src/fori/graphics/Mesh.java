package fori.graphics;

import fori.Logger;
import fori.asset.Asset;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;

public class Mesh {
    public MeshType type;
    public List<Float> vertices;
    public List<Float> textureUVs;
    public List<Integer> indices;
    public List<Integer> textureIndices;
    public int vertexCount;

    public Mesh(MeshType type, List<Float> vertices, List<Float> textureUVs, List<Integer> indices, List<Integer> textureIndices, int vertexCount) {
        this.type = type;
        this.vertices = vertices;
        this.textureUVs = textureUVs;
        this.indices = indices;
        this.textureIndices = textureIndices;
        this.vertexCount = vertexCount;
    }


    public static Mesh newMesh(MeshType type, Asset<byte[]> asset){
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


        return new Mesh(type, vertices, textureUVs, indices, textureIndices, vertices.size() / 3);
    }

    public static Mesh newTestQuad(MeshType type) {
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Float> textureUVs = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        ArrayList<Integer> textureIndices = new ArrayList<>();

        vertices.add(-0.5f);
        vertices.add(-0.5f);
        vertices.add(0.0f);

        vertices.add(0.5f);
        vertices.add(-0.5f);
        vertices.add(0.0f);

        vertices.add(0.5f);
        vertices.add(0.5f);
        vertices.add(0.0f);

        vertices.add(-0.5f);
        vertices.add(0.5f);
        vertices.add(0.0f);

        indices.add(0);
        indices.add(1);
        indices.add(2);
        indices.add(2);
        indices.add(3);
        indices.add(0);

        textureUVs.add(0.0f);
        textureUVs.add(0.0f);

        textureUVs.add(0.0f);
        textureUVs.add(0.0f);

        textureUVs.add(0.0f);
        textureUVs.add(0.0f);

        textureUVs.add(0.0f);
        textureUVs.add(0.0f);


        return new Mesh(type, vertices, textureUVs, indices, textureIndices, vertices.size() / 3);

    }

    public void put(int currentVertexCount, ShaderProgram shaderProgram, int transformIndex, ByteBuffer vertexBufferData, ByteBuffer indexBufferData) {
        for (int vertex = 0; vertex < vertexCount; vertex++) {


            for(Attributes.Type attribute : shaderProgram.getAttributes()){
                if(attribute == Attributes.Type.PositionFloat3){
                    float x = vertices.get(attribute.size * vertex);
                    float y = vertices.get(attribute.size * vertex + 1);
                    float z = vertices.get(attribute.size * vertex + 2);

                    vertexBufferData.putFloat(x);
                    vertexBufferData.putFloat(y);
                    vertexBufferData.putFloat(z);
                }

                else if(attribute == Attributes.Type.UVFloat2){
                    float u = textureUVs.get(attribute.size * vertex);
                    float v = textureUVs.get(attribute.size * vertex + 1);

                    vertexBufferData.putFloat(u);
                    vertexBufferData.putFloat(v);
                }

                else if(attribute == Attributes.Type.TransformIndexFloat1) vertexBufferData.putFloat(transformIndex);
            }
        }

        for(int index : indices) {
            indexBufferData.putInt(currentVertexCount + index);
        }
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
