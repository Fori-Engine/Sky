package fori.graphics;

import fori.Logger;
import fori.asset.Asset;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;

public class Mesh {
    public List<Float> vertices;
    public List<Float> textureUVs;
    public List<Integer> indices;
    public List<Texture> textures;
    public int vertexCount;


    public Mesh(List<Float> vertices, List<Float> textureUVs, List<Integer> indices, List<Texture> textures, int vertexCount) {
        this.vertices = vertices;
        this.textureUVs = textureUVs;
        this.indices = indices;
        this.vertexCount = vertexCount;
        this.textures = textures;
    }

    public static Mesh newMesh(Asset<byte[]> asset){
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Float> textureUVs = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        ArrayList<Texture> textures = new ArrayList<>();

        ArrayList<AIMesh> meshList = new ArrayList<>();


        ByteBuffer assetData = MemoryUtil.memAlloc(asset.asset.length);
        assetData.put(asset.asset);
        assetData.flip();




        AIScene scene = Assimp.aiImportFileFromMemory(assetData, aiProcess_FlipUVs | aiProcess_Triangulate | aiProcess_JoinIdenticalVertices, (ByteBuffer) null);


        int materialCount = scene.mNumMaterials();

        try(MemoryStack stack = MemoryStack.stackPush()) {

            for (int i = 0; i < materialCount; i++) {
                AIMaterial material = AIMaterial.create(scene.mMaterials().get(i));
                AIString aiTexturePath = AIString.calloc(stack);
                aiGetMaterialTexture(material, aiTextureType_DIFFUSE, 0, aiTexturePath, (IntBuffer) null,
                        null, null, null, null, null);
                String texturePath = aiTexturePath.dataString();

            }

        }


        int meshCount = scene.mNumMeshes();
        PointerBuffer meshes = scene.mMeshes();

        for (int i = 0; i < meshCount; i++) meshList.add(AIMesh.create(meshes.get(i)));

        StringBuilder loadData = new StringBuilder();
        loadData.append("Loading Mesh: " + asset.name);





        AINode root = scene.mRootNode();
        openNode(root, meshList, vertices, textureUVs, indices);
        loadData.append("\n\t" + "Vertex Count: " + vertices.size() / Attributes.Type.PositionFloat3.size);
        loadData.append("\n\t" + "Index Count: " + indices.size());

        Logger.info(Mesh.class, loadData.toString());


        return new Mesh(vertices, textureUVs, indices, textures, vertices.size() / 3);
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
