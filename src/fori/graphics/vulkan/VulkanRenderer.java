package fori.graphics.vulkan;

import fori.Logger;
import fori.Surface;
import fori.ecs.Scene;
import fori.graphics.*;
import fori.graphics.DynamicMesh;
import fori.graphics.StaticMeshBatch;
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


public class VulkanRenderer extends Renderer {

    public static final int FRAMES_IN_FLIGHT = 2;

    public static final int UINT64_MAX = 0xFFFFFFFF;
    private long debugMessenger;
    private VkPhysicalDevice physicalDevice;
    private VkPhysicalDeviceProperties physicalDeviceProperties;
    private VulkanPhysicalDeviceQueueFamilies physicalDeviceQueueFamilies;
    private static final Set<String> DEVICE_EXTENSIONS = Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME, KHRDynamicRendering.VK_KHR_DYNAMIC_RENDERING_EXTENSION_NAME)
            .collect(toSet());

    private VkDevice device;
    private VkQueue graphicsQueue, presentQueue;
    private VulkanSwapchain swapchain;
    private VulkanSwapchainSupportInfo swapchainSupportDetails;

    private long vkSurface;
    private VkInstance instance;

    private VulkanFrame[] frames = new VulkanFrame[FRAMES_IN_FLIGHT];
    private int frameIndex;
    private long sharedCommandPool;


    private VulkanImage depthImage;
    private VulkanImageView depthImageView;
    private VulkanAllocator allocator;

    private VkRenderingInfoKHR renderingInfoKHR;
    private VkRenderingAttachmentInfo.Buffer colorAttachment;
    private VkRenderingAttachmentInfoKHR depthAttachment;

    private RenderTarget swapchainRenderTarget;


    public VulkanRenderer(Disposable parent, VkInstance instance, long vkSurface, int width, int height, RendererSettings rendererSettings, long debugMessenger, Surface surface) {
        super(parent, width, height, FRAMES_IN_FLIGHT, rendererSettings, surface);
        this.instance = instance;
        this.vkSurface = vkSurface;
        this.debugMessenger = debugMessenger;


        physicalDevice = selectPhysicalDevice(instance, vkSurface);
        physicalDeviceProperties = getPhysicalDeviceProperties(physicalDevice);
        VulkanDeviceManager.setPhysicalDeviceProperties(physicalDeviceProperties);
        Logger.info(VulkanRenderer.class, "Selected Physical Device " + physicalDeviceProperties.deviceNameString());
        device = createDevice(physicalDevice, rendererSettings.validation);

        VulkanDeviceManager.setCurrentDevice(device);
        VulkanDeviceManager.setCurrentPhysicalDevice(physicalDevice);






        graphicsQueue = getGraphicsQueue(device);
        VulkanDeviceManager.setGraphicsQueue(graphicsQueue);
        presentQueue = getPresentQueue(device);
        VulkanDeviceManager.setGraphicsFamilyIndex(physicalDeviceQueueFamilies.graphicsFamily);
        swapchainRenderTarget = createSwapchainRenderTarget(rendererSettings);

        Logger.info(VulkanRenderer.class, "Max Allowed Allocations: " + physicalDeviceProperties.limits().maxMemoryAllocationCount());


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

                frames[i] = new VulkanFrame(pImageAcquiredSemaphore.get(0), pRenderFinishedSemaphore.get(0), pInFlightFence.get(0));
            }
        }

        VulkanAllocator.init(instance, device, physicalDevice);
        allocator = VulkanAllocator.getAllocator();


        sharedCommandPool = createCommandPool(device, physicalDeviceQueueFamilies.graphicsFamily);
        for(int i = 0; i < FRAMES_IN_FLIGHT; i++) {
            VulkanFrame frame = frames[i];


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



        depthImage = new VulkanImage(
                this,
                allocator,
                device,
                swapchain.extent.width(),
                swapchain.extent.height(),
                VK_FORMAT_D32_SFLOAT ,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                VK_IMAGE_TILING_OPTIMAL
        );

        depthImageView = new VulkanImageView(this, device, depthImage, VK_IMAGE_ASPECT_DEPTH_BIT);























    }

    private RenderTarget createSwapchainRenderTarget(RendererSettings rendererSettings) {
        swapchainRenderTarget = new RenderTarget(maxFramesInFlight + 1);
        swapchain = new VulkanSwapchain();

        try(MemoryStack stack = stackPush()) {


            //Getting Swapchain Images
            {

                swapchainSupportDetails.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
                vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, vkSurface, swapchainSupportDetails.capabilities);


                VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapchainSupportDetails.formats);
                int presentMode = chooseSwapPresentMode(swapchainSupportDetails.presentModes, rendererSettings.vsync);
                VkExtent2D extent = chooseSwapExtent(stack, swapchainSupportDetails.capabilities, width, height);


                IntBuffer imageCount = stack.ints(swapchainSupportDetails.capabilities.minImageCount());


                if (swapchainSupportDetails.capabilities.maxImageCount() > 0 && imageCount.get(0) > swapchainSupportDetails.capabilities.maxImageCount()) {
                    imageCount.put(0, swapchainSupportDetails.capabilities.maxImageCount());
                }

                VkSwapchainCreateInfoKHR swapchainCreateInfo = VkSwapchainCreateInfoKHR.calloc(stack);

                swapchainCreateInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
                swapchainCreateInfo.surface(vkSurface);
                swapchainCreateInfo.minImageCount(imageCount.get(0));
                swapchainCreateInfo.imageFormat(surfaceFormat.format());
                swapchainCreateInfo.imageColorSpace(surfaceFormat.colorSpace());
                swapchainCreateInfo.imageExtent(extent);
                swapchainCreateInfo.imageArrayLayers(1);
                swapchainCreateInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

                if (!physicalDeviceQueueFamilies.graphicsFamily.equals(physicalDeviceQueueFamilies.presentFamily)) {
                    swapchainCreateInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                    swapchainCreateInfo.pQueueFamilyIndices(stack.ints(physicalDeviceQueueFamilies.graphicsFamily, physicalDeviceQueueFamilies.presentFamily));
                } else {
                    swapchainCreateInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
                }

                swapchainCreateInfo.preTransform(swapchainSupportDetails.capabilities.currentTransform());
                swapchainCreateInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
                swapchainCreateInfo.presentMode(presentMode);
                swapchainCreateInfo.clipped(true);

                swapchainCreateInfo.oldSwapchain(VK_NULL_HANDLE);

                LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);

                if (vkCreateSwapchainKHR(device, swapchainCreateInfo, null, pSwapChain) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create swap chain");
                }

                swapchain.swapchain = pSwapChain.get(0);

                vkGetSwapchainImagesKHR(device, swapchain.swapchain, imageCount, null);

                LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));

                vkGetSwapchainImagesKHR(device, swapchain.swapchain, imageCount, pSwapchainImages);

                swapchain.images = new ArrayList<>(imageCount.get(0));

                for (int i = 0; i < pSwapchainImages.capacity(); i++) {
                    swapchain.images.add(pSwapchainImages.get(i));
                }

                swapchain.imageFormat = surfaceFormat.format();
                swapchain.extent = VkExtent2D.create().set(extent);
            }


            //TODO
            /*
            //Getting swapchain image views
            {
                LongBuffer pImageView = stack.mallocLong(1);

                for(long swapChainImage : swapchain.images) {

                    VkImageViewCreateInfo imageViewCreateInfo = VkImageViewCreateInfo.calloc(stack);

                    imageViewCreateInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
                    imageViewCreateInfo.image(swapChainImage);
                    imageViewCreateInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
                    imageViewCreateInfo.format(swapchain.imageFormat);

                    imageViewCreateInfo.components().r(VK_COMPONENT_SWIZZLE_IDENTITY);
                    imageViewCreateInfo.components().g(VK_COMPONENT_SWIZZLE_IDENTITY);
                    imageViewCreateInfo.components().b(VK_COMPONENT_SWIZZLE_IDENTITY);
                    imageViewCreateInfo.components().a(VK_COMPONENT_SWIZZLE_IDENTITY);

                    imageViewCreateInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    imageViewCreateInfo.subresourceRange().baseMipLevel(0);
                    imageViewCreateInfo.subresourceRange().levelCount(1);
                    imageViewCreateInfo.subresourceRange().baseArrayLayer(0);
                    imageViewCreateInfo.subresourceRange().layerCount(1);

                    if (vkCreateImageView(device, imageViewCreateInfo, null, pImageView) != VK_SUCCESS) {
                        throw new RuntimeException("Failed to create image views");
                    }

                    swapchain.imageViews.add(pImageView.get(0));
                }
            }

             */
        }

        for (int i = 0; i < swapchain.images.size(); i++) {
            swapchainRenderTarget.addTexture(i, new VulkanTexture(
                    this,
                    swapchain.extent.width(),
                    swapchain.extent.height(),
                    swapchain.images.get(i),
                    swapchain.imageFormat
            ));


        }










        return swapchainRenderTarget;
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

                    IntBuffer hasPresentSupport = stack.ints(VK_FALSE);

                    int graphicsFamilyIndex = 0, presentFamilyIndex = 0;

                    for (int queueFamilyIndex = 0; queueFamilyIndex < queueFamilies.capacity(); queueFamilyIndex++) {
                        VkQueueFamilyProperties queueFamilyProperties = queueFamilies.get(queueFamilyIndex);

                        if ((queueFamilyProperties.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) graphicsFamilyIndex = queueFamilyIndex;

                        vkGetPhysicalDeviceSurfaceSupportKHR(device, queueFamilyIndex, surface, hasPresentSupport);
                        if (hasPresentSupport.get(0) == VK_TRUE) presentFamilyIndex = queueFamilyIndex;


                    }
                    physicalDeviceQueueFamilies = new VulkanPhysicalDeviceQueueFamilies(graphicsFamilyIndex, presentFamilyIndex);
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
                    swapchainSupportDetails = new VulkanSwapchainSupportInfo();
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
        VkPhysicalDeviceProperties physicalDeviceProperties = VkPhysicalDeviceProperties.calloc();
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


        Logger.info(VulkanRenderer.class, "Unable to find sRGB VkSurfaceFormatKHR, using default. Colors may look incorrect.");

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


    private VulkanPipeline createPipeline(VkDevice device, VulkanSwapchain swapchain, VulkanShaderProgram vulkanShaderProgram) {



        VertexAttributes.Type[] attributes = vulkanShaderProgram.getAttributes();
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
                for(VertexAttributes.Type attribute : attributes){
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
                depthStencil.depthTestEnable(true);
                depthStencil.depthWriteEnable(true);
                depthStencil.depthCompareOp(toVkDepthCompareOpEnum(DepthTestType.LessThan));
                depthStencil.minDepthBounds(0f);
                depthStencil.maxDepthBounds(1f);


                depthStencil.stencilTestEnable(false);


            }



            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            {
                pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
                pipelineLayoutInfo.setLayoutCount(vulkanShaderProgram.getShaderResSets().length);
                pipelineLayoutInfo.pSetLayouts(stack.longs(vulkanShaderProgram.getAllDescriptorSetLayouts()));


            }
            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            pipelineLayout = pPipelineLayout.get(0);

            VkPipelineRenderingCreateInfoKHR pipelineRenderingCreateInfoKHR = VkPipelineRenderingCreateInfoKHR.calloc(stack);
            pipelineRenderingCreateInfoKHR.colorAttachmentCount(1);
            pipelineRenderingCreateInfoKHR.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO_KHR);
            pipelineRenderingCreateInfoKHR.pColorAttachmentFormats(stack.ints(swapchain.imageFormat));
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
                pipelineInfo.stageCount(vulkanShaderProgram.getShaderStages().capacity());
                pipelineInfo.pStages(vulkanShaderProgram.getShaderStages());
                pipelineInfo.pVertexInputState(vertexInputInfo);
                pipelineInfo.pNext(pipelineRenderingCreateInfoKHR);
            }


            LongBuffer pGraphicsPipeline = stack.mallocLong(1);


            if(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }


            graphicsPipeline = pGraphicsPipeline.get(0);


        }

        return new VulkanPipeline(pipelineLayout, graphicsPipeline);
    }

    private int toVkDepthCompareOpEnum(DepthTestType depthTestType) {
        if(depthTestType == null) return -1;
        switch(depthTestType) {
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

        throw new RuntimeException(Logger.error(VulkanRenderer.class, "The depth operation for this pipeline is an invalid value [" + depthTestType + "]"));
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

    private void createSwapchainAndDepthResources(int width, int height) {
        swapchainRenderTarget = createSwapchainRenderTarget(settings);

        depthImage = new VulkanImage(
                this,
                allocator,
                device,
                width,
                height,
                VK_FORMAT_D32_SFLOAT ,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                VK_IMAGE_TILING_OPTIMAL
        );

        depthImageView = new VulkanImageView(this, device, depthImage, VK_IMAGE_ASPECT_DEPTH_BIT);
        frameIndex = 0;
    }

    private void disposeSwapchainAndDepthResources(){
        vkDeviceWaitIdle(device);

        for (int frameIndex = 0; frameIndex < maxFramesInFlight; frameIndex++) {
            Texture texture = swapchainRenderTarget.getTexture(frameIndex);
            texture.dispose();
            this.remove(texture);
        }



        vkDestroySwapchainKHR(device, swapchain.swapchain, null);

        depthImageView.dispose();
        depthImage.dispose();

        this.remove(depthImageView);
        this.remove(depthImage);





    }




    public VkDevice getDevice() {
        return device;
    }


    @Override
    public StaticMeshBatch newStaticMeshBatch(int maxVertexCount, int maxIndexCount, int maxTransformCount, ShaderProgram shaderProgram) {
        Logger.info(VulkanRenderer.class,
                "Creating new StaticMeshBatch with:\n" +
                        "\t " + maxVertexCount + " max vertices\n" +
                        "\t " + maxIndexCount + " max indices\n" +
                        "\t " + maxTransformCount + " max transforms\n"
        );

        VulkanPipeline pipeline = createPipeline(device, swapchain, (VulkanShaderProgram) shaderProgram);
        VulkanStaticMeshBatch vulkanStaticMeshBatch = new VulkanStaticMeshBatch(this, shaderProgram, getMaxFramesInFlight(), sharedCommandPool, graphicsQueue, device, pipeline, maxVertexCount, maxIndexCount, maxTransformCount);

        return vulkanStaticMeshBatch;
    }



    @Override
    public void destroyStaticMeshBatch(StaticMeshBatch staticMeshBatch) {
        VulkanStaticMeshBatch vulkanStaticMeshBatch = (VulkanStaticMeshBatch) staticMeshBatch;

        for(int frameIndex = 0; frameIndex < getMaxFramesInFlight(); frameIndex++) {
            Buffer transformsBuffer = vulkanStaticMeshBatch.getTransformsBuffers()[frameIndex];
            Buffer cameraBuffer = vulkanStaticMeshBatch.getCameraBuffers()[frameIndex];

            transformsBuffer.dispose();
            cameraBuffer.dispose();

            this.remove(transformsBuffer);
            this.remove(cameraBuffer);

        }

        VulkanPipeline vulkanPipeline = vulkanStaticMeshBatch.getPipeline();

        vkDestroyPipeline(device, vulkanPipeline.pipeline, null);
        vkDestroyPipelineLayout(device, vulkanPipeline.pipelineLayout, null);

        vulkanStaticMeshBatch.getVertexBuffer().dispose();
        vulkanStaticMeshBatch.getIndexBuffer().dispose();

        this.remove(vulkanStaticMeshBatch.getVertexBuffer());
        this.remove(vulkanStaticMeshBatch.getIndexBuffer());
    }

    @Override
    public void destroyDynamicMesh(DynamicMesh dynamicMesh) {
        VulkanDynamicMesh vulkanDynamicMesh = (VulkanDynamicMesh) dynamicMesh;

        for(int frameIndex = 0; frameIndex < getMaxFramesInFlight(); frameIndex++) {
            Buffer transformsBuffer = vulkanDynamicMesh.getTransformsBuffers()[frameIndex];
            Buffer cameraBuffer = vulkanDynamicMesh.getCameraBuffers()[frameIndex];

            transformsBuffer.dispose();
            cameraBuffer.dispose();

            this.remove(transformsBuffer);
            this.remove(cameraBuffer);

        }

        VulkanPipeline vulkanPipeline = vulkanDynamicMesh.getPipeline();

        vkDestroyPipeline(device, vulkanPipeline.pipeline, null);
        vkDestroyPipelineLayout(device, vulkanPipeline.pipelineLayout, null);

        vulkanDynamicMesh.getVertexBuffer().dispose();
        vulkanDynamicMesh.getIndexBuffer().dispose();

        this.remove(vulkanDynamicMesh.getVertexBuffer());
        this.remove(vulkanDynamicMesh.getIndexBuffer());
    }

    @Override
    public SpriteBatch newSpriteBatch(int maxVertexCount, int maxIndexCount, ShaderProgram shaderProgram, Camera camera) {
        VulkanPipeline pipeline = createPipeline(device, swapchain, (VulkanShaderProgram) shaderProgram);
        VulkanSpriteBatch vulkanSpriteBatch = new VulkanSpriteBatch(
                this,
                getMaxFramesInFlight(),
                pipeline,
                maxVertexCount,
                maxIndexCount,
                camera,
                shaderProgram
        );

        return vulkanSpriteBatch;
    }


    @Override
    public DynamicMesh newDynamicMesh(int maxVertexCount, int maxIndexCount, ShaderProgram shaderProgram) {

        VulkanPipeline pipeline = createPipeline(device, swapchain, (VulkanShaderProgram) shaderProgram);
        VulkanDynamicMesh vulkanDynamicMesh = new VulkanDynamicMesh(
                this,
                shaderProgram,
                getMaxFramesInFlight(),
                pipeline,
                maxVertexCount,
                maxIndexCount
        );


        return vulkanDynamicMesh;
    }

    @Override
    public void dispatch(Scene scene, SpriteBatch spriteBatch, boolean recreateRenderer) {

        try(MemoryStack stack = stackPush()) {

            if(recreateRenderer) {
                disposeSwapchainAndDepthResources();
                this.width = surface.getWidth();
                this.height = surface.getHeight();
                createSwapchainAndDepthResources(this.width, this.height);
            }







            VulkanFrame frame = frames[frameIndex];
            IntBuffer pImageIndex = stack.ints(0);

            vkWaitForFences(device, frame.inFlightFence, true, UINT64_MAX);
            vkAcquireNextImageKHR(device, swapchain.swapchain, UINT64_MAX, frame.imageAcquiredSemaphore, VK_NULL_HANDLE, pImageIndex);


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

                    VulkanTexture swapchainTexture = (VulkanTexture) swapchainRenderTarget.getTexture(frameIndex);


                    colorAttachment.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR);
                    colorAttachment.imageView(swapchainTexture.getImageView());
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
                renderArea.extent(swapchain.extent);
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
                    startBarrier.image(swapchain.images.get(frameIndex));
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
                    viewport.width(swapchain.extent.width());
                    viewport.height(swapchain.extent.height());


                    viewport.minDepth(0.0f);
                    viewport.maxDepth(1.0f);


                    vkCmdSetViewport(commandBuffer, 0, viewport);

                    VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
                    {
                        scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
                        scissor.extent(swapchain.extent);
                    }

                    vkCmdSetScissor(commandBuffer, 0, scissor);


                    for(StaticMeshBatch staticMeshBatch : scene.getStaticMeshBatches().values()) {
                        VulkanStaticMeshBatch vulkanStaticMeshBatch = (VulkanStaticMeshBatch) staticMeshBatch;

                        VulkanBuffer vertexBuffer = (VulkanBuffer) vulkanStaticMeshBatch.getVertexBuffer();
                        VulkanBuffer indexBuffer = (VulkanBuffer) vulkanStaticMeshBatch.getIndexBuffer();




                        VulkanPipeline pipeline = vulkanStaticMeshBatch.getPipeline();
                        VulkanShaderProgram shaderProgram = (VulkanShaderProgram) vulkanStaticMeshBatch.shaderProgram;
                        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.pipeline);

                        LongBuffer vertexBuffers = stack.longs(vertexBuffer.getHandle());
                        LongBuffer offsets = stack.longs(0);

                        vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);

                        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                pipeline.pipelineLayout, 0, stack.longs(shaderProgram.getDescriptorSets(frameIndex)), null);

                        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getHandle(), 0, VK_INDEX_TYPE_UINT32);
                        vkCmdDrawIndexed(commandBuffer, vulkanStaticMeshBatch.indexCount, 1, 0, 0, 0);

                    }

                    for(DynamicMesh dynamicMesh : scene.getDynamicMeshes()) {
                        VulkanDynamicMesh vulkanDynamicMesh = (VulkanDynamicMesh) dynamicMesh;

                        VulkanBuffer vertexBuffer = (VulkanBuffer) vulkanDynamicMesh.getVertexBuffer();
                        VulkanBuffer indexBuffer = (VulkanBuffer) vulkanDynamicMesh.getIndexBuffer();




                        VulkanPipeline pipeline = vulkanDynamicMesh.getPipeline();
                        VulkanShaderProgram shaderProgram = (VulkanShaderProgram) vulkanDynamicMesh.getShaderProgram();
                        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.pipeline);

                        LongBuffer vertexBuffers = stack.longs(vertexBuffer.getHandle());
                        LongBuffer offsets = stack.longs(0);

                        vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);

                        vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                                pipeline.pipelineLayout, 0, stack.longs(shaderProgram.getDescriptorSets(frameIndex)), null);

                        vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getHandle(), 0, VK_INDEX_TYPE_UINT32);
                        vkCmdDrawIndexed(commandBuffer, vulkanDynamicMesh.getIndexCount(), 1, 0, 0, 0);

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
                    endBarrier.image(swapchain.images.get(frameIndex));
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

            vkQueueSubmit(graphicsQueue, submitInfo, frame.inFlightFence);




            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(stack.longs(frame.renderFinishedSemaphore));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapchain.swapchain));

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
    public int getMaxStaticMeshBatchCount() {
        return 10;
    }

    @Override
    public RenderTarget getSwapchainRenderTarget() {
        return swapchainRenderTarget;
    }


    @Override
    public void dispose() {
        vkDeviceWaitIdle(device);

        vkDestroySwapchainKHR(device, swapchain.swapchain, null);


        for(VulkanFrame frame : frames){
            vkDestroySemaphore(device, frame.imageAcquiredSemaphore, null);
            vkDestroySemaphore(device, frame.renderFinishedSemaphore, null);
            vkDestroyFence(device, frame.inFlightFence, null);
        }





        vkDestroyCommandPool(device, sharedCommandPool, null);

        vmaDestroyAllocator(allocator.getId());
        vkDestroySurfaceKHR(instance, vkSurface, null);
        EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);

        physicalDeviceProperties.free();
        vkDestroyDevice(device, null);

        vkDestroyInstance(instance, null);
    }

}