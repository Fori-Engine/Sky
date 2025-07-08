package fori.graphics.vulkan;

import fori.Logger;
import fori.Surface;
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

import static fori.graphics.vulkan.VulkanUtil.UINT64_MAX;
import static java.lang.Math.clamp;
import static java.util.stream.Collectors.toSet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.vma.Vma.vmaDestroyAllocator;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK13.*;


public class VulkanRenderer extends Renderer {

    public static final int FRAMES_IN_FLIGHT = 2;

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

    private long sharedCommandPool;
    private long vkSurface;
    private VkInstance instance;

    private VulkanAllocator allocator;
    private int acquiredImageIndex;


    public VulkanRenderer(Disposable parent, VkInstance instance, long vkSurface, int width, int height, RendererSettings rendererSettings, long debugMessenger, Surface surface) {
        super(parent, width, height, FRAMES_IN_FLIGHT, rendererSettings, surface);
        this.instance = instance;
        this.vkSurface = vkSurface;
        this.debugMessenger = debugMessenger;


        physicalDevice = selectPhysicalDevice(instance, vkSurface);
        physicalDeviceProperties = getPhysicalDeviceProperties(physicalDevice);
        VulkanDeviceManager.setPhysicalDeviceProperties(physicalDeviceProperties);
        Logger.info(VulkanRenderer.class, "Selected Physical Device " + physicalDeviceProperties.deviceNameString());
        device = createDevice(physicalDevice);

        VulkanDeviceManager.setCurrentDevice(device);
        VulkanDeviceManager.setCurrentPhysicalDevice(physicalDevice);
        VulkanAllocator.init(instance, device, physicalDevice);
        allocator = VulkanAllocator.getAllocator();

        graphicsQueue = getGraphicsQueue(device);
        VulkanDeviceManager.setGraphicsQueue(graphicsQueue);
        presentQueue = getPresentQueue(device);
        VulkanDeviceManager.setGraphicsFamilyIndex(physicalDeviceQueueFamilies.graphicsFamily);

        sharedCommandPool = VulkanUtil.createCommandPool(device, VulkanDeviceManager.getGraphicsFamilyIndex());
        swapchainRenderTarget = createSwapchainRenderTarget(rendererSettings);

        frameStartSemaphores = new VulkanSemaphore[maxFramesInFlight];
        for (int i = 0; i < maxFramesInFlight; i++) {
            frameStartSemaphores[i] = new VulkanSemaphore(this, device);
        }


    }



    private RenderTarget createSwapchainRenderTarget(RendererSettings rendererSettings) {
        RenderTarget swapchainRenderTarget = new RenderTarget(this, maxFramesInFlight + 1);


        try(MemoryStack stack = stackPush()) {


            //Getting Swapchain Images
            {

                swapchainSupportDetails.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
                vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, vkSurface, swapchainSupportDetails.capabilities);


                VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapchainSupportDetails.formats);
                int presentMode = chooseSwapPresentMode(swapchainSupportDetails.presentModes, rendererSettings.vsync);
                VkExtent2D extent = chooseSwapExtent(swapchainSupportDetails.capabilities, width, height);


                IntBuffer imageCount = stack.ints(swapchainSupportDetails.capabilities.minImageCount());


                if (swapchainSupportDetails.capabilities.maxImageCount() > 0 && imageCount.get(0) > swapchainSupportDetails.capabilities.maxImageCount()) {
                    imageCount.put(0, swapchainSupportDetails.capabilities.maxImageCount());
                }


                swapchain = new VulkanSwapchain(
                        this,
                        vkSurface,
                        imageCount.get(0),
                        surfaceFormat.format(),
                        surfaceFormat.colorSpace(),
                        presentMode,
                        physicalDeviceQueueFamilies,
                        swapchainSupportDetails,
                        extent
                );




                vkGetSwapchainImagesKHR(device, swapchain.handle, imageCount, null);

                LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));

                vkGetSwapchainImagesKHR(device, swapchain.handle, imageCount, pSwapchainImages);

                for (int i = 0; i < pSwapchainImages.capacity(); i++) {
                    swapchain.images.add(pSwapchainImages.get(i));
                }
            }

        }

        for (int i = 0; i < swapchain.images.size(); i++) {
            swapchainRenderTarget.addTexture(i, new VulkanTexture(
                    swapchainRenderTarget,
                    swapchain.extent.width(),
                    swapchain.extent.height(),
                    swapchain.images.get(i),
                    swapchain.imageFormat,
                    VK_IMAGE_ASPECT_COLOR_BIT
            ));
        }



        swapchainRenderTarget.addTexture(
                maxFramesInFlight,
                new VulkanTexture(
                        swapchainRenderTarget,
                        swapchain.extent.width(),
                        swapchain.extent.height(),
                        null,
                        Texture.Filter.Nearest,
                        Texture.Filter.Nearest,
                        VK_FORMAT_D32_SFLOAT,
                        VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                        VK_IMAGE_TILING_OPTIMAL,
                        VK_IMAGE_ASPECT_DEPTH_BIT
                )
        );


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
    private VkDevice createDevice(VkPhysicalDevice physicalDevice) {

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
    private VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities, int width, int height) {
        VkExtent2D extent = VkExtent2D.calloc().set(width, height);

        if(capabilities.currentExtent().width() != UINT64_MAX) {
            return extent;
        }

        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();

        extent.width(clamp(extent.width(), minExtent.width(), maxExtent.width()));
        extent.height(clamp(extent.height(), minExtent.height(), maxExtent.height()));



        return extent;
    }




    private void createSwapchainRenderTarget() {
        swapchainRenderTarget = createSwapchainRenderTarget(settings);
        frameIndex = 0;
    }

    private void disposeSwapchainRenderTarget(){
        swapchain.disposeAll();
        this.remove(swapchain);

        swapchainRenderTarget.disposeAll();
        this.remove(swapchainRenderTarget);
    }




    public VkDevice getDevice() {
        return device;
    }


    @Override
    public StaticMeshBatch newStaticMeshBatch(int maxVertexCount, int maxIndexCount, int maxTransformCount, ShaderProgram shaderProgram) {

        return new VulkanStaticMeshBatch(this, shaderProgram, getMaxFramesInFlight(), sharedCommandPool, graphicsQueue, device, maxVertexCount, maxIndexCount, maxTransformCount);
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


        vulkanDynamicMesh.getVertexBuffer().dispose();
        vulkanDynamicMesh.getIndexBuffer().dispose();

        this.remove(vulkanDynamicMesh.getVertexBuffer());
        this.remove(vulkanDynamicMesh.getIndexBuffer());
    }


    @Override
    public DynamicMesh newDynamicMesh(int maxVertexCount, int maxIndexCount, ShaderProgram shaderProgram) {

        VulkanDynamicMesh vulkanDynamicMesh = new VulkanDynamicMesh(
                this,
                shaderProgram,
                getMaxFramesInFlight(),
                maxVertexCount,
                maxIndexCount
        );


        return vulkanDynamicMesh;
    }


    @Override
    public void update(boolean surfaceInvalidated) {

        if (surfaceInvalidated) {
            disposeSwapchainRenderTarget();
            this.width = surface.getWidth();
            this.height = surface.getHeight();
            createSwapchainRenderTarget();
        }

        try(MemoryStack stack = stackPush()) {

            IntBuffer pImageIndex = stack.callocInt(1);

            vkAcquireNextImageKHR(
                    device,
                    swapchain.handle,
                    VulkanUtil.UINT64_MAX,
                    ((VulkanSemaphore[]) frameStartSemaphores)[frameIndex].getHandle(),
                    VK_NULL_HANDLE,
                    pImageIndex
            );

            acquiredImageIndex = pImageIndex.get(0);
        }


        for(CommandList commandList : commandLists) {
            commandList.run();
        }
        Semaphore[] finishedSemaphores = commandLists.getLast().getFinishedSemaphores();



        try(MemoryStack stack = stackPush()) {
            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(stack.longs(
                    ((VulkanSemaphore[]) finishedSemaphores)[frameIndex].getHandle()
            ));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapchain.handle));
            presentInfo.pImageIndices(stack.ints(acquiredImageIndex));

            vkQueuePresentKHR(presentQueue, presentInfo);

            frameIndex = (frameIndex + 1) % FRAMES_IN_FLIGHT;
        }

        commandLists.clear();
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
    public void dispose() {
        vkDeviceWaitIdle(device);


        vkDestroyCommandPool(device, sharedCommandPool, null);
        vmaDestroyAllocator(allocator.getId());
        vkDestroySurfaceKHR(instance, vkSurface, null);
        EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(instance, debugMessenger, null);

        physicalDeviceProperties.free();
        vkDestroyDevice(device, null);

        vkDestroyInstance(instance, null);
    }

}