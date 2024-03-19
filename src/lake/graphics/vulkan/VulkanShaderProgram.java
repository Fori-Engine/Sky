package lake.graphics.vulkan;

import lake.graphics.ShaderProgram;
import lake.graphics.Disposable;
import lake.graphics.Disposer;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

import java.nio.ByteBuffer;

import static lake.graphics.vulkan.ShaderSPIRVUtils.ShaderKind.FRAGMENT_SHADER;
import static lake.graphics.vulkan.ShaderSPIRVUtils.ShaderKind.VERTEX_SHADER;
import static lake.graphics.vulkan.ShaderSPIRVUtils.compileShader;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;

public class VulkanShaderProgram extends ShaderProgram implements Disposable {

    private VkPipelineShaderStageCreateInfo.Buffer shaderStages;
    private VkDevice device;
    private ShaderSPIRVUtils.SPIRV vertShaderSPIRV;
    private ShaderSPIRVUtils.SPIRV fragShaderSPIRV;

    private ByteBuffer entryPoint;

    private long vertShaderModule;
    private long fragShaderModule;
    public VulkanShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        super(vertexShaderSource, fragmentShaderSource);
        Disposer.add(this);
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
        entryPoint = MemoryUtil.memUTF8("main");

        //MemoryUtil.fr


        try(MemoryStack stack = stackPush()) {

            // Let's compile the GLSL shaders into SPIR-V at runtime using the shaderc library
            // Check ShaderSPIRVUtils class to see how it can be done
            vertShaderSPIRV = compileShader("", getVertexShaderSource(), VERTEX_SHADER);
            fragShaderSPIRV = compileShader("", getFragmentShaderSource(), FRAGMENT_SHADER);

            vertShaderModule = FastVK.createShaderModule(device, vertShaderSPIRV.bytecode());
            fragShaderModule = FastVK.createShaderModule(device, fragShaderSPIRV.bytecode());




            //MemoryUtil.memFr
            //This goes out of scope when the MemoryStack frame is blown
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


            //stack.
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

        vertShaderSPIRV.free();
        fragShaderSPIRV.free();
    }

    @Override
    public void dispose() {
        MemoryUtil.memFree(entryPoint);
    }
}
