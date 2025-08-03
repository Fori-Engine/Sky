package fori.graphics;

public class ResourceDependencyType {

    public static final int RenderTargetRead = 1;
    public static final int RenderTargetWrite = 1 << 1;
    public static final int ShaderRead = 1 << 2;
    public static final int ShaderWrite = 1 << 3;

    private ResourceDependencyType(){}
}
