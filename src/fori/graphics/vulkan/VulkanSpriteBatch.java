package fori.graphics.vulkan;

import fori.graphics.*;

import java.nio.ByteBuffer;

public class VulkanSpriteBatch extends SpriteBatch {

    private VulkanPipeline pipeline;
    public VulkanSpriteBatch(Disposable parent, int framesInFlight, VulkanPipeline pipeline, int maxVertexCount, int maxIndexCount, Camera camera, ShaderProgram shaderProgram) {
        super(parent, maxVertexCount, maxIndexCount, shaderProgram, camera);
        this.pipeline = pipeline;

        vertexBuffer = Buffer.newBuffer(
                parent,
                VertexAttributes.getSize(shaderProgram.getAttributes()) * Float.BYTES * this.maxVertexCount,
                Buffer.Usage.VertexBuffer,
                Buffer.Type.CPUGPUShared,
                false
        );
        indexBuffer = Buffer.newBuffer(
                parent,
                this.maxIndexCount * Integer.BYTES,
                Buffer.Usage.IndexBuffer,
                Buffer.Type.CPUGPUShared,
                false
        );

        cameraBuffers = new Buffer[framesInFlight];

        for (int i = 0; i < framesInFlight; i++) {
            cameraBuffers[i] = Buffer.newBuffer(
                    parent,
                    Camera.SIZE,
                    Buffer.Usage.UniformBuffer,
                    Buffer.Type.CPUGPUShared,
                    false
            );
        }


    }



    @Override
    public void start() {

        vertexCount = 0;
        indexCount = 0;

        vertexBuffer.get().clear();
        indexBuffer.get().clear();
    }

    @Override
    public void end() {

    }

    @Override
    public void drawRect(float x, float y, float w, float h, Color color) {
        ByteBuffer vertexBufferData = vertexBuffer.get();
        ByteBuffer indexBufferData = indexBuffer.get();


        vertexBufferData.putFloat(x);
        vertexBufferData.putFloat(y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);

        vertexBufferData.putFloat(x);
        vertexBufferData.putFloat(y + h);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);

        vertexBufferData.putFloat(x + w);
        vertexBufferData.putFloat(y + h);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);

        vertexBufferData.putFloat(x + w);
        vertexBufferData.putFloat(y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);

        indexBufferData.putInt(vertexCount + 0);
        indexBufferData.putInt(vertexCount + 1);
        indexBufferData.putInt(vertexCount + 2);
        indexBufferData.putInt(vertexCount + 2);
        indexBufferData.putInt(vertexCount + 3);
        indexBufferData.putInt(vertexCount + 0);

        vertexCount += 4;
        indexCount += 6;
    }

    public VulkanPipeline getPipeline() {
        return pipeline;
    }
}
