package fori.graphics;

import fori.Logger;
import fori.graphics.vulkan.VulkanShaderProgram;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public abstract class ShaderProgram extends Disposable {

    protected Map<ShaderType, Shader> shaderMap = new HashMap<>();
    protected ShaderResSet[] resourcesSets;
    protected ShaderProgramType type;
    protected int shaderContextSize;


    public ShaderProgram(Disposable parent, ShaderProgramType type, int shaderContextSize){
        super(parent);
        this.type = type;
        this.shaderContextSize = shaderContextSize;
    }


    public abstract void addShader(ShaderType shaderType, Shader shader);
    public abstract void updateBuffers(int frameIndex, ShaderUpdate<Buffer>... bufferUpdates);
    public abstract void updateTextures(int frameIndex, ShaderUpdate<Texture>... textureUpdates);

    public abstract void bind(ShaderResSet... resourceSets);

    public ShaderResSet[] getShaderResSets(){
        return resourcesSets;
    }

    public Map<ShaderType, Shader> getShaderMap() {
        return shaderMap;
    }

    public static ShaderProgram newGraphicsShaderProgram(Disposable parent, int shaderContextSize){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan){
            return new VulkanShaderProgram(parent, ShaderProgramType.Graphics, shaderContextSize);
        }
        return null;
    }
    public static ShaderProgram newComputeShaderProgram(Disposable parent, int shaderContextSize) {
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan){
            return new VulkanShaderProgram(parent, ShaderProgramType.Compute, shaderContextSize);
        }
        return null;
    }


}
