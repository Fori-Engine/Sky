package fori.graphics;

import fori.graphics.vulkan.VulkanShader;

public abstract class Shader extends Disposable {
    protected ShaderType shaderType;
    protected ShaderBinary bytecode;
    protected VertexAttributes.Type[] vertexAttributes;
    protected TextureFormatType[] attachmentTextureFormatTypes;
    protected TextureFormatType depthAttachmentTextureFormatType;

    public Shader(Disposable parent, ShaderType shaderType, ShaderBinary bytecode) {
        super(parent);
        this.shaderType = shaderType;
        this.bytecode = bytecode;
    }

    public VertexAttributes.Type[] getVertexAttributes() {
        return vertexAttributes;
    }

    public Shader setVertexAttributes(VertexAttributes.Type[] vertexAttributes) {
        this.vertexAttributes = vertexAttributes;
        return this;
    }

    public TextureFormatType[] getAttachmentTextureFormatTypes() {
        return attachmentTextureFormatTypes;
    }

    public Shader setAttachmentTextureFormatTypes(TextureFormatType... attachmentTextureFormatTypes) {
        this.attachmentTextureFormatTypes = attachmentTextureFormatTypes;
        return this;
    }

    public TextureFormatType getDepthAttachmentTextureFormatType() {
        return depthAttachmentTextureFormatType;
    }

    public Shader setDepthAttachmentTextureFormatType(TextureFormatType depthAttachmentTextureFormatType) {
        this.depthAttachmentTextureFormatType = depthAttachmentTextureFormatType;
        return this;
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
