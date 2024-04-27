package lake.graphics.vulkan;

import lake.graphics.ShaderProgram;
import lake.graphics.Disposer;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import static org.lwjgl.util.shaderc.Shaderc.*;
import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;

public class LVKShaderProgram extends ShaderProgram {

    private VkPipelineShaderStageCreateInfo.Buffer shaderStages;
    private VkDevice device;
    private LVKSPRIV vertShaderSPIRV;
    private LVKSPRIV fragShaderSPIRV;

    private ByteBuffer entryPoint;

    private long vertShaderModule;
    private long fragShaderModule;
    public LVKShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        super(vertexShaderSource, fragmentShaderSource);
        Disposer.add("managedResources", this);
    }

    public VkDevice getDevice() {
        return device;
    }

    public void setDevice(VkDevice device) {
        this.device = device;
    }

    public VkPipelineShaderStageCreateInfo.Buffer getShaderStages() {
        return shaderStages;
    }

    @Override
    public void prepare() {
        setDevice(LVKRenderer2D.getDeviceWithIndices().device);
        entryPoint = MemoryUtil.memUTF8("main");


        vertShaderSPIRV = compileShaderToSPIRV(getVertexShaderSource(), shaderc_glsl_vertex_shader);
        fragShaderSPIRV = compileShaderToSPIRV(getFragmentShaderSource(), shaderc_glsl_fragment_shader);

        vertShaderModule = FastVK.createShaderModule(device, vertShaderSPIRV.bytecode);
        fragShaderModule = FastVK.createShaderModule(device, fragShaderSPIRV.bytecode);



        shaderStages = VkPipelineShaderStageCreateInfo.create(2);

        VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);

        vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
        vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
        vertShaderStageInfo.module(vertShaderModule);
        vertShaderStageInfo.pName(entryPoint);

        VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);

        fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
        fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
        fragShaderStageInfo.module(fragShaderModule);
        fragShaderStageInfo.pName(entryPoint);
    }


    public static LVKSPRIV compileShaderToSPIRV(String source, int kind){
        long compiler = shaderc_compiler_initialize();
        if(compiler == MemoryUtil.NULL){
            throw new RuntimeException("Failed to init shaderc compiler");
        }

        long result = shaderc_compile_into_spv(compiler, source, kind, "", "main", MemoryUtil.NULL);
        if(result == MemoryUtil.NULL){
            throw new RuntimeException("Shader compilation failed for " + kind);
        }
        if(shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success){

            String shaderType = getShaderType(kind);



            throw new RuntimeException(shaderType + ": " + shaderc_result_get_error_message(result));
        }

        shaderc_compiler_release(compiler);


        return new LVKSPRIV(result, shaderc_result_get_bytes(result));
    }

    private static String getShaderType(int kind) {
        if(kind == shaderc_glsl_vertex_shader) return "Vertex Shader";
        if(kind == shaderc_glsl_fragment_shader) return "Fragment Shader";


        return null;
    }

    public static class LVKSPRIV {
        public long handle;
        public ByteBuffer bytecode;

        public LVKSPRIV(long handle, ByteBuffer bytecode) {
            this.handle = handle;
            this.bytecode = bytecode;
        }

        public void cleanup() {
            shaderc_result_release(handle);
        }
    }

















    @Override
    public void bind() {

    }

    @Override
    public void setFloat(String name, float value) {

    }

    @Override
    public void setInt(String name, int value) {

    }

    @Override
    public void setMatrix4f(String name, Matrix4f proj) {

    }

    @Override
    public void setIntArray(String name, int[] array) {

    }

    @Override
    public void setVector2fArray(String name, float[] array) {

    }

    public void disposeShaderModules(){

        vkDestroyShaderModule(device, vertShaderModule, null);
        vkDestroyShaderModule(device, fragShaderModule, null);

        vertShaderSPIRV.cleanup();
        fragShaderSPIRV.cleanup();
    }

    @Override
    public void dispose() {
        disposeShaderModules();
        MemoryUtil.memFree(entryPoint);
    }
}
