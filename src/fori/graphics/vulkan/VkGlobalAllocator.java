package fori.graphics.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;

import static org.lwjgl.util.vma.Vma.VMA_ALLOCATOR_CREATE_EXT_MEMORY_BUDGET_BIT;
import static org.lwjgl.util.vma.Vma.vmaCreateAllocator;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

public class VkGlobalAllocator {

    private static VkGlobalAllocator allocator;
    private long id;

    public static final void init(VkInstance instance, VkDevice device, VkPhysicalDevice physicalDevice){
        VmaVulkanFunctions vulkanFunctions = VmaVulkanFunctions.create();
        vulkanFunctions.set(instance, device);


        VmaAllocatorCreateInfo allocatorCreateInfo = VmaAllocatorCreateInfo.create();
        allocatorCreateInfo.vulkanApiVersion(VK_API_VERSION_1_3);
        allocatorCreateInfo.instance(instance);
        allocatorCreateInfo.physicalDevice(physicalDevice);
        allocatorCreateInfo.device(device);
        allocatorCreateInfo.flags(VMA_ALLOCATOR_CREATE_EXT_MEMORY_BUDGET_BIT);
        allocatorCreateInfo.pVulkanFunctions(vulkanFunctions);


        PointerBuffer pAllocator = MemoryUtil.memAllocPointer(1);
        vmaCreateAllocator(allocatorCreateInfo, pAllocator);

        VkGlobalAllocator.allocator = new VkGlobalAllocator();
        VkGlobalAllocator.allocator.id = pAllocator.get(0);

        MemoryUtil.memFree(pAllocator);
    }

    public long getId() {
        return id;
    }

    public static VkGlobalAllocator getAllocator() {
        return allocator;
    }
}
