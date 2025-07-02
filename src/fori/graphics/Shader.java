package fori.graphics;

import fori.graphics.vulkan.VulkanShader;

public abstract class Shader implements Disposable {
    protected ShaderType shaderType;
    protected ShaderBinary bytecode;
    protected Ref ref;

    public Shader(Ref parent, ShaderType shaderType, ShaderBinary bytecode) {
        parent.add(this);
        this.shaderType = shaderType;
        this.bytecode = bytecode;
    }

    public ShaderType getShaderType() {
        return shaderType;
    }

    public abstract void dispose();
    public static Shader newShader(Ref parent, ShaderType shaderType, ShaderBinary shaders){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan){
            return new VulkanShader(parent, shaderType, shaders);
        }

        return null;
    }

    @Override
    public Ref getRef() {
        return null;
    }
}
