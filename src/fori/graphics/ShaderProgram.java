package fori.graphics;

import fori.Logger;
import fori.graphics.vulkan.VulkanPipeline;
import fori.graphics.vulkan.VulkanShaderProgram;


public abstract class ShaderProgram extends Disposable {

    protected Shader[] shaders;
    protected ShaderResSet[] resourcesSets;
    protected VertexAttributes.Type[] attributes;
    protected RenderTarget renderTarget;

    public ShaderProgram(Disposable parent, RenderTarget renderTarget){
        super(parent);
        this.renderTarget = renderTarget;
    }

    public abstract void setShaders(Shader... shaders);
    public abstract void updateBuffers(int frameIndex, ShaderUpdate<Buffer>... bufferUpdates);
    public abstract void updateTextures(int frameIndex, ShaderUpdate<Texture>... textureUpdates);

    public void bind(VertexAttributes.Type[] attributes, ShaderResSet... resourceSets){

        this.attributes = attributes;


        int i = 0;

        for(ShaderResSet set : resourceSets){
            if(set.set != i) {
                throw new RuntimeException(Logger.error(ShaderProgram.class, "The ShaderResSet (" + set.set + ") is not consecutive with the other sets"));
            }
            i++;
        }

        this.resourcesSets = resourceSets;
    }

    public ShaderResSet[] getShaderResSets(){
        return resourcesSets;
    }

    public VertexAttributes.Type[] getAttributes() {
        return attributes;
    }

    public static ShaderProgram newShaderProgram(Disposable parent, RenderTarget renderTarget){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan){
            return new VulkanShaderProgram(parent, renderTarget);
        }

        return null;
    }

}
