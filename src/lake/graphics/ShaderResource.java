package lake.graphics;

public class ShaderResource {
    public int binding;
    public boolean isUniformBuffer;
    public int sizeBytes;
    public enum Type {
        CombinedSampler,
    }

    public enum ShaderStage {
        VertexStage,
        FragmentStage
    }

    public Type type;
    public ShaderStage shaderStage;
    public int count;



    public ShaderResource(int binding, boolean isUniformBuffer, int sizeBytes) {
        this.binding = binding;
        this.isUniformBuffer = isUniformBuffer;
        this.sizeBytes = sizeBytes;
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


}
