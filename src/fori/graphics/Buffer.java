package fori.graphics;

import fori.Logger;
import fori.graphics.vulkan.VulkanBuffer;

import java.nio.ByteBuffer;

public abstract class Buffer extends Disposable {
    public enum Type {
        GPULocal,
        CPUGPUShared
    }

    public enum Usage {
        VertexBuffer,
        IndexBuffer,
        UniformBuffer,
        ShaderStorageBuffer,
        ImageBackingBuffer
    }

    protected Usage usage;
    protected Type type;

    protected int sizeBytes;
    protected boolean mapped;
    protected boolean staging;
    protected ByteBuffer data;

    public Buffer(Disposable parent, int sizeBytes, Usage usage, Type type, boolean staging){
        super(parent);
        this.sizeBytes = sizeBytes;
        this.usage = usage;
        this.type = type;
        this.staging = staging;
    }

    public Usage getUsage() {
        return usage;
    }

    public Type getType() {
        return type;
    }

    public ByteBuffer get(){
        if(!mapped) data = map();
        return data;
    }

    public ByteBuffer map(){

        if(mapped) {
            throw new RuntimeException(
                    Logger.error(
                            Buffer.class,
                            "This Buffer cannot be remapped!"
                    ));
        }
        mapped = true;

        return null;
    }

    public boolean isMapped() {
        return mapped;
    }

    public void unmap(){
        mapped = false;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public static Buffer newBuffer(Disposable parent, int sizeBytes, Usage usage, Type type, boolean staging){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan) return new VulkanBuffer(parent, sizeBytes, usage, type, staging);

        return null;
    }

}
