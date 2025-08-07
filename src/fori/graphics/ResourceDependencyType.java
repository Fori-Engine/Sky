package fori.graphics;

public class ResourceDependencyType {

    public static final int RenderTargetRead = 1;
    public static final int RenderTargetWrite = 1 << 1;
    public static final int FragmentShaderRead = 1 << 2;
    public static final int FragmentShaderWrite = 1 << 3;

    private ResourceDependencyType(){}
}
