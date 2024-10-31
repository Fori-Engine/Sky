package noir.citizens;

import fori.Logger;
import fori.Scene;
import fori.asset.AssetPacks;
import fori.ecs.*;
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
        int maxEntities = 10;


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
                    SizeUtil.MATRIX_SIZE_BYTES * maxEntities,
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
                RenderQueue renderQueue = renderer.newRenderQueue(meshComponent.shaderProgram);

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

                        else if(attribute == Attributes.Type.TransformIndexFloat1) {
                            vertexBufferData.putFloat(entity.getID());
                        }

                        else if(attribute == Attributes.Type.MaterialBaseIndexFloat1) {
                            vertexBufferData.putFloat(meshComponent.mesh.textureIndices.get(vertex));
                        }

                        else if(attribute == Attributes.Type.RenderQueuePosFloat1) {
                            vertexBufferData.putFloat(renderQueue.getMeshIndex());
                        }



                    }
                }

                for(int index : meshComponent.mesh.indices)
                    indexBufferData.putInt(renderQueue.getVertexCount() + index);


                /*
                int maxTextures = Material.MAX_MATERIALS * Material.SIZE;


                Material[] materials = meshComponent.materials;

                int textureIndex = renderQueue.getMeshIndex() * maxTextures;



                for (int i = 0; i < materials.length; i++) {
                    Material material = materials[i];
                    Texture albedo = material.getAlbedo();
                    Texture metallic = material.getMetallic();
                    Texture normal = material.getNormal();
                    Texture roughness = material.getRoughness();

                    if(albedo == null) {
                        albedo = defaultTexture;
                        Logger.error(RenderSystem.class, "Material [" + material.getName() + "] does not have an albedo texture configured");
                    }
                    if(metallic == null) {
                        metallic = defaultTexture;
                        Logger.error(RenderSystem.class, "Material [" + material.getName() + "] does not have a metallic texture configured");
                    }
                    if(normal == null) {
                        normal = defaultTexture;
                        Logger.error(RenderSystem.class, "Material [" + material.getName() + "] does not have a normal texture configured");
                    }
                    if(roughness == null) {
                        roughness = defaultTexture;
                        Logger.error(RenderSystem.class, "Material [" + material.getName() + "] does not have a roughness texture configured");
                    }

                    renderQueue.addTexture(textureIndex++, albedo);
                    renderQueue.addTexture(textureIndex++, metallic);
                    renderQueue.addTexture(textureIndex++, normal);
                    renderQueue.addTexture(textureIndex++, roughness);
                }

                 */







                renderer.waitForDevice();

                Matrix4f combinedTransform = getCombinedTransform(scene, entity);
                System.out.println("Combined Transform (" + entity.getTag() + ")\n" + combinedTransform);

                for (int i = 0; i < renderQueue.getFramesInFlight(); i++) {
                    ByteBuffer transformsBufferData = transformsBuffers[i].get();
                    ByteBuffer cameraBufferData = cameraBuffers[i].get();


                    combinedTransform.get(entity.getID() * SizeUtil.MATRIX_SIZE_BYTES, transformsBufferData);



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
                combinedTransform.get(entity.getID() * SizeUtil.MATRIX_SIZE_BYTES, transformsBufferData);
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
        System.out.println(combinedTransform);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
