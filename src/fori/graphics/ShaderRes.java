package fori.graphics;

public class ShaderRes {

    public String friendlyName;
    public int binding;
    public int sizeBytes;




    public enum Type {
        ShaderStorageBuffer,
        UniformBuffer,
        CombinedSampler
    }

    public enum ShaderStage {
        VertexStage,
        FragmentStage
    }

    public Type type;
    public ShaderStage shaderStage;
    public int count = 1;

    public ShaderRes(String name, int binding, Type type, ShaderStage stage) {
        this.friendlyName = name;
        this.binding = binding;
        this.type = type;
        this.shaderStage = stage;
    }

    public ShaderRes type(Type type){
        this.type = type;
        return this;
    }

    public ShaderRes shaderStage(ShaderStage shaderStage){
        this.shaderStage = shaderStage;
        return this;
    }

    public ShaderRes count(int count){
        this.count = count;
        return this;
    }

    public ShaderRes sizeBytes(int sizeBytes){
        this.sizeBytes = sizeBytes;
        return this;
    }


}
