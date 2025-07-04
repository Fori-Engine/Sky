package fori.graphics.vulkan;

import fori.Logger;
import fori.graphics.Disposable;
import fori.graphics.Shader;
import fori.graphics.ShaderBinary;
import fori.graphics.ShaderType;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanShader extends Shader {
    private long module;


    public VulkanShader(Disposable parent, ShaderType shaderType, ShaderBinary bytecode) {
        super(parent, shaderType, bytecode);


        module = createShaderModule(VulkanDeviceManager.getCurrentDevice(), bytecode.data);

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
                                VulkanShaderProgram.class,
                                "Failed to create shader module"
                        ));

            }

            return pShaderModule.get(0);
        }
    }

    public int getStage() {
        switch(shaderType) {
            case Vertex -> {
                return VK_SHADER_STAGE_VERTEX_BIT;
            }
            case Fragment -> {
                return VK_SHADER_STAGE_FRAGMENT_BIT;
            }
            case Compute -> {
                return VK_SHADER_STAGE_COMPUTE_BIT;
            }
        }

        return -1;
    }

    public long getModule() {
        return module;
    }

    @Override
    public void dispose() {
        vkDestroyShaderModule(VulkanDeviceManager.getCurrentDevice(), module, null);
    }
}
