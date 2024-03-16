package lake.vulkan;

import lake.graphics.StandaloneWindow;
import lake.graphics.Window;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class FastVK {
    private FastVK(){}

    private static List<String> validationLayers = new ArrayList<>();

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


    public static VkPhysicalDevice pickPhysicalDevice(VkInstance instance) {

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

                if(isDeviceSuitable(device)) {
                    physicalDevice = device;
                    break;
                }
            }

            //throw new RuntimeException("Failed to find a suitable GPU");
        }

        return physicalDevice;
    }



    private static boolean isDeviceSuitable(VkPhysicalDevice device) {

        QueueFamilyIndices indices = findQueueFamilies(device);

        return indices.isComplete();
    }

    private static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {

        QueueFamilyIndices indices = new QueueFamilyIndices();

        try(MemoryStack stack = stackPush()) {

            IntBuffer queueFamilyCount = stack.ints(0);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);

            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);

            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

            IntStream.range(0, queueFamilies.capacity())
                    .filter(index -> (queueFamilies.get(index).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0)
                    .findFirst()
                    .ifPresent(index -> indices.graphicsFamily = index);

            return indices;
        }
    }

    public static VkDeviceConfig createLogicalDevice(VkPhysicalDevice physicalDevice, boolean validation) {

        VkDevice device;
        QueueFamilyIndices indices;

        try(MemoryStack stack = stackPush()) {

            indices = findQueueFamilies(physicalDevice);

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(1, stack);

            queueCreateInfos.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
            queueCreateInfos.queueFamilyIndex(indices.graphicsFamily);
            queueCreateInfos.pQueuePriorities(stack.floats(1.0f));

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            // queueCreateInfoCount is automatically set

            createInfo.pEnabledFeatures(deviceFeatures);

            if(validation) {
                createInfo.ppEnabledLayerNames(validationLayersAsPointerBuffer(stack));
            }

            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);

            if(vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
            }

            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);


        }

        return new VkDeviceConfig(device, indices);
    }


    public static VkQueue getGraphicsQueue(VkDeviceConfig deviceConfig) {

        VkQueue graphicsQueue;

        try(MemoryStack stack = stackPush()) {
            PointerBuffer pGraphicsQueue = stack.pointers(VK_NULL_HANDLE);

            vkGetDeviceQueue(deviceConfig.device, deviceConfig.queueFamilyIndices.graphicsFamily, 0, pGraphicsQueue);

            graphicsQueue = new VkQueue(pGraphicsQueue.get(0), deviceConfig.device);
        }

        return graphicsQueue;
    }

    public static long createSurface(VkInstance instance, StandaloneWindow window) {

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

        System.err.println("Validation layer: " + callbackData.pMessageString());

        return VK_FALSE;
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

    public static void cleanupDebugMessenger(VkInstance instance){
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
}
