package fori.graphics;

import fori.asset.Asset;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.aiProcess_FlipUVs;
import static org.lwjgl.assimp.Assimp.aiProcess_Triangulate;

public class Mesh {
    public List<Float> vertices;
    public List<Float> textureUVs;
    public List<Integer> indices;
    public int vertexCount;

    public Mesh(List<Float> vertices, List<Float> textureUVs, List<Integer> indices, int vertexCount) {
        this.vertices = vertices;
        this.textureUVs = textureUVs;
        this.indices = indices;
        this.vertexCount = vertexCount;
    }

    public static Mesh newMesh(Asset<byte[]> asset){
        return newMeshFromObj(asset);
        //else if(asset.name.endsWith(".noir")) return newMeshFromNoir(asset);


    }

    private static Mesh newMeshFromNoir(Asset<byte[]> asset) {
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Float> textureUVs = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        int vertexCount = 0;

        DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(asset.asset));


        return new Mesh(vertices, textureUVs, indices, vertexCount);
    }

    private static Mesh newMeshFromObj(Asset<byte[]> asset) {
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Float> textureUVs = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();

        ArrayList<AIMesh> meshList = new ArrayList<>();


        ByteBuffer assetData = MemoryUtil.memAlloc(asset.asset.length);
        assetData.put(asset.asset);
        assetData.flip();




        AIScene scene = Assimp.aiImportFileFromMemory(assetData, aiProcess_FlipUVs | aiProcess_Triangulate, (ByteBuffer) null);
        int meshCount = scene.mNumMeshes();
        PointerBuffer meshes = scene.mMeshes();

        for (int i = 0; i < meshCount; i++) meshList.add(AIMesh.create(meshes.get(i)));

        System.out.println("Mesh Count: " + meshCount);

        AINode root = scene.mRootNode();
        openNode(root, meshList, vertices, textureUVs, indices);
        System.out.println("Vertex Count: " + vertices.size() / 3);
        System.out.println("Index Count: " + indices.size() );

        return new Mesh(vertices, textureUVs, indices, vertices.size() / 3);
    }

    private static void openNode(AINode root, List<AIMesh> meshList, List<Float> vertices, List<Float> textureUVs, List<Integer> indices) {

        IntBuffer meshes = root.mMeshes();
        for (int i = 0; i < root.mNumMeshes(); i++) {
            int meshIndex = meshes.get(i);
            AIMesh mesh = meshList.get(meshIndex);

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
            }


        }


        for (int z = 0; z < root.mNumChildren(); z++) {
            AINode child = AINode.create(root.mChildren().get(z));
            openNode(child, meshList, vertices, textureUVs, indices);
        }






    }


}
