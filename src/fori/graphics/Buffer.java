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
        ShaderStorageBuffer
    }

    public Buffer(int sizeBytes, Usage usage, Type type){
        Disposer.add("managedResources", this);
        this.sizeBytes = sizeBytes;


    }

    protected int sizeBytes;
    private boolean mapped;
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
    public void unmap(){
        mapped = false;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public static Buffer newBuffer(int sizeBytes, Usage usage, Type type){
        if(SceneRenderer.getRenderAPI() == RenderAPI.Vulkan) return new VkBuffer(sizeBytes, usage, type);

        return null;
    }



}
