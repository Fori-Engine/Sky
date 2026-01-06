package fori.graphics;

public class Descriptor {

    private String name;
    private int binding;
    private int sizeBytes;
    private DescriptorSet descriptorSet;




    public enum Type {
        ShaderStorageBuffer,
        UniformBuffer,
        CombinedTextureSampler,
        StorageTexture,
        SeparateTexture,
        SeparateSampler
    }

    public enum ShaderStage {
        VertexStage,
        FragmentStage,
        ComputeStage,
        AllStages
    }

    private Type type;
    private ShaderStage shaderStage;
    private int count = 1;

    public Descriptor(String name, int binding, Type type, ShaderStage stage) {
        this.name = name;
        this.binding = binding;
        this.type = type;
        this.shaderStage = stage;
    }

    public Type getType() {
        return type;
    }

    protected void setDescriptorSet(DescriptorSet set) {
        this.descriptorSet = set;
    }

    public ShaderStage getShaderStage() {
        return shaderStage;
    }

    public int getCount() {
        return count;
    }

    public String getName() {
        return name;
    }

    public int getBinding() {
        return binding;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public DescriptorSet getDescriptorSet() {
        return descriptorSet;
    }

    public Descriptor type(Type type){
        this.type = type;
        return this;
    }

    public Descriptor shaderStage(ShaderStage shaderStage){
        this.shaderStage = shaderStage;
        return this;
    }

    public Descriptor count(int count){
        this.count = count;
        return this;
    }

    public Descriptor sizeBytes(int sizeBytes){
        this.sizeBytes = sizeBytes;
        return this;
    }


}
