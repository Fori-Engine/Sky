package fori.graphics;

import fori.Logger;
import fori.graphics.vulkan.VulkanShaderProgram;

import java.util.Optional;


public abstract class ShaderProgram extends Disposable {

    protected Shader[] shaders;
    protected ShaderResSet[] resourcesSets;
    protected Optional<VertexAttributes.Type[]> attributes;
    protected TextureFormatType colorTextureFormat;
    protected TextureFormatType depthTextureFormat;
    protected ShaderProgramType type;


    public ShaderProgram(Disposable parent, ShaderProgramType type, TextureFormatType colorTextureFormat, TextureFormatType depthTextureFormat){
        super(parent);
        this.colorTextureFormat = colorTextureFormat;
        this.depthTextureFormat = depthTextureFormat;
        this.type = type;

    }

    public ShaderProgram(Disposable parent, ShaderProgramType type){
        super(parent);
        this.type = type;
    }


    public abstract void setShaders(Shader... shaders);
    public abstract void updateBuffers(int frameIndex, ShaderUpdate<Buffer>... bufferUpdates);
    public abstract void updateTextures(int frameIndex, ShaderUpdate<Texture>... textureUpdates);

    public abstract void bind(Optional<VertexAttributes.Type[]> attributes, ShaderResSet... resourceSets);

    public ShaderResSet[] getShaderResSets(){
        return resourcesSets;
    }

    public Optional<VertexAttributes.Type[]> getAttributes() {
        return attributes;
    }

    public static ShaderProgram newGraphicsShaderProgram(Disposable parent, TextureFormatType colorTextureFormat, TextureFormatType depthTextureFormat){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan){
            return new VulkanShaderProgram(parent, ShaderProgramType.Graphics, colorTextureFormat, depthTextureFormat);
        }
        return null;
    }
    public static ShaderProgram newComputeShaderProgram(Disposable parent) {
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan){
            return new VulkanShaderProgram(parent, ShaderProgramType.Compute);
        }
        return null;
    }


}
