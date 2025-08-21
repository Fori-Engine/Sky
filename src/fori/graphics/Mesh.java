package fori.graphics;

import fori.Logger;
import fori.asset.Asset;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.assimp.Assimp.*;

public class Mesh {
    private Map<VertexAttributes.Type, List<Float>> vertexData;
    private List<Integer> indices;
    private int vertexCount;

    public Mesh(Map<VertexAttributes.Type, List<Float>> vertexData, List<Integer> indices, int vertexCount) {
        this.vertexData = vertexData;
        this.indices = indices;
        this.vertexCount = vertexCount;
    }



    public static Mesh newMesh(VertexAttributes.Type[] vertexAttributes, Asset<byte[]> asset){

        Map<VertexAttributes.Type, List<Float>> vertexData = new HashMap<>();
        ArrayList<Integer> indices = new ArrayList<>();
        ArrayList<AIMesh> meshList = new ArrayList<>();

        for(VertexAttributes.Type attribute : vertexAttributes) {
            vertexData.put(attribute, new ArrayList<Float>());
        }



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
        openNode(root, vertexAttributes, meshList, vertexData, indices);


        Logger.info(Mesh.class, loadData.toString());
        MemoryUtil.memFree(assetData);

        int vertexCount = getVertexCount(vertexData);
        return new Mesh(vertexData, indices, vertexCount);
    }

    public void put(MeshUploader meshUploader, int currentVertexCount, ShaderProgram shaderProgram, ByteBuffer vertexBufferData, ByteBuffer indexBufferData) {
        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            for(VertexAttributes.Type vertexAttribute : shaderProgram.getShaderMap().get(ShaderType.Vertex).getVertexAttributes()){
                meshUploader.upload(vertexAttribute, vertexIndex, vertexBufferData, vertexData);
            }
        }

        for(int index : indices) {
            indexBufferData.putInt(currentVertexCount + index);
        }
    }

    private static int getVertexCount(Map<VertexAttributes.Type, List<Float>> vertexData) {

        for(VertexAttributes.Type vertexAttribute : vertexData.keySet()) {
            List<Float> attributeData = vertexData.get(vertexAttribute);

            //Don't use dynamically injected vertex attributes to find the vertex count
            if(!attributeData.isEmpty())
                return attributeData.size() / vertexAttribute.size;
        }

        return 0;
    }

    public static Mesh newTestQuad() {
        Map<VertexAttributes.Type, List<Float>> vertexData = new HashMap<>();
        vertexData.put(VertexAttributes.Type.PositionFloat3, new ArrayList<>());
        vertexData.put(VertexAttributes.Type.UVFloat2, new ArrayList<>());


        ArrayList<Integer> indices = new ArrayList<>();

        List<Float> vertices = vertexData.get(VertexAttributes.Type.PositionFloat3);
        List<Float> textureUVs = vertexData.get(VertexAttributes.Type.UVFloat2);


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

        textureUVs.add(1.0f);
        textureUVs.add(0.0f);

        textureUVs.add(1.0f);
        textureUVs.add(1.0f);

        textureUVs.add(0.0f);
        textureUVs.add(1.0f);



        return new Mesh(vertexData, indices, 4);

    }

    private static void openMesh(AIMesh mesh, VertexAttributes.Type[] vertexAttributes, Map<VertexAttributes.Type, List<Float>> vertexData, List<Integer> indices){
        for (int faceIndex = 0; faceIndex < mesh.mNumFaces(); faceIndex++) {
            int vertexCount = getVertexCount(vertexData);



            AIFace aiFace = mesh.mFaces().get(faceIndex);
            IntBuffer indicesBuffer = aiFace.mIndices();

            for (int k = 0; k < aiFace.mNumIndices(); k++) {
                int index = indicesBuffer.get(k);
                indices.add(vertexCount + index);
            }
        }

        AIVector3D.Buffer aiVertices = mesh.mVertices();

        for (int vertexIndex = 0; vertexIndex < mesh.mNumVertices(); vertexIndex++) {

            for(VertexAttributes.Type vertexAttribute : vertexAttributes) {
                if(vertexAttribute == VertexAttributes.Type.PositionFloat3) {
                    AIVector3D aiVertex = aiVertices.get();
                    List<Float> vertices = vertexData.get(vertexAttribute);
                    vertices.add(aiVertex.x());
                    vertices.add(aiVertex.y());
                    vertices.add(aiVertex.z());
                }

                if(vertexAttribute == VertexAttributes.Type.UVFloat2) {
                    AIVector3D aiTextureCoords = mesh.mTextureCoords(0).get(vertexIndex);
                    List<Float> textureUVs = vertexData.get(vertexAttribute);
                    textureUVs.add(aiTextureCoords.x());
                    textureUVs.add(aiTextureCoords.y());
                }


            }


        }
    }
    private static void openNode(AINode parentNode, VertexAttributes.Type[] vertexAttributes, List<AIMesh> meshList, Map<VertexAttributes.Type, List<Float>> vertexData, List<Integer> indices) {
        IntBuffer meshes = parentNode.mMeshes();
        for (int i = 0; i < parentNode.mNumMeshes(); i++) {
            int meshIndex = meshes.get(i);
            AIMesh mesh = meshList.get(meshIndex);

            openMesh(mesh, vertexAttributes, vertexData, indices);
        }

        for (int z = 0; z < parentNode.mNumChildren(); z++) {
            AINode child = AINode.create(parentNode.mChildren().get(z));
            openNode(child, vertexAttributes, meshList, vertexData, indices);
        }


    }



    public int getVertexCount() {
        return vertexCount;
    }

    public int getIndexCount() {
        return indices.size();
    }
}
