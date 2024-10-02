package fori.graphics.vulkan;

import fori.Logger;
import fori.asset.AssetPacks;
import fori.graphics.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Math.clamp;
import static java.util.stream.Collectors.toSet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK13.*;
import static fori.graphics.ShaderRes.Type.*;
import static fori.graphics.ShaderRes.ShaderStage.*;


public class VkSceneRenderer extends SceneRenderer {

    private static final List<String> validationLayers = new ArrayList<>();
    static {
        validationLayers.add("VK_LAYER_KHRONOS_validation");
    }

    public static final int FRAMES_IN_FLIGHT = 2;

    private static final int UINT64_MAX = 0xFFFFFFFF;
    private long debugMessenger;
    private VkPhysicalDevice physicalDevice;
    private VkPhysicalDeviceProperties physicalDeviceProperties;
    private VkPhysicalDeviceQueueFamilies physicalDeviceQueueFamilies;
    private static final Set<String> DEVICE_EXTENSIONS = Stream.of(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
            .collect(toSet());

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
    private VkPipeline pipeline;
    private long sharedCommandPool;
    private boolean resized = false;
    private ShaderProgram shaderProgram;
    private Buffer vertexBuffer, indexBuffer, cameraBuffer, transformsBuffer;


    private VkImage depthBuffer;
    private VkImageView depthBufferImageView;
    private VkGlobalAllocator allocator;





    public VkSceneRenderer(VkInstance instance, long surface, int width, int height, RendererSettings rendererSettings, long debugMessenger) {
        this(width, height, rendererSettings);
        this.instance = instance;
        this.surface = surface;
        this.debugMessenger = debugMessenger;


        physicalDevice = selectPhysicalDevice(instance, surface);
        physicalDeviceProperties = getPhysicalDeviceProperties(physicalDevice);
        VkContextManager.setPhysicalDeviceProperties(physicalDeviceProperties);
        Logger.info(VkSceneRenderer.class, "Selected Physical Device " + physicalDeviceProperties.deviceNameString());
        device = createDevice(physicalDevice, rendererSettings.validation);

        VkContextManager.setCurrentDevice(device);
        VkContextManager.setCurrentPhysicalDevice(physicalDevice);






        graphicsQueue = getGraphicsQueue(device);
        VkContextManager.setGraphicsQueue(graphicsQueue);
        presentQueue = getPresentQueue(device);
        VkContextManager.setGraphicsFamilyIndex(physicalDeviceQueueFamilies.graphicsFamily);
        swapchain = createSwapChain(device, surface, width, height, rendererSettings.vsync);
        swapchainImageViews = createSwapchainImageViews(device, swapchain);

        Logger.info(VkSceneRenderer.class, "Max Allowed Allocations: " + physicalDeviceProperties.limits().maxMemoryAllocationCount());


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

        ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                AssetPacks.<String> getAsset("core:assets/shaders/vulkan/Default.glsl").asset
        );

        int matrixSizeBytes = 4 * 4 * Float.BYTES;

        shaderProgram = ShaderProgram.newShaderProgram(shaderSources.vertexShader, shaderSources.fragmentShader);
        shaderProgram.bind(
                new ShaderResSet(
                        0,
                        new ShaderRes(
                                "camera",
                                0,
                                UniformBuffer,
                                VertexStage
                        ).sizeBytes(2 * matrixSizeBytes),
                        new ShaderRes(
                                "transforms",
                                1,
                                ShaderStorageBuffer,
                                VertexStage
                        ).sizeBytes(10 * matrixSizeBytes),
                        new ShaderRes(
                                "materials",
                                2,
                                CombinedSampler,
                                FragmentStage
                        )
                )
        );


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


        VkGlobalAllocator.init(instance, device, physicalDevice);
        allocator = VkGlobalAllocator.getAllocator();



        vertexBuffer = Buffer.newBuffer(
                2 * 4 * 4 * Float.BYTES,
                Buffer.Usage.VertexBuffer,
                Buffer.Type.CPUGPUShared
        );

        ByteBuffer vertexBufferData = vertexBuffer.map();
        {
            vertexBufferData.putFloat(-0.5f);
            vertexBufferData.putFloat(-0.5f);
            vertexBufferData.putFloat(0.5f);
            vertexBufferData.putFloat(0);

            vertexBufferData.putFloat(0.5f);
            vertexBufferData.putFloat(-0.5f);
            vertexBufferData.putFloat(0.5f);
            vertexBufferData.putFloat(0);


            vertexBufferData.putFloat(0.5f);
            vertexBufferData.putFloat(0.5f);
            vertexBufferData.putFloat(0.5f);
            vertexBufferData.putFloat(0);


            vertexBufferData.putFloat(-0.5f);
            vertexBufferData.putFloat(0.5f);
            vertexBufferData.putFloat(0.5f);
            vertexBufferData.putFloat(0);
        }
        {
            vertexBufferData.putFloat(-0.5f);
            vertexBufferData.putFloat(-0.5f);
            vertexBufferData.putFloat(-0.5f);
            vertexBufferData.putFloat(1);

            vertexBufferData.putFloat(0.5f);
            vertexBufferData.putFloat(-0.5f);
            vertexBufferData.putFloat(-0.5f);
            vertexBufferData.putFloat(1);

            vertexBufferData.putFloat(0.5f);
            vertexBufferData.putFloat(0.5f);
            vertexBufferData.putFloat(-0.5f);
            vertexBufferData.putFloat(1);

            vertexBufferData.putFloat(-0.5f);
            vertexBufferData.putFloat(0.5f);
            vertexBufferData.putFloat(-0.5f);
            vertexBufferData.putFloat(1);
        }

        indexBuffer = Buffer.newBuffer(
                Integer.BYTES * 6 * 2,
                Buffer.Usage.IndexBuffer,
                Buffer.Type.CPUGPUShared
        );

        ByteBuffer indexBufferData = indexBuffer.map();
        {
            indexBufferData.putInt(0);
            indexBufferData.putInt(1);
            indexBufferData.putInt(2);
            indexBufferData.putInt(2);
            indexBufferData.putInt(3);
            indexBufferData.putInt(0);
        }
        {
            indexBufferData.putInt(4);
            indexBufferData.putInt(5);
            indexBufferData.putInt(6);
            indexBufferData.putInt(6);
            indexBufferData.putInt(7);
            indexBufferData.putInt(4);
        }


        cameraBuffer = Buffer.newBuffer(
                matrixSizeBytes * 2,
                Buffer.Usage.UniformBuffer,
                Buffer.Type.CPUGPUShared
        );

        ByteBuffer cameraBufferData = cameraBuffer.map();
        {
            Matrix4f view = new Matrix4f().lookAt(new Vector3f(1.0f, 2.0f, 3.0f), new Vector3f(0, 0, 0), new Vector3f(0.0f, 0.0f, 1.0f));
            view.get(0, cameraBufferData);

            Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(35.0f), (float) width / height, 0.01f, 100.0f, true);
            proj.m11(proj.m11() * -1);
            proj.get(matrixSizeBytes, cameraBufferData);
        }


        transformsBuffer = Buffer.newBuffer(
                matrixSizeBytes * 10,
                Buffer.Usage.ShaderStorageBuffer,
                Buffer.Type.CPUGPUShared
        );


        ByteBuffer transformsBufferData = transformsBuffer.map();
        {
            Matrix4f transform0 = new Matrix4f().rotate((float) Math.toRadians(30.0f), 0.0f, 1.0f, 0.0f);
            transform0.get(0, transformsBufferData);

            Matrix4f transform1 = new Matrix4f().rotate((float) Math.toRadians(90.0f), 0.0f, 0.0f, 1.0f);
            transform1.get(matrixSizeBytes, transformsBufferData);
        }



        Texture texture = Texture.newTexture(AssetPacks.getAsset("core:assets/ForiEngine.png"), Texture.Filter.Nearest, Texture.Filter.Nearest);


        for (int i = 0; i < FRAMES_IN_FLIGHT; i++) {

            shaderProgram.updateBuffers(
                    i,
                    new ShaderUpdate<>("camera", 0, 0, cameraBuffer),
                    new ShaderUpdate<>("transforms", 0, 1, transformsBuffer)
            );

            shaderProgram.updateTextures(
                    i,
                    new ShaderUpdate<>("materials", 0, 2, texture).arrayIndex(0)
            );

        }

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


        depthBuffer = new VkImage(
                allocator,
                device,
                swapchain.swapChainExtent.width(),
                swapchain.swapChainExtent.height(),
                VK_FORMAT_D32_SFLOAT ,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                VK_IMAGE_TILING_OPTIMAL
        );

        depthBufferImageView = new VkImageView(device, depthBuffer, VK_IMAGE_ASPECT_DEPTH_BIT);







        renderPass = createRenderPass(device, swapchain, depthBuffer);
        swapchainFramebuffers = createSwapchainFramebuffers(device, swapchain, swapchainImageViews, renderPass, depthBufferImageView);

        pipeline = createPipeline(device, swapchain, ((VkShaderProgram) shaderProgram).getShaderStages(), renderPass);






















    }
    public VkSceneRenderer(int width, int height, RendererSettings rendererSettings) {
        super(width, height, rendererSettings);
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
            if(availableFormat.format() == VK_FORMAT_B8G8R8A8_SRGB && availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR){
                return availableFormat;
            }
        }


        Logger.info(VkSceneRenderer.class, "Unable to find sRGB VkSurfaceFormatKHR, using default. Colors may look incorrect.");

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
    private ArrayList<Long> createSwapchainFramebuffers(VkDevice device, VkSwapchain swapchain, List<Long> swapChainImageViews, long renderPass, VkImageView depthBufferImageView) {

        ArrayList<Long> swapChainFramebuffers = new ArrayList<>(swapChainImageViews.size());

        try(MemoryStack stack = stackPush()) {

            LongBuffer attachments = stack.mallocLong(2);
            LongBuffer pFramebuffer = stack.mallocLong(1);

            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(renderPass);
            framebufferInfo.width(swapchain.swapChainExtent.width());
            framebufferInfo.height(swapchain.swapChainExtent.height());
            framebufferInfo.layers(1);

            for(long imageView : swapChainImageViews) {

                attachments.put(0, imageView);
                attachments.put(1, depthBufferImageView.getHandle());

                framebufferInfo.pAttachments(attachments);

                if(vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer");
                }

                swapChainFramebuffers.add(pFramebuffer.get(0));
            }
        }

        return swapChainFramebuffers;
    }
    private long createRenderPass(VkDevice device, VkSwapchain swapchain, VkImage depthBuffer) {

        try(MemoryStack stack = stackPush()) {


            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.calloc(2, stack);



            VkAttachmentDescription colorAttachment = attachments.get(0);
            colorAttachment.format(swapchain.swapChainImageFormat);
            colorAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentDescription depthAttachment = attachments.get(1);
            depthAttachment.format(depthBuffer.getFormat());
            depthAttachment.samples(VK_SAMPLE_COUNT_1_BIT);
            depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            depthAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            depthAttachment.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);



            VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.calloc(1, stack);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkAttachmentReference depthAttachmentRef = VkAttachmentReference.calloc(stack);
            depthAttachmentRef.attachment(1);
            depthAttachmentRef.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);




            VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(colorAttachmentRef);
            subpass.pDepthStencilAttachment(depthAttachmentRef);

            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack);
            dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependency.dstSubpass(0);
            dependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT);
            dependency.srcAccessMask(0);
            dependency.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT);
            dependency.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(attachments);
            renderPassInfo.pSubpasses(subpass);
            renderPassInfo.pDependencies(dependency);

            LongBuffer pRenderPass = stack.mallocLong(1);

            if(vkCreateRenderPass(device, renderPassInfo, null, pRenderPass) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }

            return pRenderPass.get(0);
        }
    }
    private VkPipeline createPipeline(VkDevice device, VkSwapchain swapchain, VkPipelineShaderStageCreateInfo.Buffer shaderStages, long renderPass) {

        long pipelineLayout;
        long graphicsPipeline = 0;

        try(MemoryStack stack = stackPush()) {

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            {
                vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);


                VkVertexInputBindingDescription.Buffer bindingDescription =
                        VkVertexInputBindingDescription.calloc(1, stack);

                bindingDescription.binding(0);
                bindingDescription.stride(4 * Float.BYTES);
                bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
                vertexInputInfo.pVertexBindingDescriptions(bindingDescription);



                VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                        VkVertexInputAttributeDescription.calloc(2);

                // Position
                VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(0);
                {
                    posDescription.binding(0);
                    posDescription.location(0);
                    posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
                    posDescription.offset(0);
                }

                // Transform Index
                VkVertexInputAttributeDescription transformIndexDescription = attributeDescriptions.get(1);
                {
                    transformIndexDescription.binding(0);
                    transformIndexDescription.location(1);
                    transformIndexDescription.format(VK_FORMAT_R32_SFLOAT);
                    transformIndexDescription.offset(3 * Float.BYTES);
                }

                vertexInputInfo.pVertexAttributeDescriptions(attributeDescriptions.rewind());



            }

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            {
                inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
                inputAssembly.topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);
                inputAssembly.primitiveRestartEnable(false);
            }

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            {
                viewport.x(0.0f);
                viewport.y(0.0f);
                viewport.width(swapchain.swapChainExtent.width());
                viewport.height(swapchain.swapChainExtent.height());
                viewport.minDepth(0.0f);
                viewport.maxDepth(1.0f);
            }

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            {
                scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
                scissor.extent(swapchain.swapChainExtent);
            }
            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            {
                viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
                viewportState.pViewports(viewport);
                viewportState.pScissors(scissor);
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
                depthStencil.depthCompareOp(VK_COMPARE_OP_LESS);


                depthStencil.stencilTestEnable(false);


            }



            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            {
                pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
                pipelineLayoutInfo.setLayoutCount(4);
                pipelineLayoutInfo.pSetLayouts(stack.longs(((VkShaderProgram) shaderProgram).getAllDescriptorSetLayouts()));


            }
            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if(vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            pipelineLayout = pPipelineLayout.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            {
                pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
                pipelineInfo.pStages(shaderStages);
                pipelineInfo.pVertexInputState(vertexInputInfo);
                pipelineInfo.pInputAssemblyState(inputAssembly);
                pipelineInfo.pViewportState(viewportState);
                pipelineInfo.pRasterizationState(rasterizer);
                pipelineInfo.pMultisampleState(multisampling);
                pipelineInfo.pColorBlendState(colorBlending);
                pipelineInfo.pDepthStencilState(depthStencil);
                pipelineInfo.layout(pipelineLayout);
                pipelineInfo.renderPass(renderPass);
                pipelineInfo.subpass(0);
                pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
                pipelineInfo.basePipelineIndex(-1);
            }

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);


            if(vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }


            graphicsPipeline = pGraphicsPipeline.get(0);


        }

        return new VkPipeline(pipelineLayout, graphicsPipeline);
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





    private void recreateDisplay(){

        disposeDisplay();
        swapchain = createSwapChain(device, surface, width, height, settings.vsync);
        swapchainImageViews = createSwapchainImageViews(device, swapchain);

        depthBuffer = new VkImage(
                allocator,
                device,
                swapchain.swapChainExtent.width(),
                swapchain.swapChainExtent.height(),
                VK_FORMAT_D32_SFLOAT ,
                VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                VK_IMAGE_TILING_OPTIMAL
        );

        depthBufferImageView = new VkImageView(device, depthBuffer, VK_IMAGE_ASPECT_DEPTH_BIT);

        renderPass = createRenderPass(device, swapchain, depthBuffer);
        swapchainFramebuffers = createSwapchainFramebuffers(device, swapchain, swapchainImageViews, renderPass, depthBufferImageView);

        pipeline = createPipeline(device, swapchain, ((VkShaderProgram) shaderProgram).getShaderStages(), renderPass);
        frameIndex = 0;
    }

    private void disposeDisplay(){


        vkDeviceWaitIdle(device);
        vkDestroyPipeline(device, pipeline.pipeline, null);

        vkDestroyImage(device, depthBuffer.getHandle(), null);
        vkDestroyImageView(device, depthBufferImageView.getHandle(), null);

        for(long swapchainFramebuffer : swapchainFramebuffers){
            vkDestroyFramebuffer(device, swapchainFramebuffer, null);
        }

        vkDestroyRenderPass(device, renderPass, null);


        for(long swapchainImageView : swapchainImageViews){
            vkDestroyImageView(device, swapchainImageView, null);
        }
        vkDestroySwapchainKHR(device, swapchain.swapChain, null);
    }




    public VkDevice getDevice() {
        return device;
    }

    @Override
    public void onSurfaceResized(int width, int height) {
        this.width = width;
        this.height = height;

        resized = true;
    }

    @Override
    public void update() {

        try(MemoryStack stack = stackPush()) {
            VkFrame frame = frames[frameIndex];
            IntBuffer pImageIndex = stack.ints(0);

            vkWaitForFences(device, frame.inFlightFence, true, UINT64_MAX);


            int acquireNextImageResult = vkAcquireNextImageKHR(device, swapchain.swapChain, UINT64_MAX, frame.imageAcquiredSemaphore, VK_NULL_HANDLE, pImageIndex);
            
            if(acquireNextImageResult == VK_ERROR_OUT_OF_DATE_KHR){
                recreateDisplay();
                return;
            }
            

            
            
            int imageIndex = pImageIndex.get(0);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(stack.longs(frame.imageAcquiredSemaphore));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            submitInfo.pCommandBuffers(stack.pointers(frame.renderCommandBuffer));
            submitInfo.pSignalSemaphores(stack.longs(frame.renderFinishedSemaphore));

            vkResetFences(device, frame.inFlightFence);
            vkResetCommandBuffer(frame.renderCommandBuffer, 0);
            {

                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
                beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

                VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
                renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);

                renderPassInfo.renderPass(renderPass);

                VkRect2D renderArea = VkRect2D.calloc(stack);
                renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
                renderArea.extent(swapchain.swapChainExtent);
                renderPassInfo.renderArea(renderArea);

                VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
                clearValues.get(0).color().float32(stack.floats(0.3f, 0.3f, 0.3f, 1.0f));
                clearValues.get(1).depthStencil().set(1.0f, 0);

                renderPassInfo.pClearValues(clearValues);

                VkCommandBuffer commandBuffer = frame.renderCommandBuffer;

                if(vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to begin recording command buffer");
                }

                renderPassInfo.framebuffer(swapchainFramebuffers.get(frameIndex));


                vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
                {
                    VkBuffer vertexBuffer = ((VkBuffer) this.vertexBuffer);
                    VkBuffer indexBuffer = ((VkBuffer) this.indexBuffer);

                    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.pipeline);
                    LongBuffer vertexBuffers = stack.longs(vertexBuffer.getHandle());
                    LongBuffer offsets = stack.longs(0);

                    vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
                    vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getHandle(), 0, VK_INDEX_TYPE_UINT32);
                    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipeline.pipelineLayout, 0, stack.longs(((VkShaderProgram) shaderProgram).getDescriptorSets(frameIndex)), null);




                    vkCmdDrawIndexed(commandBuffer, 12, 1, 0, 0, 0);
                }
                vkCmdEndRenderPass(commandBuffer);


                if(vkEndCommandBuffer(commandBuffer) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to record command buffer");
                }









            }

            if(vkQueueSubmit(graphicsQueue, submitInfo, frame.inFlightFence) != VK_SUCCESS) {
                throw new RuntimeException("Failed to submit draw command buffer");
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);
            presentInfo.pWaitSemaphores(stack.longs(frame.renderFinishedSemaphore));
            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(swapchain.swapChain));

            presentInfo.pImageIndices(pImageIndex);

            int presentResult = vkQueuePresentKHR(presentQueue, presentInfo);

            if(presentResult == VK_ERROR_OUT_OF_DATE_KHR || presentResult == VK_SUBOPTIMAL_KHR || resized){
                resized = false;
                recreateDisplay();
                return;
            }

            frameIndex = (frameIndex + 1) % FRAMES_IN_FLIGHT;

        }


    }


    @Override
    public void dispose() {

        vkDeviceWaitIdle(device);

        for(VkFrame frame : frames){
            vkDestroySemaphore(device, frame.imageAcquiredSemaphore, null);
            vkDestroySemaphore(device, frame.renderFinishedSemaphore, null);
            vkDestroyFence(device, frame.inFlightFence, null);
        }

        vkDestroyCommandPool(device, sharedCommandPool, null);






        vkDestroyRenderPass(device, renderPass, null);
        disposeDisplay();



        vkDestroyDevice(device, null);
        vkDestroyInstance(instance, null);
    }

}