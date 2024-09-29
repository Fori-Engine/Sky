package fori.graphics;

import fori.graphics.vulkan.VkShaderProgram;


public abstract class ShaderProgram implements Disposable {

    protected String vertexShaderSource = null;
    protected String fragmentShaderSource = null;


    public ShaderProgram(String vertexShaderSource, String fragmentShaderSource){
        this.vertexShaderSource = vertexShaderSource;
        this.fragmentShaderSource = fragmentShaderSource;
    }


    public abstract void bind(ShaderResSet... resourceSets);
    public abstract void dispose();

    public static ShaderProgram newShaderProgram(SceneRenderer sceneRenderer, String vertexShaderSource, String fragmentShaderSource){
        if(SceneRenderer.getRenderAPI() == RenderAPI.Vulkan){
            return new VkShaderProgram(sceneRenderer, vertexShaderSource, fragmentShaderSource);
        }

        return null;
    }


}
