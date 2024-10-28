package noir.citizens;

import fori.ecs.*;
import fori.graphics.*;

import java.nio.ByteBuffer;
import java.util.List;

public class RenderSystem extends EntitySystem {

    private Renderer renderer;
    private Camera camera;



    public RenderSystem(Renderer renderer) {
        this.renderer = renderer;
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

                        if(attribute == Attributes.Type.UVFloat2){
                            float u = meshComponent.mesh.textureUVs.get(attribute.size * vertex);
                            float v = meshComponent.mesh.textureUVs.get(attribute.size * vertex + 1);

                            vertexBufferData.putFloat(u);
                            vertexBufferData.putFloat(v);
                        }

                        if(attribute == Attributes.Type.TransformIndexFloat1) {
                            vertexBufferData.putFloat(renderQueue.getMeshIndex());
                        }

                        if(attribute == Attributes.Type.MaterialBaseIndexFloat1) {
                            vertexBufferData.putFloat(renderQueue.getMeshIndex());
                        }




                    }
                }

                for(int index : meshComponent.mesh.indices)
                    indexBufferData.putInt(renderQueue.getVertexCount() + index);


                renderQueue.addTexture(renderQueue.getMeshIndex(), meshComponent.texture);


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
