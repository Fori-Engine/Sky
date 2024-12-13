package fori.graphics.vulkan;

import editor.awt.AWTVK;
import fori.Logger;
import fori.Surface;
import fori.graphics.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.clamp;
import static java.util.stream.Collectors.toSet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.vmaDestroyAllocator;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.KHRSynchronization2.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR;
import static org.lwjgl.vulkan.VK13.*;


public class VkRenderer extends Renderer {

    public static final int FRAMES_IN_FLIGHT = 2;

    public static final int UINT64_MAX = 0xFFFFFFFF;
    private long debugMessenger;
    private VkPhysicalDevice physicalDevice;
    private VkPhysicalDeviceProperties physicalDeviceProperties;
    private VkPhysicalDeviceQueueFamilies physicalDeviceQueueFamilies;
    private static final Set<String> DEVICE_EXTENSIONS = Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME, KHRDynamicRendering.VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME)
            .collect(toSet());

    private VkDevice device;
    private VkQueue graphicsQueue, presentQueue;
    private VkSwapchain swapchain;
    private VkSwapchainSupportDetails swapchainSupportDetails;
    private List<Long> swapchainImageViews = new ArrayList<>();

    private long vkSurface;
    private VkInstance instance;

    private VkFrame[] frames = new VkFrame[FRAMES_IN_FLIGHT];
    private int frameIndex;
    private long sharedCommandPool;


    private VkImage depthImage;
    private VkImageView depthImageView;
    private VkGlobalAllocator allocator;

    private VkRenderingInfoKHR renderingInfoKHR;
    private VkRenderingAttachmentInfo.Buffer colorAttachment;
    private VkRenderingAttachmentInfoKHR depthAttachment;
    private boolean surfacePreviouslyLost = false;


    public VkRenderer(Ref parent, VkInstance instance, long vkSurface, int width, int height, RendererSettings rendererSettings, long debugMessenger, Surface surface) {
        super(parent, width, height, FRAMES_IN_FLIGHT, rendererSettings, surface);
        this.instance = instance;
        this.vkSurface = vkSurface;
        this.debugMessenger = debugMessenger;


        physicalDevice = selectPhysicalDevice(instance, vkSurface);
        physicalDeviceProperties = getPhysicalDeviceProperties(physicalDevice);
        VkContextManager.setPhysicalDeviceProperties(physicalDeviceProperties);
        Logger.info(VkRenderer.class, "Selected Physical Device " + physicalDeviceProperties.deviceNameString());
        device = createDevice(physicalDevice, rendererSettings.validation);

        VkContextManager.setCurrentDevice(device);
        VkContextManager.setCurrentPhysicalDevice(physicalDevice);






        graphicsQueue = getGraphicsQueue(device);
        VkContextManager.setGraphicsQueue(graphicsQueue);
        presentQueue = getPresentQueue(device);
        VkContextManager.setGraphicsFamilyIndex(physicalDeviceQueueFamilies.graphicsFamily);
        swapchain = createSwapChain(device, vkSurface, width, height, rendererSettings.vsync);
        swapchainImageViews = createSwapchainImageViews(device, swapchain);

        Logger.info(VkRenderer.class, "Max Allowed Allocations: " + physicalDeviceProperties.limits().maxMemoryAllocationCount());


        //Sync Objects
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

        VkGlobalAllocator.init(instance, device, physicalDevice);
        allocator = VkGlobalAllocator.getAllocator();


        sharedCommandPool = createCommandPool(device, physicalDeviceQueueFamilies.graphicsFamily);
        for(int i = 0; i < FRAMES_IN_FLIGHT; i++) {
            VkFrame frame = frames[i];


            try(MemoryStack stack = stackPush()) {

                VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
                allocInfo.commandPool(sharedCommandPool);
                allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
                allocInfo.commandBufferCount(FRAMES_IN_FLIGHT);

                PointerBuffer pCommandBuffers = stack.mallocPointer(FRAMES_IN_FLIGHT);

                if(vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to allocate command buffers");
                }

                frame.renderCommandBuffer = new VkCommandBuffer(pCommandBuffers.get(i), device);

            }



        }



        depthImage = new VkImage(
                ref,
                allocator,
                device,
                swapchain.swapChainExtent.width(),
                swapchain.swapChainExtent.height(),
                VK_FORMAT_D32_SFLOAT ,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                VK_IMAGE_TILING_OPTIMAL
        );

        depthImageView = new VkImageView(ref, device, depthImage, VK_IMAGE_ASPECT_DEPTH_BIT);























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


            //Bindless Setup
            VkPhysicalDeviceDescriptorIndexingFeatures deviceDescriptorIndexingFeatures = VkPhysicalDeviceDescriptorIndexingFeatures.calloc(stack);
            {
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
            }

            //Dynamic Rendering
            VkPhysicalDeviceDynamicRenderingFeaturesKHR physicalDeviceDynamicRenderingFeaturesKHR = VkPhysicalDeviceDynamicRenderingFeaturesKHR.calloc(stack);
            {
                physicalDeviceDynamicRenderingFeaturesKHR.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES_KHR);
                physicalDeviceDynamicRenderingFeaturesKHR.dynamicRendering(true);


            }

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pNext(deviceDescriptorIndexingFeatures).pNext(physicalDeviceDynamicRenderingFeaturesKHR);


            createInfo.pEnabledFeatures(deviceFeatures);
            createInfo.ppEnabledExtensionNames(asPointerBuffer(stack, DEVICE_EXTENSIONS));


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
            vkGetDeviceQueue(device, physicalDeviceQueueFamilies.graphicsFamily, 0, pGraphicsQueue);
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
            if(availableFormat.format() == VK_FORMAT_R8G8B8A8_SRGB && availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR){
                return availableFormat;
            }
        }


        Logger.info(VkRenderer.class, "Unable to find sRGB VkSurfaceFormatKHR, using default. Colors may look incorrect.");

        return availableFormats.get(0);

    }
    private int chooseSwapPresentMode(IntBuffer availablePresentModes, boolean vsync) {

        Function<Integer, Boolean> isPresentModeAvailable = presentMode -> {

            for(int i = 0; i < availablePresentModes.capacity(); i++){
                if(availablePresentModes.get(i) == presentMode) return true;
            }

            return false;
        };

        if(vsync) {
            if(isPresentModeAvailable.apply(VK_PRESENT_MODE_FIFO_KHR)) return VK_PRESENT_MODE_FIFO_KHR;
            else throw new RuntimeException("Vsync was requested but VK_PRESENT_MODE_FIFO_KHR is not available as a present mode");
        }
        else {
            if(isPresentModeAvailable.apply(VK_PRESENT_MODE_IMMEDIATE_KHR)) return VK_PRESENT_MODE_IMMEDIATE_KHR;
            else throw new RuntimeException("Vsync was requested to be disabled but VK_PRESENT_MODE_IMMEDIATE_KHR is not available as a present mode");
        }

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
    private VkSwapchain createSwapChain(VkDevice device, long surface, int width, int height, boolean vsync) {

        VkSwapchain swapchain = new VkSwapchain();

        try(MemoryStack stack = stackPush()) {

            swapchainSupportDetails.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, swapchainSupportDetails.capabilities);



            VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapchainSupportDetails.formats);
            int presentMode = chooseSwapPresentMode(swapchainSupportDetails.presentModes, vsync);
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
    private VkPipeline createPipeline(VkDevice device, VkSwapchain swapchain, RenderQueueFlags renderQueueFlags) {

        VkShaderProgram vkShaderProgram = (VkShaderProgram) renderQueueFlags.shaderProgram;


        Attributes.Type[] attributes = vkShaderProgram.getAttributes();
        long pipelineLayout;
        long graphicsPipeline = 0;

        try(MemoryStack stack = stackPush()) {

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            {

                Function<Integer, Integer> attributeSizeToVulkanParam = attributeSize -> {
                    switch(attributeSize){
                        case 1: return VK_FORMAT_R32_SFLOAT;
                        case 2: return VK_FORMAT_R32G32_SFLOAT;
                        case 3: return VK_FORMAT_R32G32B32_SFLOAT;
                        case 4: return VK_FORMAT_R32G32B32A32_SFLOAT;
                    }
                    return 0;
                };

                int vertexSize = 0;
                for(Attributes.Type attribute : attributes){
                    vertexSize += attribute.size;
                }


                VkVertexInputBindingDescription.Buffer bindingDescription =
                        VkVertexInputBindingDescription.calloc(1, stack);


                bindingDescription.binding(0);
                bindingDescription.stride(vertexSize * Float.BYTES);
                bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
                vertexInputInfo.pVertexBindingDescriptions(bindingDescription);



                VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                        VkVertexInputAttributeDescription.calloc(attributes.length, stack);

                int offset = 0;

                for (int i = 0; i < attributes.length; i++) {
                    VkVertexInputAttributeDescription attribute = attributeDescriptions.get(i);
                    attribute.binding(0);
                    attribute.location(i);
                    attribute.format(attributeSizeToVulkanParam.apply(attributes[i].size));
                    attribute.offset(offset);

                    offset += attributes[i].size * Float.BYTES;
                }




                vertexInputInfo.pVertexAttributeDescriptions(attributeDescriptions.rewind());



            }

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            {
                inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
                inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
                inputAssembly.primitiveRestartEnable(false);
            }




            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            {
                viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
                viewportState.viewportCount(1);
                viewportState.scissorCount(1);
            }

            VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack);
            {
                dynamicState.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
                dynamicState.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR));
            }

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            {
                rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
                rasterizer.depthClampEnable(false);
                rasterizer.rasterizerDiscardEnable(false);

                rasterizer.lineWidth(1);
                rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
                rasterizer.cullMode(VK_CULL_MODE_BACK_BIT);
                rasterizer.frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE);
                rasterizer.depthBiasEnable(false);
            }

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            {
                multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
                multisampling.sampleShadingEnable(false);
                multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);
            }

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            {
                colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
                colorBlendAttachment.blendEnable(false);
                colorBlendAttachment.colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);
                colorBlendAttachment.blendEnable(true);
                colorBlendAttachment.srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA);
                colorBlendAttachment.dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
                colorBlendAttachment.colorBlendOp(VK_BLEND_OP_ADD);
                colorBlendAttachment.srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE);
                colorBlendAttachment.dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO);
                colorBlendAttachment.alphaBlendOp(VK_BLEND_OP_ADD);

            }


            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            {
                colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
                colorBlending.logicOpEnable(false);
                colorBlending.logicOp(VK_LOGIC_OP_COPY);
                colorBlending.pAttachments(colorBlendAttachment);
                colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));
            }

            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            {
                depthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
                depthStencil.depthTestEnable(renderQueueFlags.depthTest);
                depthStencil.depthWriteEnable(renderQueueFlags.depthTest);
                depthStencil.depthCompareOp(toVkDepthCompareOpEnum(renderQueueFlags.depthOp));
                depthStencil.minDepthBounds(0f);
                depthStencil.maxDepthBounds(1f);


                depthStencil.stencilTestEnable(false);


            }



            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            {
                pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
                pipelineLayoutInfo.setLayoutCount(vkShaderProgram.getShaderResSets().length);
                pipelineLayoutInfo.pSetLayouts(stack.longs(vkShaderProgram.getAllDescriptorSetLayouts()));


            }
            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            pipelineLayout = pPipelineLayout.get(0);

            VkPipelineRenderingCreateInfoKHR pipelineRenderingCreateInfoKHR = VkPipelineRenderingCreateInfoKHR.calloc(stack);
            pipelineRenderingCreateInfoKHR.colorAttachmentCount(1);
            pipelineRenderingCreateInfoKHR.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO_KHR);
            pipelineRenderingCreateInfoKHR.pColorAttachmentFormats(stack.ints(swapchain.swapChainImageFormat));
            pipelineRenderingCreateInfoKHR.depthAttachmentFormat(depthImage.getFormat());



            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            {
                pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
                pipelineInfo.layout(pipelineLayout);
                pipelineInfo.pInputAssemblyState(inputAssembly);
                pipelineInfo.pRasterizationState(rasterizer);
                pipelineInfo.pColorBlendState(colorBlending);
                pipelineInfo.pMultisampleState(multisampling);
                pipelineInfo.pViewportState(viewportState);
                pipelineInfo.pDepthStencilState(depthStencil);
                pipelineInfo.pDynamicState(dynamicState);
                pipelineInfo.stageCount(vkShaderProgram.getShaderStages().capacity());
                pipelineInfo.pStages(vkShaderProgram.getShaderStages());
                pipelineInfo.pVertexInputState(vertexInputInfo);
                pipelineInfo.pNext(pipelineRenderingCreateInfoKHR);
            }


            LongBuffer pGraphicsPipeline = stack.mallocLong(1);


            if(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }


            graphicsPipeline = pGraphicsPipeline.get(0);


        }

        return new VkPipeline(pipelineLayout, graphicsPipeline);
    }

    private int toVkDepthCompareOpEnum(RenderQueueFlags.DepthOp depthOp) {
        if(depthOp == null) return -1;
        switch(depthOp) {
            case LessThan -> {
                return VK_COMPARE_OP_LESS;
            }
            case GreaterThan -> {
                return VK_COMPARE_OP_GREATER;
            }
            case LessOrEqualTo -> {
                return VK_COMPARE_OP_LESS_OR_EQUAL;
            }
            case GreaterOrEqualTo -> {
                return VK_COMPARE_OP_GREATER_OR_EQUAL;
            }
            case Always -> {
                return VK_COMPARE_OP_ALWAYS;
            }
            case Never -> {
                return VK_COMPARE_OP_NEVER;
            }
        }

        throw new RuntimeException(Logger.error(VkRenderer.class, "The depth operation for this pipeline is an invalid value [" + depthOp + "]"));
    }

    private long createCommandPool(VkDevice device, int queueFamily) {

        long commandPool = 0;

        try(MemoryStack stack = stackPush()) {
            VkCommandPoolCreateInfo commandPoolCreateInfo = VkCommandPoolCreateInfo.calloc(stack);
            commandPoolCreateInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            commandPoolCreateInfo.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);
            commandPoolCreateInfo.queueFamilyIndex(queueFamily);

            LongBuffer pCommandPool = stack.mallocLong(1);

            if(vkCreateCommandPool(device, commandPoolCreateInfo, null, pCommandPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool");
            }
            commandPool = pCommandPool.get(0);
        }

        return commandPool;
    }





    private void recreateSwapchainAndDepthResources(int width, int height){
        disposeSwapchainAndDepthResources();
        createSwapchainAndDepthResources(width, height);
    }

    private void createSwapchainAndDepthResources(int width, int height){
        swapchain = createSwapChain(device, vkSurface, width, height, settings.vsync);
        swapchainImageViews = createSwapchainImageViews(device, swapchain);


        depthImage = new VkImage(
                ref,
                allocator,
                device,
                width,
                height,
                VK_FORMAT_D32_SFLOAT ,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                VK_IMAGE_TILING_OPTIMAL
        );

        depthImageView = new VkImageView(ref, device, depthImage, VK_IMAGE_ASPECT_DEPTH_BIT);




        frameIndex = 0;
    }

    private void disposeSwapchainAndDepthResources(){

        vkDeviceWaitIdle(device);
        for(long swapchainImageView : swapchainImageViews){
            vkDestroyImageView(device, swapchainImageView, null);
        }
        vkDestroySwapchainKHR(device, swapchain.swapChain, null);

        depthImageView.dispose();
        depthImage.dispose();

        ref.remove(depthImageView);
        ref.remove(depthImage);



    }




    public VkDevice getDevice() {
        return device;
    }



    @Override
    public RenderQueue newRenderQueue(RenderQueueFlags renderQueueFlags) {
        VkRenderQueue vkRenderQueue = new VkRenderQueue(FRAMES_IN_FLIGHT, sharedCommandPool, graphicsQueue, device);
        renderQueues.add(vkRenderQueue);


        vkRenderQueue.setStagingVertexBuffer(Buffer.newBuffer(
                getRef(),
                Attributes.getSize(renderQueueFlags.shaderProgram.getAttributes()) * Float.BYTES * renderQueueFlags.maxVertices,
                Buffer.Usage.VertexBuffer,
                Buffer.Type.CPUGPUShared,
                true
        ));
        vkRenderQueue.setStagingIndexBuffer(Buffer.newBuffer(
                getRef(),
                renderQueueFlags.maxIndices * Integer.BYTES,
                Buffer.Usage.IndexBuffer,
                Buffer.Type.CPUGPUShared,
                true
        ));
        vkRenderQueue.setVertexBuffer(Buffer.newBuffer(
                getRef(),
                Attributes.getSize(renderQueueFlags.shaderProgram.getAttributes()) * Float.BYTES * renderQueueFlags.maxVertices,
                Buffer.Usage.VertexBuffer,
                Buffer.Type.GPULocal,
                false
        ));
        vkRenderQueue.setIndexBuffer(Buffer.newBuffer(
                getRef(),
                renderQueueFlags.maxIndices * Integer.BYTES,
                Buffer.Usage.IndexBuffer,
                Buffer.Type.GPULocal,
                false
        ));

        vkRenderQueue.setShaderProgram(renderQueueFlags.shaderProgram);
        vkRenderQueue.setPipeline(createPipeline(device, swapchain, renderQueueFlags));

        return vkRenderQueue;
    }

    @Override
    public void removeQueue(RenderQueue renderQueue) {
        if(renderQueues.contains(renderQueue)){
            renderQueues.remove(renderQueue);
        }
        else {
            throw new RuntimeException(Logger.error(VkRenderer.class, "Cannot remove a RenderCommand which was never placed in the queue"));
        }
    }

    @Override
    public void update(boolean recreateRenderer) {

        try(MemoryStack stack = stackPush()) {

            if(recreateRenderer) {
                disposeSwapchainAndDepthResources();
                vkDestroySurfaceKHR(instance, vkSurface, null);
                this.vkSurface = surface.getVulkanSurface();
                this.width = surface.getWidth();
                this.height = surface.getHeight();
                createSwapchainAndDepthResources(this.width, this.height);
            }





            if(surface.getWidth() != width || surface.getHeight() != height){
                this.width = surface.getWidth();
                this.height = surface.getHeight();
                recreateSwapchainAndDepthResources(this.width, this.height);
            }


            VkFrame frame = frames[frameIndex];
            IntBuffer pImageIndex = stack.ints(0);

            vkWaitForFences(device, frame.inFlightFence, true, UINT64_MAX);
            vkAcquireNextImageKHR(device, swapchain.swapChain, UINT64_MAX, frame.imageAcquiredSemaphore, VK_NULL_HANDLE, pImageIndex);



            int imageIndex = pImageIndex.get(0);
            vkResetFences(device, frame.inFlightFence);
            vkResetCommandBuffer(frame.renderCommandBuffer, 0);
            {

                VkCommandBuffer commandBuffer = frame.renderCommandBuffer;

                VkClearValue colorClearValue = VkClearValue.calloc(stack);
                colorClearValue.color().float32(stack.floats(0, 0, 0, 1.0f));

                VkClearValue depthClearValue = VkClearValue.calloc(stack);
                depthClearValue.depthStencil().set(1.0f, 0);


                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
                beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);


                colorAttachment = VkRenderingAttachmentInfoKHR.calloc(1, stack);
                {
                    colorAttachment.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
                    colorAttachment.imageView(swapchainImageViews.get(frameIndex));
                    colorAttachment.imageLayout(VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR);
                    colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
                    colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
                    colorAttachment.clearValue(colorClearValue);
                }
                depthAttachment = VkRenderingAttachmentInfoKHR.calloc(stack);
                {
                    depthAttachment.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
                    depthAttachment.imageView(depthImageView.getHandle());
                    depthAttachment.imageLayout(VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL_KHR);
                    depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
                    depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
                    depthAttachment.clearValue(depthClearValue);
                }

                renderingInfoKHR = VkRenderingInfoKHR.calloc(stack);
                renderingInfoKHR.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_INFO_KHR);
                VkRect2D renderArea = VkRect2D.calloc(stack);
                renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
                renderArea.extent(swapchain.swapChainExtent);
                renderingInfoKHR.renderArea(renderArea);
                renderingInfoKHR.layerCount(1);
                renderingInfoKHR.pColorAttachments(colorAttachment);
                renderingInfoKHR.pDepthAttachment(depthAttachment);


                if(vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to begin recording command buffer");
                }

                VkImageMemoryBarrier.Buffer startBarrier = VkImageMemoryBarrier.calloc(1, stack);
                {
                    startBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                    startBarrier.oldLayout(VK_IMAGE_LAYOUT_UNDEFINED);
                    startBarrier.newLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                    startBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                    startBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                    startBarrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                    startBarrier.image(swapchain.swapChainImages.get(frameIndex));
                    startBarrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    startBarrier.subresourceRange().baseMipLevel(0);
                    startBarrier.subresourceRange().levelCount(1);
                    startBarrier.subresourceRange().baseArrayLayer(0);
                    startBarrier.subresourceRange().layerCount(1);
                }

                vkCmdPipelineBarrier(
                        commandBuffer,
                        VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
                        VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                        0,
                        null,
                        null,
                        startBarrier
                );



                KHRDynamicRendering.vkCmdBeginRenderingKHR(commandBuffer, renderingInfoKHR);
                {
                    VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
                    viewport.x(0.0f);
                    viewport.y(0.0f);
                    viewport.width(swapchain.swapChainExtent.width());
                    viewport.height(swapchain.swapChainExtent.height());


                    viewport.minDepth(0.0f);
                    viewport.maxDepth(1.0f);


                    vkCmdSetViewport(commandBuffer, 0, viewport);

                    VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
                    {
                        scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
                        scissor.extent(swapchain.swapChainExtent);
                    }

                    vkCmdSetScissor(commandBuffer, 0, scissor);


                    for(RenderQueue renderQueue : renderQueues) {

                        VkBuffer vertexBuffer = (VkBuffer) renderQueue.getVertexBuffer();
                        VkBuffer indexBuffer = (VkBuffer) renderQueue.getIndexBuffer();




                        VkPipeline pipeline = ((VkRenderQueue) renderQueue).getPipeline();
                        VkShaderProgram shaderProgram = (VkShaderProgram) renderQueue.getShaderProgram();
                        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.pipeline);

                        LongBuffer vertexBuffers = stack.longs(vertexBuffer.getHandle());
                        LongBuffer offsets = stack.longs(0);

                        vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);

                        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                pipeline.pipelineLayout, 0, stack.longs(shaderProgram.getDescriptorSets(frameIndex)), null);

                        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getHandle(), 0, VK_INDEX_TYPE_UINT32);
                        vkCmdDrawIndexed(commandBuffer, renderQueue.getIndexCount(), 1, 0, 0, 0);







                    }

                }
                KHRDynamicRendering.vkCmdEndRenderingKHR(commandBuffer);

                VkImageMemoryBarrier.Buffer endBarrier = VkImageMemoryBarrier.calloc(1, stack);
                {
                    endBarrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
                    endBarrier.oldLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                    endBarrier.newLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
                    endBarrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                    endBarrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
                    endBarrier.srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                    endBarrier.image(swapchain.swapChainImages.get(frameIndex));
                    endBarrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    endBarrier.subresourceRange().baseMipLevel(0);
                    endBarrier.subresourceRange().levelCount(1);
                    endBarrier.subresourceRange().baseArrayLayer(0);
                    endBarrier.subresourceRange().layerCount(1);
                }

                vkCmdPipelineBarrier(
                        commandBuffer,
                        VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                        VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                        0,
                        null,
                        null,
                        endBarrier
                );

                if(vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to record command buffer");
                }
            }

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(stack.longs(frame.imageAcquiredSemaphore));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(frame.renderCommandBuffer));
            submitInfo.pSignalSemaphores(stack.longs(frame.renderFinishedSemaphore));

            int catchVal = vkQueueSubmit(graphicsQueue, submitInfo, frame.inFlightFence);

            if(catchVal != VK_SUCCESS) {
                System.out.println("Catch: " + catchVal);
                throw new RuntimeException("Failed to submit draw command buffer");
            }




            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(stack.longs(frame.renderFinishedSemaphore));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapchain.swapChain));

            presentInfo.pImageIndices(pImageIndex);

            vkQueuePresentKHR(presentQueue, presentInfo);

            frameIndex = (frameIndex + 1) % FRAMES_IN_FLIGHT;

        }


    }

    @Override
    public int getFrameIndex() {
        return frameIndex;
    }

    @Override
    public String getDeviceName() {
        return physicalDeviceProperties.deviceNameString();
    }

    @Override
    public void waitForDevice() {
        vkDeviceWaitIdle(device);
    }

    @Override
    public int getMaxRenderQueueCount() {
        return 10;
    }


    @Override
    public void dispose() {
        vkDeviceWaitIdle(device);



        for(VkFrame frame : frames){
            vkDestroySemaphore(device, frame.imageAcquiredSemaphore, null);
            vkDestroySemaphore(device, frame.renderFinishedSemaphore, null);
            vkDestroyFence(device, frame.inFlightFence, null);
        }

        disposeSwapchainAndDepthResources();
        for(RenderQueue renderQueue : renderQueues){
            VkRenderQueue vkRenderQueue = (VkRenderQueue) renderQueue;
            vkDestroyFence(device, vkRenderQueue.getFence(), null);
            vkDestroyPipeline(device, vkRenderQueue.getPipeline().pipeline, null);
            vkDestroyPipelineLayout(device, vkRenderQueue.getPipeline().pipelineLayout, null);


        }



        vkDestroyCommandPool(device, sharedCommandPool, null);

        vmaDestroyAllocator(allocator.getId());
        vkDestroySurfaceKHR(instance, vkSurface, null);
        EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);









        vkDestroyDevice(device, null);
        vkDestroyInstance(instance, null);
    }

}