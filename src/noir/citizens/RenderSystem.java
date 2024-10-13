package noir.citizens;

import fori.ecs.*;
import fori.graphics.Attributes;
import fori.graphics.Camera;
import fori.graphics.RenderQueue;
import fori.graphics.Renderer;

import java.nio.ByteBuffer;
import java.util.List;

public class RenderSystem extends EntitySystem {

    private Renderer renderer;
    private Camera camera;
    private int matrixSizeBytes = 4 * 4 * Float.BYTES;


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

                for(float vertexPart : meshComponent.vertices) vertexBufferData.putFloat(vertexPart);
                for(int index : meshComponent.indices) {


                    indexBufferData.putInt(renderQueue.getVertexCount() + index);
                }


                renderQueue.addTexture(renderQueue.getMeshIndex(), meshComponent.texture);

                for (int i = 0; i < renderQueue.getFramesInFlight(); i++) {
                    ByteBuffer transformsBufferData = renderQueue.transformsBuffer[i].get();
                    ByteBuffer cameraBufferData = renderQueue.cameraBuffer[i].map();


                    meshComponent.transform.get(renderQueue.getMeshIndex() * matrixSizeBytes, transformsBufferData);
                    camera.getView().get(0, cameraBufferData);
                    camera.getProj().get(4 * 4 * Float.BYTES, cameraBufferData);



                }


                renderQueue.updateQueue(meshComponent.vertices.size() / Attributes.getSize(meshComponent.shaderProgram.getAttributes()), meshComponent.indices.size());

                meshComponent.queueIndex = renderQueue.getMeshIndex();
                meshComponent.queued = true;


                renderQueue.nextMesh();
            }

            if(meshComponent.queued){
                ByteBuffer transformsBufferData = renderQueue.transformsBuffer[renderer.getFrameIndex()].get();
                meshComponent.transform.get(meshComponent.queueIndex * matrixSizeBytes, transformsBufferData);
            }












        });





    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}
