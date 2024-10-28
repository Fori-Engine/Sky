package fori.graphics;

import fori.Logger;
import fori.graphics.vulkan.VkBuffer;

import java.nio.ByteBuffer;

public abstract class Buffer implements Disposable {
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

    private Usage usage;
    private Type type;

    private int sizeBytes;
    private boolean mapped;
    protected boolean staging;
    private ByteBuffer data;
    protected Ref ref;

    public Buffer(Ref parent, int sizeBytes, Usage usage, Type type, boolean staging){
        ref = parent.add(this);
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

    public static Buffer newBuffer(Ref parent, int sizeBytes, Usage usage, Type type, boolean staging){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan) return new VkBuffer(parent, sizeBytes, usage, type, staging);

        return null;
    }

    @Override
    public Ref getRef() {
        return ref;
    }
}
