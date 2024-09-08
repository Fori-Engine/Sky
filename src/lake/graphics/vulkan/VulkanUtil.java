//This code heavily reuses code from https://github.com/Naitsirc98/Vulkan-Tutorial-Java
//Thank you to Nathan and Alexander


package lake.graphics.vulkan;

import lake.FlightRecorder;
import lake.graphics.RenderContext;
import lake.graphics.PlatformWindow;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK13.*;

public class VulkanUtil {
    private VulkanUtil(){}

    private static List<String> validationLayers = new ArrayList<>();
    private static final Set<String> DEVICE_EXTENSIONS = Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
            .collect(toSet());

    static {
        validationLayers.add("VK_LAYER_KHRONOS_validation");
    }
    private static boolean checkValidationLayerSupport() {

        try(MemoryStack stack = stackPush()) {

            IntBuffer layerCount = stack.ints(0);

            vkEnumerateInstanceLayerProperties(layerCount, null);

            VkLayerProperties.Buffer availableLayers = VkLayerProperties.malloc(layerCount.get(0), stack);

            vkEnumerateInstanceLayerProperties(layerCount, availableLayers);

            Set<String> availableLayerNames = availableLayers.stream()
                    .map(VkLayerProperties::layerNameString)
                    .collect(toSet());

            availableLayerNames.forEach(s -> System.out.println(s));

            return availableLayerNames.containsAll(validationLayers);
        }
    }

    public static VkInstance createInstance(String appName, String appInfoEngineName, boolean validation, RenderContext.SurfaceType surfaceType) {

        if(validation && !checkValidationLayerSupport()) {
            throw new RuntimeException("Validation requested but not supported");
        }

        VkInstance instance;

        try(MemoryStack stack = stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);

            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8Safe(appName));
            appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8Safe(appInfoEngineName));
            appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK_API_VERSION_1_3);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            // enabledExtensionCount is implicitly set when you call ppEnabledExtensionNames
            createInfo.ppEnabledExtensionNames(getRequiredExtensions(stack, validation, surfaceType));

            // same with enabledLayerCount

            if(validation) {

                createInfo.ppEnabledLayerNames(validationLayersAsPointerBuffer(stack));
                VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                populateDebugMessengerCreateInfo(debugCreateInfo);
                createInfo.pNext(debugCreateInfo.address());
            }





            // We need to retrieve the pointer of the created instance
            PointerBuffer instancePtr = stack.mallocPointer(1);

            if(vkCreateInstance(createInfo, null, instancePtr) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create instance");
            }

            instance = new VkInstance(instancePtr.get(0), createInfo);
        }

        return instance;
    }







    public static ArrayList<Long> createFramebuffers(VkDevice device, VkSwapchain swapchain, List<Long> swapChainImageViews, long renderPass) {

        ArrayList<Long> swapChainFramebuffers = new ArrayList<>(swapChainImageViews.size());

        try(MemoryStack stack = stackPush()) {

            LongBuffer attachments = stack.mallocLong(1);
            LongBuffer pFramebuffer = stack.mallocLong(1);

            // Lets allocate the create info struct once and just update the pAttachments field each iteration
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(renderPass);
            framebufferInfo.width(swapchain.swapChainExtent.width());
            framebufferInfo.height(swapchain.swapChainExtent.height());
            framebufferInfo.layers(1);

            for(long imageView : swapChainImageViews) {

                attachments.put(0, imageView);

                framebufferInfo.pAttachments(attachments);

                if(vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer");
                }

                swapChainFramebuffers.add(pFramebuffer.get(0));
            }
        }

        return swapChainFramebuffers;
    }





    private static PointerBuffer asPointerBuffer(MemoryStack stack, Collection<String> collection) {

        PointerBuffer buffer = stack.mallocPointer(collection.size());

        collection.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);

        return buffer.rewind();
    }



    public static long createSurface(VkInstance instance, PlatformWindow window) {

        long surface;

        try(MemoryStack stack = stackPush()) {

            LongBuffer pSurface = stack.longs(VK_NULL_HANDLE);

            if(glfwCreateWindowSurface(instance, window.getGLFWHandle(), null, pSurface) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create window surface");
            }

            surface = pSurface.get(0);
        }

        return surface;
    }

    public static int findMemoryType(int typeFilter, int properties, VkPhysicalDevice physicalDevice) {

        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.create();
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);

        for(int i = 0;i < memProperties.memoryTypeCount();i++) {
            if((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }

        throw new RuntimeException("Failed to find suitable memory type");
    }
    public static VulkanBuffer createBuffer(VkDevice device, VkPhysicalDevice physicalDevice, long size, int usage, int properties, LongBuffer pBufferMemory) {

        LongBuffer pBuffer = MemoryUtil.memAllocLong(1);

        VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.create();
        bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
        bufferInfo.size(size);
        bufferInfo.usage(usage);
        bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

        if(vkCreateBuffer(device, bufferInfo, null, pBuffer) != VK_SUCCESS) {
            throw new RuntimeException("Failed to create buffer");
        }

        VkMemoryRequirements memRequirements = VkMemoryRequirements.create();
        vkGetBufferMemoryRequirements(device, pBuffer.get(0), memRequirements);

        VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.create();
        allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
        allocInfo.allocationSize(memRequirements.size());
        allocInfo.memoryTypeIndex(findMemoryType(memRequirements.memoryTypeBits(), properties, physicalDevice));

        if(vkAllocateMemory(device, allocInfo, null, pBufferMemory) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate buffer memory");
        }

        vkBindBufferMemory(device, pBuffer.get(0), pBufferMemory.get(0), 0);

        VulkanBuffer genericBuffer =  new VulkanBuffer(pBuffer.get(0), bufferInfo, pBufferMemory.get(0));

        MemoryUtil.memFree(pBuffer);
        return genericBuffer;
    }


    public static List<Long> createImageViews(VkDevice device, VkSwapchain swapchain) {

        List<Long> swapChainImageViews = new ArrayList<>(swapchain.swapChainImages.size());

        try(MemoryStack stack = stackPush()) {

            LongBuffer pImageView = stack.mallocLong(1);

            for(long swapChainImage : swapchain.swapChainImages) {

                VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack);

                createInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
                createInfo.image(swapChainImage);
                createInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
                createInfo.format(swapchain.swapChainImageFormat);

                createInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().g(VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().b(VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().a(VK_COMPONENT_SWIZZLE_IDENTITY);

                createInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                createInfo.subresourceRange().baseMipLevel(0);
                createInfo.subresourceRange().levelCount(1);
                createInfo.subresourceRange().baseArrayLayer(0);
                createInfo.subresourceRange().layerCount(1);

                if (vkCreateImageView(device, createInfo, null, pImageView) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create image views");
                }

                swapChainImageViews.add(pImageView.get(0));
            }

        }

        return swapChainImageViews;
    }


    public static long createRenderPass(VkDevice device, VkSwapchain swapchain) {

        long renderPass;

        try(MemoryStack stack = stackPush()) {

            VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.calloc(1, stack);
            colorAttachment.format(swapchain.swapChainImageFormat);
            colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.calloc(1, stack);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(colorAttachmentRef);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(colorAttachment);
            renderPassInfo.pSubpasses(subpass);

            LongBuffer pRenderPass = stack.mallocLong(1);

            if(vkCreateRenderPass(device, renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }

            renderPass = pRenderPass.get(0);
        }

        return renderPass;
    }
    private static VkSwapchainSupportDetails querySwapChainSupport(VkPhysicalDevice device, MemoryStack stack, long surface) {

        VkSwapchainSupportDetails details = new VkSwapchainSupportDetails();

        details.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, details.capabilities);

        IntBuffer count = stack.ints(0);

        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, null);

        if(count.get(0) != 0) {
            details.formats = VkSurfaceFormatKHR.malloc(count.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, details.formats);
        }

        vkGetPhysicalDeviceSurfacePresentModesKHR(device,surface, count, null);

        if(count.get(0) != 0) {
            details.presentModes = stack.mallocInt(count.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, details.presentModes);
        }

        return details;
    }



    private static VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {

        for(VkSurfaceFormatKHR availableFormat : availableFormats){
            if(availableFormat.format() == VK_FORMAT_B8G8R8A8_SRGB && availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR){
                return availableFormat;
            }
        }


        System.err.println("FastVK.chooseSwapSurfaceFormat() // " + "Unable to find sRGB VkSurfaceFormatKHR, using default. Colors may look incorrect.");

        return availableFormats.get(0);

    }

    private static int chooseSwapPresentMode(IntBuffer availablePresentModes) {

        for(int i = 0;i < availablePresentModes.capacity();i++) {
            if(availablePresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                return availablePresentModes.get(i);
            }
        }

        return VK_PRESENT_MODE_FIFO_RELAXED_KHR;
    }

    private static final int UINT32_MAX = 0xFFFFFFFF;
    private static VkExtent2D chooseSwapExtent(MemoryStack stack, VkSurfaceCapabilitiesKHR capabilities, int width, int height) {

        if(capabilities.currentExtent().width() != UINT32_MAX) {
            return capabilities.currentExtent();
        }

        VkExtent2D actualExtent = VkExtent2D.malloc(stack).set(width, height);

        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();

        actualExtent.width(clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
        actualExtent.height(clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

        return actualExtent;
    }

    private static int clamp(int min, int max, int value) {
        return Math.max(min, Math.min(max, value));
    }

    private static void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo) {
        debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
        debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
        debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
        debugCreateInfo.pfnUserCallback(VulkanUtil::debugCallback);
    }

    private static PointerBuffer validationLayersAsPointerBuffer(MemoryStack stack) {

        PointerBuffer buffer = stack.mallocPointer(validationLayers.size());

        validationLayers.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);

        return buffer.rewind();
    }

    private static int debugCallback(int messageSeverity, int messageType, long pCallbackData, long pUserData) {

        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);

        FlightRecorder.info(VulkanUtil.class, callbackData.pMessageString());

        return VK_FALSE;
    }

    public static VkSubmitInfo transfer(int sizeInBytes, long commandPool, VkDevice device, long srcBuffer, long dstBuffer){

        //TODO: Move to LVKCommandRunner



        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.create();
        allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
        allocInfo.commandPool(commandPool);
        allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
        allocInfo.commandBufferCount(1);

        PointerBuffer pCommandBuffer = MemoryUtil.memAllocPointer(1);
        vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer);
        VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(), device);


        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.create();
        beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
        beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

        vkBeginCommandBuffer(commandBuffer, beginInfo);


        VkBufferCopy.Buffer copyRegion = VkBufferCopy.create(1);;
        copyRegion.srcOffset(0);
        copyRegion.dstOffset(0);
        copyRegion.size(sizeInBytes);
        vkCmdCopyBuffer(commandBuffer, dstBuffer, srcBuffer, copyRegion);
        vkEndCommandBuffer(commandBuffer);

        VkSubmitInfo submitInfo = VkSubmitInfo.create();
        {
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(pCommandBuffer);
        }

        return submitInfo;
    }

    public static long debugMessenger;

    public static void setupDebugMessenger(VkInstance instance, boolean validation) {

        if(!validation) {
            return;
        }

        try(MemoryStack stack = stackPush()) {

            VkDebugUtilsMessengerCreateInfoEXT createInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);

            populateDebugMessengerCreateInfo(createInfo);

            LongBuffer pDebugMessenger = stack.longs(VK_NULL_HANDLE);

            if(createDebugUtilsMessengerEXT(instance, createInfo, null, pDebugMessenger) != VK_SUCCESS) {
                throw new RuntimeException("Failed to set up debug messenger");
            }

            debugMessenger = pDebugMessenger.get(0);
        }
    }

    public static void cleanupDebugMessenger(VkInstance instance, boolean validation){

        if(validation)
            destroyDebugUtilsMessengerEXT(instance, debugMessenger, null);


    }

    private static int createDebugUtilsMessengerEXT(VkInstance instance, VkDebugUtilsMessengerCreateInfoEXT createInfo,
                                                    VkAllocationCallbacks allocationCallbacks, LongBuffer pDebugMessenger) {

        if(vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != NULL) {
            return vkCreateDebugUtilsMessengerEXT(instance, createInfo, allocationCallbacks, pDebugMessenger);
        }

        return VK_ERROR_EXTENSION_NOT_PRESENT;
    }

    private static PointerBuffer getRequiredExtensions(MemoryStack stack, boolean validation, RenderContext.SurfaceType surfaceType) {

        PointerBuffer extensions = null;


        if(surfaceType == RenderContext.SurfaceType.PlatformWindow){
            PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();
            extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);
            extensions.put(glfwExtensions);

        }
        else if(surfaceType == RenderContext.SurfaceType.Canvas){
            extensions = stack.mallocPointer(3);
            extensions.put(stack.UTF8(VK_KHR_SURFACE_EXTENSION_NAME));
            extensions.put(stack.UTF8(KHRWin32Surface.VK_KHR_WIN32_SURFACE_EXTENSION_NAME));
        }

        if(validation) extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));


        return extensions.rewind();
    }


    private static void destroyDebugUtilsMessengerEXT(VkInstance instance, long debugMessenger, VkAllocationCallbacks allocationCallbacks) {

        if(vkGetInstanceProcAddr(instance, "vkDestroyDebugUtilsMessengerEXT") != NULL) {
            vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, allocationCallbacks);
        }

    }

    public static VkPhysicalDeviceProperties getPhysicalDeviceProperties(VkPhysicalDevice physicalDevice) {
        VkPhysicalDeviceProperties physicalDeviceProperties = VkPhysicalDeviceProperties.create();
        vkGetPhysicalDeviceProperties(physicalDevice, physicalDeviceProperties);




        return physicalDeviceProperties;
    }

    public static long commandPool;
    private static VkQueue graphicsQueue;



    public static void runCommand(VkDevice device, MemoryStack stack, VulkanCommand r){
        PointerBuffer pCommandBuffer = stack.mallocPointer(1);

        VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
        allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
        allocInfo.commandPool(commandPool);
        allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
        allocInfo.commandBufferCount(1);

        if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffer) != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate command buffers");
        }

        VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(), device);

        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
        beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

        if(vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
            throw new RuntimeException("Failed to begin recording command buffer");
        }

        r.run(commandBuffer);

        if(vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer");
        }

        VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
        {
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(stack.pointers(commandBuffer));
        }



        if(vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE) != VK_SUCCESS) {
            throw new RuntimeException("Failed to submit command buffer");
        }

        vkQueueWaitIdle(graphicsQueue);

    }

    public static void cleanupUtilsCommandPool(VkDevice device){
        vkDestroyCommandPool(device, commandPool, null);
    }

    public interface VulkanCommand {
        void run(VkCommandBuffer commandBuffer);
    }

}
