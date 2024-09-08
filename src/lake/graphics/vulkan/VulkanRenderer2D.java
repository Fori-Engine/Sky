package lake.graphics.vulkan;

import lake.FlightRecorder;
import lake.graphics.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocationCreateInfo;
import org.lwjgl.util.vma.VmaAllocationInfo;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.clamp;
import static java.util.stream.Collectors.toSet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK10.vkGetInstanceProcAddr;
import static org.lwjgl.vulkan.VK13.*;


public class VulkanRenderer2D extends Renderer2D {

    private static int FRAMES_IN_FLIGHT = 2;

    private static final int UINT64_MAX = 0xFFFFFFFF;
    private long debugMessenger;
    private VkPhysicalDevice physicalDevice;
    private VkPhysicalDeviceProperties physicalDeviceProperties;
    private VkPhysicalDeviceQueueFamilies physicalDeviceQueueFamilies;
    private static final Set<String> DEVICE_EXTENSIONS = Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
            .collect(toSet());
    private static List<String> validationLayers = new ArrayList<>();
    static {
        validationLayers.add("VK_LAYER_KHRONOS_validation");
    }
    private VkDevice device;
    private VkQueue graphicsQueue, presentQueue;
    private VkSwapchain swapchain;
    private VkSwapchainSupportDetails swapchainSupportDetails;
    private List<Long> swapchainImageViews = new ArrayList<>();
    private List<Long> swapchainFramebuffers = new ArrayList<>();
    private long surface;
    private VkInstance instance;
    private long renderPass;
    private VkFrame[] frames = new VkFrame[FRAMES_IN_FLIGHT];
    private int frameIndex;


    private long createDebugMessenger(VkInstance instance){

        long debugMessenger;

        try (MemoryStack stack = stackPush()) {

            VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
            debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
            debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
            debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
            debugCreateInfo.pfnUserCallback((messageSeverity, messageTypes, pCallbackData, pUserData) -> {

                VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                FlightRecorder.info(VulkanRenderer2D.class, callbackData.pMessageString());


                return VK_FALSE;
            });


            LongBuffer pDebugMessenger = stack.longs(VK_NULL_HANDLE);

            int result = 0;

            if(vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != NULL) {
                result = vkCreateDebugUtilsMessengerEXT(instance, debugCreateInfo, null, pDebugMessenger) == VK_SUCCESS ? VK_SUCCESS :  VK_ERROR_EXTENSION_NOT_PRESENT;
            }

            if(result != VK_SUCCESS) throw new RuntimeException("Failed to create the debug messenger as the extension is not present");

            debugMessenger = pDebugMessenger.get(0);
        }

        return debugMessenger;
    }


    private VkPhysicalDevice selectPhysicalDevice(VkInstance instance, long surface){
        try(MemoryStack stack = stackPush()) {

            IntBuffer physicalDevicesCount = stack.ints(0);

            vkEnumeratePhysicalDevices(instance, physicalDevicesCount, null);

            if(physicalDevicesCount.get(0) == 0) {
                throw new RuntimeException("No GPUs on the host support Vulkan");
            }
            PointerBuffer ppPhysicalDevices = stack.mallocPointer(physicalDevicesCount.get(0));
            vkEnumeratePhysicalDevices(instance, physicalDevicesCount, ppPhysicalDevices);



            for(int i = 0;i < ppPhysicalDevices.capacity();i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(ppPhysicalDevices.get(i), instance);


                boolean hasExtensions;
                boolean hasCorrectSwapchain = false;

                //Find the graphics and present queue
                {
                    IntBuffer queueFamilyCount = stack.ints(0);
                    vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);
                    VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
                    vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

                    IntBuffer presentSupport = stack.ints(VK_FALSE);

                    int graphicsFamily = 0, presentFamily = 0;

                    for (int queue = 0; queue < queueFamilies.capacity(); queue++) {
                        VkQueueFamilyProperties queueFamilyProperties = queueFamilies.get(queue);

                        if ((queueFamilyProperties.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) graphicsFamily = queue;
                        vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);
                        if (presentSupport.get(0) == VK_TRUE) presentFamily = queue;


                    }
                    physicalDeviceQueueFamilies = new VkPhysicalDeviceQueueFamilies(graphicsFamily, presentFamily);
                }

                //Find the requisite extensions
                {
                    IntBuffer extensionCount = stack.ints(0);

                    vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, null);
                    VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.malloc(extensionCount.get(0), stack);
                    vkEnumerateDeviceExtensionProperties(device, (String)null, extensionCount, availableExtensions);

                    hasExtensions = availableExtensions.stream()
                            .map(VkExtensionProperties::extensionNameString)
                            .collect(toSet())
                            .containsAll(DEVICE_EXTENSIONS);
                }

                //Find the write swapchain specifications
                if(hasExtensions) {
                    swapchainSupportDetails = new VkSwapchainSupportDetails();
                    swapchainSupportDetails.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
                    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, surface, swapchainSupportDetails.capabilities);

                    IntBuffer count = stack.ints(0);

                    vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, null);

                    if(count.get(0) != 0) {
                        swapchainSupportDetails.formats = VkSurfaceFormatKHR.malloc(count.get(0), stack);
                        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, count, swapchainSupportDetails.formats);
                    }

                    vkGetPhysicalDeviceSurfacePresentModesKHR(device,surface, count, null);

                    if(count.get(0) != 0) {
                        swapchainSupportDetails.presentModes = stack.mallocInt(count.get(0));
                        vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, count, swapchainSupportDetails.presentModes);
                    }

                    hasCorrectSwapchain = swapchainSupportDetails.formats.hasRemaining() && swapchainSupportDetails.presentModes.hasRemaining();
                }

                if(physicalDeviceQueueFamilies.graphicsFamily != null &&
                        physicalDeviceQueueFamilies.presentFamily != null &&
                        hasExtensions &&
                        hasCorrectSwapchain){

                    return device;
                }

            }

            throw new RuntimeException("No GPU was selected");
        }
    }
    private VkPhysicalDeviceProperties getPhysicalDeviceProperties(VkPhysicalDevice physicalDevice){
        VkPhysicalDeviceProperties physicalDeviceProperties = VkPhysicalDeviceProperties.create();
        vkGetPhysicalDeviceProperties(physicalDevice, physicalDeviceProperties);
        return physicalDeviceProperties;
    }
    private static PointerBuffer asPointerBuffer(MemoryStack stack, Collection<String> collection) {

        PointerBuffer buffer = stack.mallocPointer(collection.size());

        collection.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);

        return buffer.rewind();
    }
    private VkDevice createDevice(VkPhysicalDevice physicalDevice, boolean validation) {

        VkDevice device;


        try(MemoryStack stack = stackPush()) {



            int[] uniqueQueueFamilies = IntStream.of(physicalDeviceQueueFamilies.graphicsFamily, physicalDeviceQueueFamilies.presentFamily).distinct().toArray();

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueQueueFamilies.length, stack);

            for(int i = 0;i < uniqueQueueFamilies.length;i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            deviceFeatures.samplerAnisotropy(true);

            VkPhysicalDeviceDescriptorIndexingFeatures deviceDescriptorIndexingFeatures = VkPhysicalDeviceDescriptorIndexingFeatures.calloc(stack);
            deviceDescriptorIndexingFeatures.sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_FEATURES);

            deviceDescriptorIndexingFeatures.runtimeDescriptorArray(true);
            deviceDescriptorIndexingFeatures.descriptorBindingPartiallyBound(true);
            deviceDescriptorIndexingFeatures.shaderStorageBufferArrayNonUniformIndexing(true);
            deviceDescriptorIndexingFeatures.shaderSampledImageArrayNonUniformIndexing(true);
            deviceDescriptorIndexingFeatures.shaderStorageImageArrayNonUniformIndexing(true);
            deviceDescriptorIndexingFeatures.descriptorBindingUniformBufferUpdateAfterBind(true);

            deviceDescriptorIndexingFeatures.descriptorBindingStorageBufferUpdateAfterBind(true);
            deviceDescriptorIndexingFeatures.descriptorBindingSampledImageUpdateAfterBind(true);
            deviceDescriptorIndexingFeatures.descriptorBindingStorageImageUpdateAfterBind(true);




            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pNext(deviceDescriptorIndexingFeatures);


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

        return device;
    }
    private VkQueue getGraphicsQueue(VkDevice device) {

        VkQueue graphicsQueue;
        try(MemoryStack stack = stackPush()) {
            PointerBuffer pGraphicsQueue = stack.pointers(VK_NULL_HANDLE);
            vkGetDeviceQueue(device, physicalDeviceQueueFamilies.presentFamily, 0, pGraphicsQueue);
            graphicsQueue = new VkQueue(pGraphicsQueue.get(0), device);
        }

        return graphicsQueue;
    }
    private VkQueue getPresentQueue(VkDevice device) {

        VkQueue presentQueue;
        try(MemoryStack stack = stackPush()) {
            PointerBuffer pPresentQueue = stack.pointers(VK_NULL_HANDLE);
            vkGetDeviceQueue(device, physicalDeviceQueueFamilies.presentFamily, 0, pPresentQueue);
            presentQueue = new VkQueue(pPresentQueue.get(0), device);
        }

        return presentQueue;
    }
    private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {

        for(VkSurfaceFormatKHR availableFormat : availableFormats){
            if(availableFormat.format() == VK_FORMAT_B8G8R8A8_SRGB && availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR){
                return availableFormat;
            }
        }


        FlightRecorder.info(VulkanRenderer2D.class, "Unable to find sRGB VkSurfaceFormatKHR, using default. Colors may look incorrect.");

        return availableFormats.get(0);

    }
    private int chooseSwapPresentMode(IntBuffer availablePresentModes) {

        for(int i = 0;i < availablePresentModes.capacity();i++) {
            if(availablePresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                return availablePresentModes.get(i);
            }
        }

        return VK_PRESENT_MODE_FIFO_RELAXED_KHR;
    }
    private VkExtent2D chooseSwapExtent(MemoryStack stack, VkSurfaceCapabilitiesKHR capabilities, int width, int height) {



        if(capabilities.currentExtent().width() != UINT64_MAX) {
            VkExtent2D extent = VkExtent2D.malloc(stack).set(width, height);
            return extent;
        }

        VkExtent2D actualExtent = VkExtent2D.malloc(stack).set(width, height);

        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();

        actualExtent.width(clamp(actualExtent.width(), minExtent.width(), maxExtent.width()));
        actualExtent.height(clamp(actualExtent.height(), minExtent.height(), maxExtent.height()));



        return actualExtent;
    }
    private VkSwapchain createSwapChain(VkDevice device, long surface, int width, int height) {

        VkSwapchain swapchain = new VkSwapchain();

        try(MemoryStack stack = stackPush()) {

            swapchainSupportDetails.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, swapchainSupportDetails.capabilities);



            VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapchainSupportDetails.formats);
            int presentMode = chooseSwapPresentMode(swapchainSupportDetails.presentModes);
            VkExtent2D extent = chooseSwapExtent(stack, swapchainSupportDetails.capabilities, width, height);


            IntBuffer imageCount = stack.ints(swapchainSupportDetails.capabilities.minImageCount());


            if(swapchainSupportDetails.capabilities.maxImageCount() > 0 && imageCount.get(0) > swapchainSupportDetails.capabilities.maxImageCount()) {
                imageCount.put(0, swapchainSupportDetails.capabilities.maxImageCount());
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

            if(!physicalDeviceQueueFamilies.graphicsFamily.equals(physicalDeviceQueueFamilies.presentFamily)) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(stack.ints(physicalDeviceQueueFamilies.graphicsFamily, physicalDeviceQueueFamilies.presentFamily));
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(swapchainSupportDetails.capabilities.currentTransform());
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
    private List<Long> createSwapchainImageViews(VkDevice device, VkSwapchain swapchain) {

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
    private ArrayList<Long> createSwapchainFramebuffers(VkDevice device, VkSwapchain swapchain, List<Long> swapChainImageViews, long renderPass) {

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
    private static long createRenderPass(VkDevice device, VkSwapchain swapchain) {

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

    public VulkanRenderer2D(VkInstance instance, long surface, int width, int height, RenderSettings renderSettings) {
        this(width, height, renderSettings);
        this.instance = instance;
        this.surface = surface;

        if(renderSettings.enableValidation) {
            debugMessenger = createDebugMessenger(instance);
        }

        physicalDevice = selectPhysicalDevice(instance, surface);
        physicalDeviceProperties = getPhysicalDeviceProperties(physicalDevice);
        FlightRecorder.info(VulkanRenderer2D.class, "Selected Physical Device " + physicalDeviceProperties.deviceNameString());

        device = createDevice(physicalDevice, renderSettings.enableValidation);
        graphicsQueue = getGraphicsQueue(device);
        presentQueue = getPresentQueue(device);
        swapchain = createSwapChain(device, surface, width, height);
        swapchainImageViews = createSwapchainImageViews(device, swapchain);

        FlightRecorder.info(VulkanRenderer2D.class, "Max: " + physicalDeviceProperties.limits().maxDescriptorSetSamplers());


        renderPass = createRenderPass(device, swapchain);
        swapchainFramebuffers = createSwapchainFramebuffers(device, swapchain, swapchainImageViews, renderPass);
        for (int i = 0; i < FRAMES_IN_FLIGHT; i++) {


            try(MemoryStack stack = stackPush()) {

                LongBuffer pImageAcquiredSemaphore = stack.mallocLong(1);
                LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
                LongBuffer pInFlightFence = stack.mallocLong(1);



                VkSemaphoreCreateInfo imageAcquiredSemaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
                imageAcquiredSemaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

                VkSemaphoreCreateInfo renderFinishedSemaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
                renderFinishedSemaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

                VkFenceCreateInfo inFlightFenceCreateInfo = VkFenceCreateInfo.calloc(stack);
                inFlightFenceCreateInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
                inFlightFenceCreateInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

                vkCreateSemaphore(device, imageAcquiredSemaphoreInfo, null, pImageAcquiredSemaphore);
                vkCreateSemaphore(device, renderFinishedSemaphoreInfo, null, pRenderFinishedSemaphore);
                vkCreateFence(device, inFlightFenceCreateInfo, null, pInFlightFence);

                frames[i] = new VkFrame(pImageAcquiredSemaphore.get(0), pRenderFinishedSemaphore.get(0), pInFlightFence.get(0));
            }
        }










        //Allocate and submit renderSettings.batchSize-number indices in a staging index buffer
        //Transfer from index buffer to GPU dedicated index buffer

        //Begin Frame
        //Acquire a swapchain image

        //Uniform Updates
        //All uniform buffers are bound to host visible buffers as descriptors

        //Draw Calls
        //Look up the pipeline for the current shader
        //transfer all drawQuad() outputs to the staging vertex buffer
        //on invoking render() transfer from the staging vertex buffer to the GPU dedicated vertex buffer
        //Submit to the graphics queue and wait to finish
        //Wait on the submission fence

        //End Frame

        /*

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



        VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.create();
        bufferCreateInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
        bufferCreateInfo.size(65536);
        bufferCreateInfo.usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT);

        VmaAllocationCreateInfo allocationCreateInfo = VmaAllocationCreateInfo.create();
        allocationCreateInfo.usage(VMA_MEMORY_USAGE_AUTO);

        LongBuffer pBuffer = MemoryUtil.memAllocLong(1);
        PointerBuffer pAllocation = MemoryUtil.memAllocPointer(1);

        VmaAllocationInfo allocationInfo = VmaAllocationInfo.create();


        vmaCreateBuffer(pAllocator.get(0), bufferCreateInfo, allocationCreateInfo, pBuffer, pAllocation, allocationInfo);


         */

    }


    public VulkanRenderer2D(int width, int height, RenderSettings renderSettings) {
        super(width, height, renderSettings);







    }

    @Override
    public void onResize(int width, int height) {

        vkDeviceWaitIdle(device);

        System.out.println("Resizing!");

        vkDestroyRenderPass(device, renderPass, null);

        for(long swapchainFramebuffer : swapchainFramebuffers){
            vkDestroyFramebuffer(device, swapchainFramebuffer, null);
        }

        for(long swapchainImageView : swapchainImageViews) {
            vkDestroyImageView(device, swapchainImageView, null);
        }
        vkDestroySwapchainKHR(device, swapchain.swapChain, null);

        swapchain = createSwapChain(device, surface, width, height);
        swapchainImageViews = createSwapchainImageViews(device, swapchain);
        renderPass = createRenderPass(device, swapchain);
        swapchainFramebuffers = createSwapchainFramebuffers(device, swapchain, swapchainImageViews, renderPass);


        System.out.println("Resize Finished");



    }

    @Override
    public void createResources(ShaderProgram... shaderPrograms) {

    }

    @Override
    public void updateMatrices(ShaderProgram shaderProgram, ShaderResource modelViewProj) {

    }

    @Override
    public void drawTexture(float x, float y, float w, float h, Texture2D texture, Color color, Rect2D rect2D, boolean xFlip, boolean yFlip) {

    }

    @Override
    public void drawQuad(float x, float y, float w, float h, int quadTypeOrTextureIndex, Color color, float originX, float originY, Rect2D region, float thickness, boolean xFlip, boolean yFlip) {

    }

    @Override
    public void render() {

    }

    @Override
    public String getDeviceName() {
        return "";
    }

    @Override
    public void update() {

    }

    @Override
    public void dispose() {

    }
}