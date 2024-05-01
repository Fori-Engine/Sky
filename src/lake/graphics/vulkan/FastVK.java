//This code heavily reuses code from https://github.com/Naitsirc98/Vulkan-Tutorial-Java
//Thank you to Nathan and Alexander


package lake.graphics.vulkan;

import lake.FlightRecorder;
import lake.graphics.Window;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
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

public class FastVK {
    private FastVK(){}

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

            return availableLayerNames.containsAll(validationLayers);
        }
    }

    public static VkInstance createInstance(String appName, String appInfoEngineName, boolean validation) {

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
            appInfo.apiVersion(VK_API_VERSION_1_0);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);
            // enabledExtensionCount is implicitly set when you call ppEnabledExtensionNames
            createInfo.ppEnabledExtensionNames(getRequiredExtensions(stack, validation));

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


    public static VkPhysicalDevice pickPhysicalDevice(VkInstance instance, long surface) {

        VkPhysicalDevice physicalDevice = null;

        try(MemoryStack stack = stackPush()) {

            IntBuffer deviceCount = stack.ints(0);

            vkEnumeratePhysicalDevices(instance, deviceCount, null);

            if(deviceCount.get(0) == 0) {
                throw new RuntimeException("Failed to find GPUs with Vulkan support");
            }

            PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));

            vkEnumeratePhysicalDevices(instance, deviceCount, ppPhysicalDevices);

            for(int i = 0;i < ppPhysicalDevices.capacity();i++) {

                VkPhysicalDevice device = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);

                if(isDeviceSuitable(device, surface)) {
                    physicalDevice = device;
                    break;
                }
            }

            //throw new RuntimeException("Failed to find a suitable GPU");
        }

        return physicalDevice;
    }



    private static boolean isDeviceSuitable(VkPhysicalDevice device, long surface) {

        QueueFamilyIndices indices = findQueueFamilies(device, surface);

        boolean extensionsSupported = checkDeviceExtensionSupport(device);
        boolean swapChainAdequate = false;

        if(extensionsSupported) {
            try(MemoryStack stack = stackPush()) {
                SwapchainSupportDetails swapChainSupport = querySwapChainSupport(device, stack,  surface);
                swapChainAdequate = swapChainSupport.formats.hasRemaining() && swapChainSupport.presentModes.hasRemaining();
            }
        }

        return indices.isComplete() && extensionsSupported && swapChainAdequate;
    }
    private static boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {

        try(MemoryStack stack = stackPush()) {

            IntBuffer extensionCount = stack.ints(0);

            vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, null);

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0), stack);

            vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, availableExtensions);

            return availableExtensions.stream()
                    .map(VkExtensionProperties::extensionNameString)
                    .collect(toSet())
                    .containsAll(DEVICE_EXTENSIONS);
        }
    }

    private static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device, long surface) {

        QueueFamilyIndices indices = new QueueFamilyIndices();

        try(MemoryStack stack = stackPush()) {

            IntBuffer queueFamilyCount = stack.ints(0);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

            IntBuffer presentSupport = stack.ints(VK_FALSE);

            for(int i = 0;i < queueFamilies.capacity() || !indices.isComplete();i++) {

                if((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.graphicsFamily = i;
                }

                vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);

                if(presentSupport.get(0) == VK_TRUE) {
                    indices.presentFamily = i;
                }
            }

            return indices;
        }
    }




    public static long createShaderModule(VkDevice device, ByteBuffer spirvCode) {

        try(MemoryStack stack = stackPush()) {

            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode);

            LongBuffer pShaderModule = stack.mallocLong(1);

            if(vkCreateShaderModule(device, createInfo, null, pShaderModule) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create shader module");
            }

            return pShaderModule.get(0);
        }
    }








    public static ArrayList<Long> createFramebuffers(VkDevice device, LVKSwapchain swapchain, List<Long> swapChainImageViews, long renderPass) {

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


    public static long createCommandPool(VkDeviceWithIndices deviceWithIndices) {

        long commandPool;

        try(MemoryStack stack = stackPush()) {



            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(deviceWithIndices.queueFamilyIndices.graphicsFamily);
            poolInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            LongBuffer pCommandPool = stack.mallocLong(1);

            if (vkCreateCommandPool(deviceWithIndices.device, poolInfo, null, pCommandPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool");
            }

            commandPool = pCommandPool.get(0);
        }

        return commandPool;
    }






    public static VkDeviceWithIndices createLogicalDevice(VkPhysicalDevice physicalDevice, boolean validation, long surface) {

        VkDevice device;
        QueueFamilyIndices indices = findQueueFamilies(physicalDevice, surface);

        try(MemoryStack stack = stackPush()) {



            int[] uniqueQueueFamilies = indices.unique();

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);

            for(int i = 0;i < uniqueQueueFamilies.length;i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            deviceFeatures.samplerAnisotropy(true);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            // queueCreateInfoCount is automatically set

            createInfo.pEnabledFeatures(deviceFeatures);

            createInfo.ppEnabledExtensionNames(asPointerBuffer(stack, DEVICE_EXTENSIONS));

            if(validation) {
                createInfo.ppEnabledLayerNames(asPointerBuffer(stack, validationLayers));
            }

            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);

            if(vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }

            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

        }

        return new VkDeviceWithIndices(device, indices);
    }

    private static PointerBuffer asPointerBuffer(MemoryStack stack, Collection<String> collection) {

        PointerBuffer buffer = stack.mallocPointer(collection.size());

        collection.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);

        return buffer.rewind();
    }



    public static VkQueue getGraphicsQueue(VkDeviceWithIndices deviceConfig) {

        VkQueue graphicsQueue;

        try(MemoryStack stack = stackPush()) {
            PointerBuffer pGraphicsQueue = stack.pointers(VK_NULL_HANDLE);

            vkGetDeviceQueue(deviceConfig.device, deviceConfig.queueFamilyIndices.graphicsFamily, 0, pGraphicsQueue);

            graphicsQueue = new VkQueue(pGraphicsQueue.get(0), deviceConfig.device);
        }

        return graphicsQueue;
    }

    public static VkQueue getPresentQueue(VkDeviceWithIndices deviceConfig) {

        VkQueue presentQueue;

        try(MemoryStack stack = stackPush()) {
            PointerBuffer pPresentQueue = stack.pointers(VK_NULL_HANDLE);

            vkGetDeviceQueue(deviceConfig.device, deviceConfig.queueFamilyIndices.presentFamily, 0, pPresentQueue);




            presentQueue = new VkQueue(pPresentQueue.get(0), deviceConfig.device);
        }

        return presentQueue;
    }

    public static long createSurface(VkInstance instance, Window window) {

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
    public static LVKGenericBuffer createBuffer(VkDevice device, VkPhysicalDevice physicalDevice, long size, int usage, int properties, LongBuffer pBufferMemory) {

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

        LVKGenericBuffer genericBuffer =  new LVKGenericBuffer(pBuffer.get(0), bufferInfo, pBufferMemory.get(0));

        MemoryUtil.memFree(pBuffer);
        return genericBuffer;
    }


    public static List<Long> createImageViews(VkDevice device, LVKSwapchain swapchain) {

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


    public static long createRenderPass(VkDevice device, LVKSwapchain swapchain) {

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
    private static SwapchainSupportDetails querySwapChainSupport(VkPhysicalDevice device, MemoryStack stack, long surface) {

        SwapchainSupportDetails details = new SwapchainSupportDetails();

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

    public static LVKSwapchain createSwapChain(VkPhysicalDevice physicalDevice, VkDevice device, long surface, int width, int height) {

        LVKSwapchain swapchain = new LVKSwapchain();

        try(MemoryStack stack = stackPush()) {

            SwapchainSupportDetails swapChainSupport = querySwapChainSupport(physicalDevice, stack, surface);

            VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats);
            int presentMode = chooseSwapPresentMode(swapChainSupport.presentModes);
            VkExtent2D extent = chooseSwapExtent(stack, swapChainSupport.capabilities, width, height);

            IntBuffer imageCount = stack.ints(swapChainSupport.capabilities.minImageCount());


            if(swapChainSupport.capabilities.maxImageCount() > 0 && imageCount.get(0) > swapChainSupport.capabilities.maxImageCount()) {
                imageCount.put(0, swapChainSupport.capabilities.maxImageCount());
            }

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(surface);

            // Image settings
            createInfo.minImageCount(imageCount.get(0));
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            QueueFamilyIndices indices = findQueueFamilies(physicalDevice, surface);

            if(!indices.graphicsFamily.equals(indices.presentFamily)) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(stack.ints(indices.graphicsFamily, indices.presentFamily));
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(swapChainSupport.capabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);

            createInfo.oldSwapchain(VK_NULL_HANDLE);

            LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);

            if(vkCreateSwapchainKHR(device, createInfo, null, pSwapChain) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create swap chain");
            }

            swapchain.swapChain = pSwapChain.get(0);

            vkGetSwapchainImagesKHR(device, swapchain.swapChain, imageCount, null);

            LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));

            vkGetSwapchainImagesKHR(device, swapchain.swapChain, imageCount, pSwapchainImages);

            swapchain.swapChainImages = new ArrayList<>(imageCount.get(0));

            for(int i = 0;i < pSwapchainImages.capacity();i++) {
                swapchain.swapChainImages.add(pSwapchainImages.get(i));
            }

            swapchain.swapChainImageFormat = surfaceFormat.format();
            swapchain.swapChainExtent = VkExtent2D.create().set(extent);
        }


        return swapchain;
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

        return VK_PRESENT_MODE_FIFO_KHR;
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
        debugCreateInfo.pfnUserCallback(FastVK::debugCallback);
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

        FlightRecorder.info(FastVK.class, callbackData.pMessageString());

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

    private static PointerBuffer getRequiredExtensions(MemoryStack stack, boolean validation) {

        PointerBuffer glfwExtensions = glfwGetRequiredInstanceExtensions();

        if(validation) {

            PointerBuffer extensions = stack.mallocPointer(glfwExtensions.capacity() + 1);

            extensions.put(glfwExtensions);
            extensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));

            // Rewind the buffer before returning it to reset its position back to 0
            return extensions.rewind();
        }

        return glfwExtensions;
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
}
