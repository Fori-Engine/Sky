package fori.graphics.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;

import static org.lwjgl.util.vma.Vma.VMA_ALLOCATOR_CREATE_EXT_MEMORY_BUDGET_BIT;
import static org.lwjgl.util.vma.Vma.vmaCreateAllocator;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

public class VulkanAllocator {

    private static VulkanAllocator allocator;
    private long id;

    public static final void init(VkInstance instance, VkDevice device, VkPhysicalDevice physicalDevice){

        try(MemoryStack stack = MemoryStack.stackPush()) {
            VmaVulkanFunctions vulkanFunctions = VmaVulkanFunctions.calloc(stack);
            vulkanFunctions.set(instance, device);


            VmaAllocatorCreateInfo allocatorCreateInfo = VmaAllocatorCreateInfo.calloc(stack);
            allocatorCreateInfo.vulkanApiVersion(VK_API_VERSION_1_3);
            allocatorCreateInfo.instance(instance);
            allocatorCreateInfo.physicalDevice(physicalDevice);
            allocatorCreateInfo.device(device);
            allocatorCreateInfo.flags(VMA_ALLOCATOR_CREATE_EXT_MEMORY_BUDGET_BIT);
            allocatorCreateInfo.pVulkanFunctions(vulkanFunctions);


            PointerBuffer pAllocator = stack.callocPointer(1);
            vmaCreateAllocator(allocatorCreateInfo, pAllocator);

            VulkanAllocator.allocator = new VulkanAllocator();
            VulkanAllocator.allocator.id = pAllocator.get(0);

        }



    }

    public long getId() {
        return id;
    }

    public static VulkanAllocator getAllocator() {
        return allocator;
    }
}
