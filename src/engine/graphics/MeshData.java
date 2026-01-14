package engine.graphics;

import engine.Logger;
import engine.asset.Asset;
import engine.asset.AssetRegistry;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class MeshData {
    private Map<String, List<Float>> data;
    private int vertexCount;
    private List<Integer> indices;


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


    private static AIFile file;
    private static long cursor = 0;

    public static MeshData newMeshFromGLTF2(Asset<byte[]> asset){
        /*
        HashMap<String, List<Float>> data = new HashMap<>();

        data.put("Positions", new LinkedList<>());
        data.put("TextureUVs", new LinkedList<>());

        List<Integer> indices = new LinkedList<>();
        List<AIMesh> meshList = new LinkedList<>();

        AIFileIO fileIO;
        try (MemoryStack stack = stackPush()) {

            {


                fileIO = AIFileIO.calloc(stack);
                fileIO.set(
                        (pAIFileIO, pFileName, pOpenMode) -> {
                            String assetPath = asset.namespace + ":" + AssetUtil.getAssetPath(MemoryUtil.memUTF8(pFileName).replace("/", ""));
                            Asset<byte[]> fileAsset = AssetRegistry.getAsset(assetPath);

                            file = AIFile.create();
                            file.ReadProc((pFile, pBuffer, size, count) -> {
                                ByteBuffer buffer = MemoryUtil.memByteBuffer(pBuffer, (int) (size * count));
                                buffer.position((int) cursor);

                                for(int t = 0; t < count; t++) {
                                    buffer.put((int) (t * size), fileAsset.asset[(int) (t * size)]);
                                }


                                return count;
                            });
                            file.WriteProc((pFile, pBuffer, memB, count) -> {
                                return 0;
                            });
                            file.SeekProc((pFile, offset, origin) -> {
                                //SEEK_SET
                                if(origin == 0) cursor = offset;
                                //SEEK_CUR
                                else if(origin == 1) cursor += offset;
                                //SEEK_END
                                else if(origin == 2) cursor = fileAsset.asset.length + offset;
                                return 0;
                            });
                            file.TellProc(_ -> cursor);
                            file.FileSizeProc(_ -> fileAsset.asset.length);


                            return file.address();
                        },
                        (_, _) -> file.free(),
                        0
                );

            }



            AIScene scene = Assimp.aiImportFileEx(stack.UTF8(asset.path), aiProcess_FlipUVs | aiProcess_Triangulate | aiProcess_JoinIdenticalVertices, fileIO);

            int meshCount = scene.mNumMeshes();
            PointerBuffer meshes = scene.mMeshes();

            for (int i = 0; i < meshCount; i++) meshList.add(AIMesh.create(meshes.get(i)));


            AINode root = scene.mRootNode();
            openNode(root, meshList, data, indices);
            Logger.info(MeshData.class, "Loaded mesh " + asset.path);
        }

        return new MeshData(data, indices, data.get("Positions").size() / 3);

         */

        return null;
    }

    private static void openNode(AINode node, List<AIMesh> meshList, Map<String, List<Float>> data, List<Integer> indices) {
        IntBuffer meshes = node.mMeshes();
        for (int i = 0; i < node.mNumMeshes(); i++) {
            int meshIndex = meshes.get(i);
            AIMesh mesh = meshList.get(meshIndex);
            openMesh(mesh, data, indices);
        }

        for (int z = 0; z < node.mNumChildren(); z++) {
            AINode child = AINode.create(node.mChildren().get(z));
            openNode(child, meshList, data, indices);
        }
    }

    private static void openMesh(AIMesh mesh, Map<String, List<Float>> data, List<Integer> indices){

        List<Float> positionsList = data.get("Positions");
        List<Float> textureUVsList = data.get("TextureUVs");

        for (int faceIndex = 0; faceIndex < mesh.mNumFaces(); faceIndex++) {
            int vertexCount = positionsList.size() / 3;

            AIFace aiFace = mesh.mFaces().get(faceIndex);
            IntBuffer indicesBuffer = aiFace.mIndices();

            for (int k = 0; k < aiFace.mNumIndices(); k++) {
                int index = indicesBuffer.get(k);
                indices.add(vertexCount + index);
            }
        }

        AIVector3D.Buffer aiVertices = mesh.mVertices();
        //AIVector3D.Buffer aiTextureCoords = mesh.mTextureCoords(0);

        for (int vertexIndex = 0; vertexIndex < mesh.mNumVertices(); vertexIndex++) {
            AIVector3D aiVertex = aiVertices.get();
            //AIVector3D aiTextureCoord = aiTextureCoords.get();

            positionsList.add(aiVertex.x());
            positionsList.add(aiVertex.y());
            positionsList.add(aiVertex.z());

            textureUVsList.add(0f);
            textureUVsList.add(0f);

        }
    }

    public int getVertexCount() {
        return vertexCount;
    }
    public int getIndexCount() {
        return indices.size();
    }
}
