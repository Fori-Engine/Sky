package fori.graphics;

import fori.Logger;
import fori.asset.Asset;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.assimp.Assimp.*;

public class MeshData {
    private Map<String, List<Float>> data;
    private List<Integer> indices;
    private int vertexCount;

    public MeshData(Map<String, List<Float>> data, List<Integer> indices, int vertexCount) {
        this.data = data;
        this.indices = indices;
        this.vertexCount = vertexCount;
    }

    public Map<String, List<Float>> getData() {
        return data;
    }

    public List<Integer> getIndices() {
        return indices;
    }

    public static MeshData newMeshFromObj(Asset<byte[]> asset){
        HashMap<String, List<Float>> data = new HashMap<>();

        data.put("Positions", new LinkedList<>());
        data.put("TextureUVs", new LinkedList<>());

        List<Integer> indices = new LinkedList<>();
        List<AIMesh> meshList = new LinkedList<>();

        ByteBuffer assetData = MemoryUtil.memAlloc(asset.asset.length);
        assetData.put(asset.asset);
        assetData.flip();
        AIScene scene = Assimp.aiImportFileFromMemory(assetData, aiProcess_FlipUVs | aiProcess_Triangulate | aiProcess_JoinIdenticalVertices, (ByteBuffer) null);

        int meshCount = scene.mNumMeshes();
        PointerBuffer meshes = scene.mMeshes();

        for (int i = 0; i < meshCount; i++) meshList.add(AIMesh.create(meshes.get(i)));


        AINode root = scene.mRootNode();
        openNode(root, meshList, data, indices);
        Logger.info(MeshData.class, "Loaded mesh " + asset.name);
        MemoryUtil.memFree(assetData);

        int vertexCount = getVertexCount(data, 5);
        return new MeshData(data, indices, vertexCount);
    }

    private static int getVertexCount(Map<String, List<Float>> data, int vertexSize) {
        int combinedAttributesSize = 0;
        for(List<Float> attributeList : data.values()) {
            combinedAttributesSize += attributeList.size();
        }
        return combinedAttributesSize / vertexSize;
    }

    private static void openMesh(AIMesh mesh, Map<String, List<Float>> data, List<Integer> indices){

        List<Float> positionsList = data.get("Positions");
        List<Float> textureUVsList = data.get("TextureUVs");

        for (int faceIndex = 0; faceIndex < mesh.mNumFaces(); faceIndex++) {
            int vertexCount = getVertexCount(data, 5);

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

            positionsList.add(aiVertex.x());
            positionsList.add(aiVertex.y());
            positionsList.add(aiVertex.z());

            textureUVsList.add(aiTextureCoords.x());
            textureUVsList.add(aiTextureCoords.y());

        }
    }
    private static void openNode(AINode parentNode, List<AIMesh> meshList, Map<String, List<Float>> data, List<Integer> indices) {
        IntBuffer meshes = parentNode.mMeshes();
        for (int i = 0; i < parentNode.mNumMeshes(); i++) {
            int meshIndex = meshes.get(i);
            AIMesh mesh = meshList.get(meshIndex);
            openMesh(mesh, data, indices);
        }

        for (int z = 0; z < parentNode.mNumChildren(); z++) {
            AINode child = AINode.create(parentNode.mChildren().get(z));
            openNode(child, meshList, data, indices);
        }


    }



    public int getVertexCount() {
        return vertexCount;
    }

    public int getIndexCount() {
        return indices.size();
    }
}
