package fori.graphics;

import fori.graphics.vulkan.VkShaderProgram;


public abstract class ShaderProgram implements Disposable {

    protected String vertexShaderSource = null;
    protected String fragmentShaderSource = null;
    protected ShaderResSet[] resourcesSets;


    public ShaderProgram(String vertexShaderSource, String fragmentShaderSource){
        this.vertexShaderSource = vertexShaderSource;
        this.fragmentShaderSource = fragmentShaderSource;
    }

    public ShaderProgram(ShaderResSet[] resourceSets) {
    }

    public abstract void update(int frameIndex, ShaderUpdate<Buffer>... bufferUpdates);

    public void bind(ShaderResSet... resourceSets){
        this.resourcesSets = resourceSets;
    }
    public abstract void dispose();
    public static ShaderProgram newShaderProgram(SceneRenderer sceneRenderer, String vertexShaderSource, String fragmentShaderSource){
        if(SceneRenderer.getRenderAPI() == RenderAPI.Vulkan){
            return new VkShaderProgram(sceneRenderer, vertexShaderSource, fragmentShaderSource);
        }

        return null;
    }


}
