package fori.graphics;

import fori.Logger;
import fori.graphics.vulkan.VkShaderProgram;


public abstract class ShaderProgram implements Disposable {

    protected String vertexShaderSource = null;
    protected String fragmentShaderSource = null;
    protected ShaderResSet[] resourcesSets;
    protected Attributes.Type[] attributes;
    protected Ref ref;


    public ShaderProgram(Ref parent, String vertexShaderSource, String fragmentShaderSource){
        ref = parent.add(this);
        this.vertexShaderSource = vertexShaderSource;
        this.fragmentShaderSource = fragmentShaderSource;
    }

    public abstract void updateBuffers(int frameIndex, ShaderUpdate<Buffer>... bufferUpdates);
    public abstract void updateTextures(int frameIndex, ShaderUpdate<Texture>... textureUpdates);

    public void bind(Attributes.Type[] attributes, ShaderResSet... resourceSets){

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

    public Attributes.Type[] getAttributes() {
        return attributes;
    }

    public abstract void dispose();
    public static ShaderProgram newShaderProgram(Ref parent, String vertexShaderSource, String fragmentShaderSource){
        if(Renderer.getRenderAPI() == RenderAPI.Vulkan){
            return new VkShaderProgram(parent, vertexShaderSource, fragmentShaderSource);
        }

        return null;
    }


}
