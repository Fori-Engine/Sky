package fori.ecs;

import fori.Scene;
import fori.asset.AssetPacks;
import fori.graphics.*;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;

public class RenderSystem extends EntitySystem {

    private Renderer renderer;
    private Camera camera;
    private Texture defaultTexture;
    private Buffer[] lightsBuffers;
    private Buffer[] transformsBuffers;
    private Buffer[] cameraBuffers;


    public RenderSystem(Renderer renderer) {
        this.renderer = renderer;
        defaultTexture = Texture.newTexture(
                renderer.getRef(),
                AssetPacks.getAsset("core:assets/textures/default.png"),
                Texture.Filter.Linear,
                Texture.Filter.Linear
        );

        lightsBuffers = new Buffer[renderer.getMaxFramesInFlight()];
        transformsBuffers = new Buffer[renderer.getMaxFramesInFlight()];
        cameraBuffers = new Buffer[renderer.getMaxFramesInFlight()];

        int maxLights = 10;

        for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
            lightsBuffers[i] = Buffer.newBuffer(
                    renderer.getRef(),
                    Light.SIZE * maxLights,
                    Buffer.Usage.ShaderStorageBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );

            transformsBuffers[i] = Buffer.newBuffer(
                    renderer.getRef(),
                    SizeUtil.MATRIX_SIZE_BYTES * (RenderQueue.MAX_MESH_COUNT * renderer.getMaxRenderQueueCount()),
                    Buffer.Usage.ShaderStorageBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );

            cameraBuffers[i] = Buffer.newBuffer(
                    renderer.getRef(),
                    Camera.SIZE,
                    Buffer.Usage.UniformBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );
        }

    }


    @Override
    public void update(Scene scene, MessageQueue messageQueue) {


        scene.view(CameraComponent.class, (entity, cameraComponent) -> RenderSystem.this.camera = cameraComponent.camera);

        scene.view(MeshComponent.class,  (entity, meshComponent) -> {
            if(renderer.getRenderQueueByShaderProgram(meshComponent.shaderProgram) == null){

                RenderQueueFlags renderQueueFlags = new RenderQueueFlags();
                renderQueueFlags.shaderProgram = meshComponent.shaderProgram;
                renderQueueFlags.maxVertices = RenderQueue.MAX_VERTEX_COUNT;
                renderQueueFlags.maxIndices = RenderQueue.MAX_INDEX_COUNT;
                renderQueueFlags.depthOp = RenderQueueFlags.DepthOp.LessThan;
                renderQueueFlags.depthTest = true;



                RenderQueue renderQueue = renderer.newRenderQueue(renderQueueFlags);

                for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                    renderQueue.getShaderProgram().updateBuffers(
                            i,
                            new ShaderUpdate<>("camera", 0, 0, cameraBuffers[i]),
                            new ShaderUpdate<>("transforms", 0, 1, transformsBuffers[i])
                    );
                }

            }

            RenderQueue renderQueue = renderer.getRenderQueueByShaderProgram(meshComponent.shaderProgram);
            if(renderQueue.getMeshCount() != RenderQueue.MAX_MESH_COUNT && !meshComponent.queued){


                ByteBuffer vertexBufferData = renderQueue.getDefaultVertexBuffer().get();
                vertexBufferData.clear();

                ByteBuffer indexBufferData = renderQueue.getDefaultIndexBuffer().get();
                indexBufferData.clear();

                meshComponent.queueIndex = renderQueue.getMeshIndex();

                for (int vertex = 0; vertex < meshComponent.mesh.vertexCount; vertex++) {


                    for(Attributes.Type attribute : meshComponent.shaderProgram.getAttributes()){
                        if(attribute == Attributes.Type.PositionFloat3){
                            float x = meshComponent.mesh.vertices.get(attribute.size * vertex);
                            float y = meshComponent.mesh.vertices.get(attribute.size * vertex + 1);
                            float z = meshComponent.mesh.vertices.get(attribute.size * vertex + 2);

                            vertexBufferData.putFloat(x);
                            vertexBufferData.putFloat(y);
                            vertexBufferData.putFloat(z);
                        }

                        else if(attribute == Attributes.Type.UVFloat2){
                            float u = meshComponent.mesh.textureUVs.get(attribute.size * vertex);
                            float v = meshComponent.mesh.textureUVs.get(attribute.size * vertex + 1);

                            vertexBufferData.putFloat(u);
                            vertexBufferData.putFloat(v);
                        }

                        else if(attribute == Attributes.Type.TransformIndexFloat1) vertexBufferData.putFloat(meshComponent.queueIndex);
                        else if(attribute == Attributes.Type.MaterialBaseIndexFloat1) vertexBufferData.putFloat(meshComponent.queueIndex);
                        else if(attribute == Attributes.Type.RenderQueuePosFloat1) vertexBufferData.putFloat(meshComponent.queueIndex);



                    }
                }

                for(int index : meshComponent.mesh.indices)
                    indexBufferData.putInt(renderQueue.getVertexCount() + index);




                renderer.waitForDevice();

                Matrix4f combinedTransform = getCombinedTransform(scene, entity);


                for (int i = 0; i < renderQueue.getFramesInFlight(); i++) {
                    ByteBuffer transformsBufferData = transformsBuffers[i].get();
                    ByteBuffer cameraBufferData = cameraBuffers[i].get();


                    combinedTransform.get(meshComponent.queueIndex * SizeUtil.MATRIX_SIZE_BYTES, transformsBufferData);


                    int index = meshComponent.queueIndex * Material.SIZE;
                    ShaderProgram shaderProgram = meshComponent.shaderProgram;

                    shaderProgram.updateTextures(i, new ShaderUpdate<>("materials", 0, 2, defaultTexture).arrayIndex(index));
                    shaderProgram.updateTextures(i, new ShaderUpdate<>("materials", 0, 2, defaultTexture).arrayIndex(index + 1));
                    shaderProgram.updateTextures(i, new ShaderUpdate<>("materials", 0, 2, defaultTexture).arrayIndex(index + 2));
                    shaderProgram.updateTextures(i, new ShaderUpdate<>("materials", 0, 2, defaultTexture).arrayIndex(index + 3));


                    camera.getView().get(0, cameraBufferData);
                    camera.getProj().get(4 * 4 * Float.BYTES, cameraBufferData);
                }


                renderQueue.updateQueue(
                        meshComponent.mesh.vertexCount,
                        meshComponent.mesh.indices.size()
                );
                meshComponent.queued = true;


                renderQueue.nextMesh();
            }

            if(meshComponent.queued){
                Matrix4f combinedTransform = getCombinedTransform(scene, entity);
                ByteBuffer transformsBufferData = transformsBuffers[renderer.getFrameIndex()].get();
                combinedTransform.get(meshComponent.queueIndex * SizeUtil.MATRIX_SIZE_BYTES, transformsBufferData);
            }

            if(meshComponent.materialChanged) {
                renderer.waitForDevice();
                for (int i = 0; i < renderQueue.getFramesInFlight(); i++) {
                    int index = meshComponent.queueIndex * Material.SIZE;
                    Material material = meshComponent.material;
                    ShaderProgram shaderProgram = meshComponent.shaderProgram;

                    if (material.getAlbedo() != null)
                        shaderProgram.updateTextures(i, new ShaderUpdate<>("materials", 0, 2, material.getAlbedo()).arrayIndex(index));
                    if (material.getMetallic() != null)
                        shaderProgram.updateTextures(i, new ShaderUpdate<>("materials", 0, 2, material.getMetallic()).arrayIndex(index + 1));
                    if (material.getNormal() != null)
                        shaderProgram.updateTextures(i, new ShaderUpdate<>("materials", 0, 2, material.getNormal()).arrayIndex(index + 2));
                    if (material.getRoughness() != null)
                        shaderProgram.updateTextures(i, new ShaderUpdate<>("materials", 0, 2, material.getRoughness()).arrayIndex(index + 3));

                    meshComponent.materialChanged = false;
                }

            }



        });





    }



    private Matrix4f getCombinedTransform(Scene scene, Entity entity) {
        Matrix4f combinedTransform = new Matrix4f();
        multiplyTransform(scene, entity, combinedTransform);
        return combinedTransform;
    }

    private void multiplyTransform(Scene scene, Entity root, Matrix4f combinedTransform) {
        if(root.getParent() != null) {
            multiplyTransform(scene, root.getParent(), combinedTransform);
        }

        MeshComponent meshComponent = scene.get(root, MeshComponent.class);
        combinedTransform.mul(meshComponent.transform);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
