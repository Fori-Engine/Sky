package fori;

import fori.graphics.DebugUtil;
import fori.graphics.Disposable;
import fori.graphics.Ref;
import fori.graphics.RenderAPI;
import fori.graphics.vulkan.VulkanRenderer;
import org.joml.Vector2f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

public abstract class Surface implements Disposable {
    protected String title;
    protected int width, height;
    protected boolean resizable;
    protected Ref ref;
    protected Vector2f cursorPos = new Vector2f();
    protected long vkDebugMessenger;
    protected long vkSurface;
    protected VkInstance vkInstance;

    public abstract void requestRenderAPI(RenderAPI api);


    public enum Cursor {
        Default,
        ResizeWE
    }

    public Surface(Ref parent, String title, int width, int height, boolean resizable) {
        ref = parent.add(this);
        this.title = title;
        this.width = width;
        this.height = height;
        this.resizable = resizable;
    }

    public String getTitle() {
        return title;
    }

    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }


    public abstract boolean getKeyPressed(int key);
    public abstract boolean getKeyReleased(int key);
    public abstract boolean getMousePressed(int button);
    public abstract boolean getMouseReleased(int button);
    public abstract Vector2f getMousePos();
    public abstract void setCursor(Cursor cursor);


    public static Surface newSurface(Ref parent, String title, int width, int height) {
        return newSurface(parent, title, width, height, true);
    }
    public static Surface newSurface(Ref parent, String title, int width, int height, boolean resizable) {
        return new GLFWSurface(parent, title, width, height, resizable);
    }




    public boolean isResizable() {
        return resizable;
    }
    public abstract PointerBuffer getVulkanInstanceExtensions();
    public long getVulkanSurface() {
        return vkSurface;
    }
    public VkInstance getVulkanInstance() {
        return vkInstance;
    }

    protected VkInstance createInstance(String appName, List<String> validationLayers){

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
            PointerBuffer windowInstanceExtensions = getVulkanInstanceExtensions();




            //System.exit(1);

            int instanceExtensionCount = windowInstanceExtensions.capacity();
            if(validation) instanceExtensionCount++;


            totalRequiredInstanceExtensions = stack.mallocPointer(instanceExtensionCount);


            totalRequiredInstanceExtensions.put(windowInstanceExtensions);
            if(validation) totalRequiredInstanceExtensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));


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
                    Logger.info(VulkanRenderer.class, callbackData.pMessageString());

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

                vkDebugMessenger = pDebugMessenger.get(0);


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

    public long getVulkanDebugMessenger() {
        return vkDebugMessenger;
    }

    public abstract boolean supportsRenderAPI(RenderAPI api);
    public abstract void display();
    public abstract boolean update();
    public abstract boolean shouldClose();


    @Override
    public Ref getRef() {
        return ref;
    }



}



