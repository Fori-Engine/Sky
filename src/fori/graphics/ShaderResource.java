package fori.graphics;

public class ShaderResource {
    public int binding;

    public int sizeBytes;
    public enum Type {
        UniformBuffer,
        CombinedSampler
    }

    public enum ShaderStage {
        VertexStage,
        FragmentStage
    }

    public Type type;
    public ShaderStage shaderStage;
    public int count;


    public ShaderResource(int binding) {
        this.binding = binding;
    }

    public ShaderResource type(Type type){
        this.type = type;
        return this;
    }

    public ShaderResource shaderStage(ShaderStage shaderStage){
        this.shaderStage = shaderStage;
        return this;
    }

    public ShaderResource count(int count){
        this.count = count;
        return this;
    }

    public ShaderResource sizeBytes(int sizeBytes){
        this.sizeBytes = sizeBytes;
        return this;
    }


}
