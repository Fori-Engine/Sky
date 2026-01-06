package fori.graphics;

import fori.Pair;
import fori.asset.Asset;
import fori.graphics.vulkan.VulkanShaderProgram;

import java.util.ArrayList;
import java.util.List;


public abstract class ShaderProgram extends Disposable {

    protected List<DescriptorSet> descriptorSetsSpec = new ArrayList<>();
    protected ShaderPipelineType programType;
    protected List<TextureFormatType> attachmentTextureFormatTypes = new ArrayList<>();
    protected TextureFormatType depthAttachmentTextureFormatType = TextureFormatType.Depth32;
    protected List<VertexAttribute> vertexAttributes = new ArrayList<>();
    protected int pushConstantsSizeBytes;


    public ShaderProgram(Disposable parent){
        super(parent);

    }

    public abstract void setBuffers(int frameIndex, DescriptorUpdate<Buffer>... bufferUpdates);
    public abstract void setCombinedTextureSamplers(int frameIndex, DescriptorUpdate<Pair<Texture, Sampler>>... textureUpdates);
    public abstract void setTextures(int frameIndex, DescriptorUpdate<Texture>... textureUpdates);
    public abstract void setSamplers(int frameIndex, DescriptorUpdate<Sampler>... samplerUpdates);


    public List<DescriptorSet> getDescriptorSetsSpec(){
        return descriptorSetsSpec;
    }

    public List<VertexAttribute> getVertexAttributes() {
        return vertexAttributes;
    }

    public int getVertexAttributesSize() {
        int size = 0;
        for(VertexAttribute vertexAttribute : vertexAttributes) {
            size += vertexAttribute.getSize();
        }
        return size;
    }

    public Descriptor getDescriptorByName(String name) {
        for(DescriptorSet descriptorSet : descriptorSetsSpec) {
            for(Descriptor descriptor : descriptorSet.getDescriptors()) {
                if(descriptor.getName().equals(name)) return descriptor;
            }
        }
        throw new RuntimeException("Unable to find descriptor " + name);
    }

    public static ShaderProgram newShaderProgram(Disposable parent){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan){
            return new VulkanShaderProgram(parent);
        }
        return null;
    }

    public abstract void add(Asset<byte[]> bytecode, ShaderType shaderType);
    public abstract void assemble();
}
