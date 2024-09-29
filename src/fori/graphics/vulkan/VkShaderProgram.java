package fori.graphics.vulkan;

import fori.Logger;
import fori.graphics.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_fragment_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_vertex_shader;
import static org.lwjgl.vulkan.VK10.*;

public class VkShaderProgram extends ShaderProgram {

    private VkDevice device;
    private ShaderBinary vertexShaderBinary, fragmentShaderBinary;
    private long vertexShaderModule, fragmentShaderModule;
    private VkPipelineShaderStageCreateInfo.Buffer shaderStages;



    public VkShaderProgram(SceneRenderer sceneRenderer, String vertexShaderSource, String fragmentShaderSource) {
        super(vertexShaderSource, fragmentShaderSource);
        this.device = ((VkSceneRenderer) sceneRenderer).getDevice();
    }

    public static long createShaderModule(VkDevice device, ByteBuffer spirvCode) {

        try(MemoryStack stack = stackPush()) {

            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode);

            LongBuffer pShaderModule = stack.mallocLong(1);

            if(vkCreateShaderModule(device, createInfo, null, pShaderModule) != VK_SUCCESS) {

                throw new RuntimeException(
                        Logger.error(
                                VkShaderProgram.class,
                                "Failed to create shader module"
                        ));

            }

            return pShaderModule.get(0);
        }
    }


    @Override
    public ByteBuffer[] mapUniformBuffer(ShaderResource resource) {
        return new ByteBuffer[0];
    }

    @Override
    public void unmapUniformBuffer(ShaderResource resource, ByteBuffer[] byteBuffers) {

    }

    @Override
    public void updateEntireSampler2DArrayWithOnly(ShaderResource resource, Texture2D texture) {

    }

    @Override
    public void updateSampler2DArray(ShaderResource resource, int index, Texture2D texture2D) {

    }

    @Override
    public void prepare() {
        try(MemoryStack stack = MemoryStack.stackPush()){
            ByteBuffer entryPoint = MemoryUtil.memUTF8("main");
            vertexShaderBinary = ShaderCompiler.compile(vertexShaderSource, shaderc_glsl_vertex_shader);
            fragmentShaderBinary = ShaderCompiler.compile(fragmentShaderSource, shaderc_glsl_fragment_shader);

            vertexShaderModule = createShaderModule(device, vertexShaderBinary.bytecode);
            fragmentShaderModule = createShaderModule(device, fragmentShaderBinary.bytecode);

            shaderStages = VkPipelineShaderStageCreateInfo.create(2);


            VkPipelineShaderStageCreateInfo vertexShaderStageInfo = shaderStages.get(0);
            vertexShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertexShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
            vertexShaderStageInfo.module(vertexShaderModule);
            vertexShaderStageInfo.pName(entryPoint);

            VkPipelineShaderStageCreateInfo fragmentShaderStageInfo = shaderStages.get(1);
            fragmentShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragmentShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragmentShaderStageInfo.module(fragmentShaderModule);
            fragmentShaderStageInfo.pName(entryPoint);

        }

    }

    public VkPipelineShaderStageCreateInfo.Buffer getShaderStages() {
        return shaderStages;
    }

    @Override
    public void dispose() {

    }
}
