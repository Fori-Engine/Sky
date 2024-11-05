package fori.graphics.vulkan;

import fori.Logger;
import fori.Surface;
import fori.GLFWSurface;
import fori.graphics.RenderAPI;
import fori.graphics.RenderContext;

import fori.graphics.DebugUtil;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

public class VkRenderContext extends RenderContext {

    private long vkSurface;
    private VkInstance instance;
    private static final List<String> validationLayers = new ArrayList<>();
    static {
        validationLayers.add("VK_LAYER_KHRONOS_validation");
    }
    private long debugMessenger;



    public VkRenderContext() {

    }



    @Override
    public void readyDisplay(Surface surface) {
        if(!surface.supportsRenderAPI(RenderAPI.Vulkan)) {
            throw new RuntimeException(Logger.error(VkRenderContext.class, "The surface does not support Vulkan"));
        }

        instance = createInstance("Fori", validationLayers, surface);
        vkSurface = surface.getVulkanSurface(instance);
    }

    public long getDebugMessenger() {
        return debugMessenger;
    }

    private VkInstance createInstance(String appName, List<String> validationLayers, Surface surface){

        boolean validation = validationLayers != null;

        VkInstance instance;



        try (MemoryStack stack = stackPush()) {


            if(validation) {


                IntBuffer layerCount = stack.ints(0);

                vkEnumerateInstanceLayerProperties(layerCount, null);

                VkLayerProperties.Buffer availableLayers = VkLayerProperties.malloc(layerCount.get(0), stack);

                vkEnumerateInstanceLayerProperties(layerCount, availableLayers);

                Set<String> availableLayerNames = availableLayers.stream()
                        .map(VkLayerProperties::layerNameString)
                        .collect(toSet());

                for(String validationLayerName : validationLayers){
                    if(!availableLayerNames.contains(validationLayerName)){
                        throw new RuntimeException("Validation Layer " + validationLayerName + " is not available");
                    }
                }
            }

            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);

            appInfo.sType(VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8Safe(appName));
            appInfo.applicationVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8Safe(appName));
            appInfo.engineVersion(VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK_API_VERSION_1_3);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);




            PointerBuffer totalRequiredInstanceExtensions = null;
            PointerBuffer windowInstanceExtensions = surface.getVulkanInstanceExtensions();




            //System.exit(1);

            int instanceExtensionCount = windowInstanceExtensions.capacity();
            if(validation) instanceExtensionCount++;


            totalRequiredInstanceExtensions = stack.mallocPointer(instanceExtensionCount);


            totalRequiredInstanceExtensions.put(windowInstanceExtensions);
            if(validation) totalRequiredInstanceExtensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));

            DebugUtil.printPointerBuffer("totalRequiredInstanceExtensions", totalRequiredInstanceExtensions);

            createInfo.ppEnabledExtensionNames(totalRequiredInstanceExtensions.rewind());
            VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = null;

            if (validation) {
                createInfo.ppEnabledLayerNames(validationLayersAsPointerBuffer(validationLayers, stack));

                debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
                debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
                debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
                debugCreateInfo.pfnUserCallback((messageSeverity, messageTypes, pCallbackData, pUserData) -> {

                    VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                    Logger.info(VkRenderer.class, callbackData.pMessageString());

                    return VK_FALSE;
                });

                createInfo.pNext(debugCreateInfo.address());

            }

            PointerBuffer instancePtr = stack.mallocPointer(1);

            if (vkCreateInstance(createInfo, null, instancePtr) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create instance");
            }

            instance = new VkInstance(instancePtr.get(0), createInfo);


            if(validation) {

                LongBuffer pDebugMessenger = stack.longs(VK_NULL_HANDLE);

                int result = 0;

                if (vkGetInstanceProcAddr(instance, "vkCreateDebugUtilsMessengerEXT") != NULL) {
                    result = vkCreateDebugUtilsMessengerEXT(instance, debugCreateInfo, null, pDebugMessenger) == VK_SUCCESS ? VK_SUCCESS : VK_ERROR_EXTENSION_NOT_PRESENT;
                }

                if (result != VK_SUCCESS)
                    throw new RuntimeException("Failed to create the debug messenger as the extension is not present");

                debugMessenger = pDebugMessenger.get(0);


            }
        }

        return instance;
    }

    private static PointerBuffer validationLayersAsPointerBuffer(List<String> validationLayers, MemoryStack stack) {

        PointerBuffer buffer = stack.mallocPointer(validationLayers.size());

        validationLayers.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);

        return buffer.rewind();
    }


    public long getVkSurface() {
        return vkSurface;
    }
    public VkInstance getInstance() {
        return instance;
    }
}
