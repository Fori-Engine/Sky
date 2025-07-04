package fori.graphics;

import fori.graphics.vulkan.VulkanShader;

public abstract class Shader extends Disposable {
    protected ShaderType shaderType;
    protected ShaderBinary bytecode;

    public Shader(Disposable parent, ShaderType shaderType, ShaderBinary bytecode) {
        super(parent);
        this.shaderType = shaderType;
        this.bytecode = bytecode;
    }

    public ShaderType getShaderType() {
        return shaderType;
    }

    public static Shader newShader(Disposable parent, ShaderType shaderType, ShaderBinary shaders){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan){
            return new VulkanShader(parent, shaderType, shaders);
        }

        return null;
    }

}
