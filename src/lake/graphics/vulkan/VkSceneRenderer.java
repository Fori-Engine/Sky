package lake.graphics.vulkan;

import lake.FlightRecorder;
import lake.asset.AssetPacks;
import lake.graphics.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
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
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK10.vkGetInstanceProcAddr;
import static org.lwjgl.vulkan.VK13.*;


public class VkSceneRenderer extends SceneRenderer {

    private static final List<String> validationLayers = new ArrayList<>();
    static {
        validationLayers.add("VK_LAYER_KHRONOS_validation");
    }

    private static int FRAMES_IN_FLIGHT = 2;

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
    private VkBuffer vertexBuffer, indexBuffer, uniformBuffer;

    private long descriptorSet;
    private long descriptorSetLayout;



    public VkSceneRenderer(VkInstance instance, long surface, int width, int height, RendererSettings rendererSettings, long debugMessenger) {
        this(width, height, rendererSettings);
        this.instance = instance;
        this.surface = surface;
        this.debugMessenger = debugMessenger;


        physicalDevice = selectPhysicalDevice(instance, surface);
        physicalDeviceProperties = getPhysicalDeviceProperties(physicalDevice);
        FlightRecorder.info(VkSceneRenderer.class, "Selected Physical Device " + physicalDeviceProperties.deviceNameString());

        device = createDevice(physicalDevice, rendererSettings.validation);
        graphicsQueue = getGraphicsQueue(device);
        presentQueue = getPresentQueue(device);
        swapchain = createSwapChain(device, surface, width, height, rendererSettings.vsync);
        swapchainImageViews = createSwapchainImageViews(device, swapchain);

        FlightRecorder.info(VkSceneRenderer.class, "Max Allowed Allocations: " + physicalDeviceProperties.limits().maxMemoryAllocationCount());


        renderPass = createRenderPass(device, swapchain);
        swapchainFramebuffers = createSwapchainFramebuffers(device, swapchain, swapchainImageViews, renderPass);

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
        shaderProgram = ShaderProgram.newShaderProgram(this, shaderSources.vertexShader, shaderSources.fragmentShader);
        shaderProgram.prepare();


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



        vertexBuffer = new VkBuffer(
                pAllocator.get(0),
                3 * Float.BYTES * 4,
                VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                VMA_MEMORY_USAGE_CPU_TO_GPU
        );

        ByteBuffer vertexBufferData = vertexBuffer.map();


        vertexBufferData.putFloat(-0.5f);
        vertexBufferData.putFloat(-0.5f);
        vertexBufferData.putFloat(0.5f);

        vertexBufferData.putFloat(0.5f);
        vertexBufferData.putFloat(-0.5f);
        vertexBufferData.putFloat(0.5f);

        vertexBufferData.putFloat(0.5f);
        vertexBufferData.putFloat(0.5f);
        vertexBufferData.putFloat(0.5f);

        vertexBufferData.putFloat(-0.5f);
        vertexBufferData.putFloat(0.5f);
        vertexBufferData.putFloat(0.5f);



        indexBuffer = new VkBuffer(
                pAllocator.get(0),
                Integer.BYTES * 6,
                VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                VMA_MEMORY_USAGE_CPU_TO_GPU
        );

        ByteBuffer indexBufferData = indexBuffer.map();


        indexBufferData.putInt(0);
        indexBufferData.putInt(1);
        indexBufferData.putInt(2);
        indexBufferData.putInt(2);
        indexBufferData.putInt(3);
        indexBufferData.putInt(0);

        VkDescriptorPoolSize.Buffer descriptorPoolSize = VkDescriptorPoolSize.create(1);
        descriptorPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
        descriptorPoolSize.descriptorCount(1);

        VkDescriptorPoolCreateInfo descriptorPoolCreateInfo = VkDescriptorPoolCreateInfo.create();
        descriptorPoolCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
        descriptorPoolCreateInfo.pPoolSizes(descriptorPoolSize);
        descriptorPoolCreateInfo.maxSets(1);


        LongBuffer pDescriptorPool = MemoryUtil.memAllocLong(1);

        if(vkCreateDescriptorPool(device, descriptorPoolCreateInfo, null, pDescriptorPool) != VK_SUCCESS){
            throw new RuntimeException("Failed to create descriptor pool");
        }

        VkDescriptorSetLayoutBinding.Buffer descriptorSetLayoutBinding = VkDescriptorSetLayoutBinding.create(1);
        descriptorSetLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
        descriptorSetLayoutBinding.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);
        descriptorSetLayoutBinding.binding(0);
        descriptorSetLayoutBinding.descriptorCount(1);

        LongBuffer pDescriptorSetLayout = MemoryUtil.memAllocLong(1);

        VkDescriptorSetLayoutCreateInfo descriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.create();
        descriptorSetLayoutCreateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
        descriptorSetLayoutCreateInfo.pBindings(descriptorSetLayoutBinding);


        if(vkCreateDescriptorSetLayout(device, descriptorSetLayoutCreateInfo, null, pDescriptorSetLayout) != VK_SUCCESS){
            throw new RuntimeException("Failed to create descriptor set layout");
        }

        descriptorSetLayout = pDescriptorSetLayout.get(0);


        VkDescriptorSetAllocateInfo descriptorSetAllocateInfo = VkDescriptorSetAllocateInfo.create();
        descriptorSetAllocateInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
        descriptorSetAllocateInfo.descriptorPool(pDescriptorPool.get(0));
        descriptorSetAllocateInfo.pSetLayouts(pDescriptorSetLayout);

        LongBuffer pDescriptorSet = MemoryUtil.memAllocLong(1);
        if(vkAllocateDescriptorSets(device, descriptorSetAllocateInfo, pDescriptorSet) != VK_SUCCESS){
            throw new RuntimeException("Failed to allocate descriptor set");
        }
        descriptorSet = pDescriptorSet.get(0);

        int matrixSizeBytes = 4 * 4 * Float.BYTES;

        uniformBuffer = new VkBuffer(
                pAllocator.get(0),
                matrixSizeBytes * 3,
                VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                VMA_MEMORY_USAGE_CPU_TO_GPU
        );

        ByteBuffer uniformBufferData = uniformBuffer.map();

        Matrix4f model = new Matrix4f().rotate((float) Math.toRadians(0.0f), 0.0f, 0.0f, 1.0f);
        model.get(0, uniformBufferData);

        Matrix4f view = new Matrix4f().lookAt(new Vector3f(1.0f, 2.0f, 3.0f), new Vector3f(0, 0, 0), new Vector3f(0.0f, 0.0f, 1.0f));
        view.get(matrixSizeBytes, uniformBufferData);

        Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(45.0f), (float) width / height, 0.01f, 100.0f, true);
        proj.m11(proj.m11() * -1);
        proj.get(matrixSizeBytes * 2, uniformBufferData);


        Matrix4f combined = proj.mul(view.mul(model));
        System.out.println(new Vector4f(-0.5f, -0.5f, 1, 1.0f).mul(combined));



        VkDescriptorBufferInfo.Buffer uniformBufferInfo = VkDescriptorBufferInfo.create(1);
        uniformBufferInfo.buffer(uniformBuffer.getHandle());
        uniformBufferInfo.offset(0);
        uniformBufferInfo.range(uniformBuffer.getSizeBytes());

        VkWriteDescriptorSet.Buffer writeDescriptorSet = VkWriteDescriptorSet.create(1);
        writeDescriptorSet.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
        writeDescriptorSet.dstSet(pDescriptorSet.get(0));
        writeDescriptorSet.dstBinding(0);
        writeDescriptorSet.dstArrayElement(0);
        writeDescriptorSet.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
        writeDescriptorSet.pBufferInfo(uniformBufferInfo);
        writeDescriptorSet.descriptorCount(1);

        vkUpdateDescriptorSets(device, writeDescriptorSet, null);



        pipeline = createPipeline(device, swapchain, ((VkShaderProgram) shaderProgram).getShaderStages(), renderPass);
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


        FlightRecorder.info(VkSceneRenderer.class, "Unable to find sRGB VkSurfaceFormatKHR, using default. Colors may look incorrect.");

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
    private ArrayList<Long> createSwapchainFramebuffers(VkDevice device, VkSwapchain swapchain, List<Long> swapChainImageViews, long renderPass) {

        ArrayList<Long> swapChainFramebuffers = new ArrayList<>(swapChainImageViews.size());

        try(MemoryStack stack = stackPush()) {

            LongBuffer attachments = stack.mallocLong(1);
            LongBuffer pFramebuffer = stack.mallocLong(1);

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
    private long createRenderPass(VkDevice device, VkSwapchain swapchain) {

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

            VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc(1, stack);
            dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependency.dstSubpass(0);
            dependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.srcAccessMask(0);
            dependency.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(colorAttachment);
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
                bindingDescription.stride(3 * Float.BYTES);
                bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
                vertexInputInfo.pVertexBindingDescriptions(bindingDescription);



                VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                        VkVertexInputAttributeDescription.calloc(1);

                // Position
                VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(0);
                {
                    posDescription.binding(0);
                    posDescription.location(0);
                    posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
                    posDescription.offset(0);
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

                rasterizer.polygonMode(VK_POLYGON_MODE_FILL);
                rasterizer.lineWidth(3.0f);
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



            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            {
                pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
                pipelineLayoutInfo.setLayoutCount(1);
                pipelineLayoutInfo.pSetLayouts(stack.longs(descriptorSetLayout));

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

        renderPass = createRenderPass(device, swapchain);
        swapchainFramebuffers = createSwapchainFramebuffers(device, swapchain, swapchainImageViews, renderPass);

        pipeline = createPipeline(device, swapchain, ((VkShaderProgram) shaderProgram).getShaderStages(), renderPass);
        frameIndex = 0;
    }

    private void disposeDisplay(){


        vkDeviceWaitIdle(device);
        vkDestroyPipeline(device, pipeline.pipeline, null);


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

                VkClearValue.Buffer clearValues = VkClearValue.calloc(1, stack);
                clearValues.color().float32(stack.floats(0.0f, 0.0f, 0.0f, 1.0f));
                renderPassInfo.pClearValues(clearValues);

                VkCommandBuffer commandBuffer = frame.renderCommandBuffer;

                if(vkBeginCommandBuffer(commandBuffer, beginInfo) != VK_SUCCESS) {
                    throw new RuntimeException("Failed to begin recording command buffer");
                }

                renderPassInfo.framebuffer(swapchainFramebuffers.get(frameIndex));


                vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
                {
                    vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.pipeline);
                    LongBuffer vertexBuffers = stack.longs(vertexBuffer.getHandle());
                    LongBuffer offsets = stack.longs(0);

                    vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);
                    vkCmdBindIndexBuffer(commandBuffer, indexBuffer.getHandle(), 0, VK_INDEX_TYPE_UINT32);
                    vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipeline.pipelineLayout, 0, stack.longs(descriptorSet), null);




                    vkCmdDrawIndexed(commandBuffer, 6, 1, 0, 0, 0);
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