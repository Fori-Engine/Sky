package noir.citizens;

import fori.Logger;
import fori.asset.AssetPacks;
import fori.ecs.*;
import fori.graphics.*;

import java.nio.ByteBuffer;

public class RenderSystem extends EntitySystem {

    private Renderer renderer;
    private Camera camera;
    private Texture defaultTexture;


    public RenderSystem(Renderer renderer) {
        this.renderer = renderer;
        defaultTexture = Texture.newTexture(
                renderer.getRef(),
                AssetPacks.getAsset("core:assets/textures/default.png"),
                Texture.Filter.Linear,
                Texture.Filter.Linear
        );
    }


    @Override
    public void update(Engine ecs, MessageQueue messageQueue) {


        ecs.view(CameraComponent.class, cameraComponent -> RenderSystem.this.camera = cameraComponent.camera);

        ecs.view(MeshComponent.class, meshComponent -> {
            if(renderer.getRenderQueueByShaderProgram(meshComponent.shaderProgram) == null){
                renderer.newRenderQueue(meshComponent.shaderProgram);
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
                            vertexBufferData.putFloat(renderQueue.getMeshIndex());
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







                for (int i = 0; i < renderQueue.getFramesInFlight(); i++) {
                    ByteBuffer transformsBufferData = renderQueue.getTransformsBuffer(i).get();
                    ByteBuffer cameraBufferData = renderQueue.getCameraBuffer(i).get();

                    meshComponent.transform.get(renderQueue.getMeshIndex() * SizeUtil.MATRIX_SIZE_BYTES, transformsBufferData);
                    camera.getView().get(0, cameraBufferData);
                    camera.getProj().get(4 * 4 * Float.BYTES, cameraBufferData);
                }


                renderQueue.updateQueue(
                        meshComponent.mesh.vertexCount,
                        meshComponent.mesh.indices.size()
                );

                meshComponent.queueIndex = renderQueue.getMeshIndex();
                meshComponent.queued = true;


                renderQueue.nextMesh();
            }

            if(meshComponent.queued){
                ByteBuffer transformsBufferData = renderQueue.getTransformsBuffer(renderer.getFrameIndex()).get();
                meshComponent.transform.get(meshComponent.queueIndex * SizeUtil.MATRIX_SIZE_BYTES, transformsBufferData);
            }












        });





    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
