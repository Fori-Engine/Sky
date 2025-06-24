package fori;
import static java.util.stream.Collectors.toSet;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK13.VK_API_VERSION_1_3;

import fori.graphics.Ref;
import fori.graphics.RenderAPI;
import fori.graphics.vulkan.VulkanRenderer;
import org.joml.Vector2f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.List;
import java.util.Set;


public class GLFWSurface extends Surface {

    private long handle;
    private Cursor cursor;
    private GLFWErrorCallback errorCallback;

    @Override
    public void requestRenderAPI(RenderAPI api) {
        if(api == RenderAPI.Vulkan) {
            try (MemoryStack stack = stackPush()) {
                vkInstance = createInstance(title, List.of("VK_LAYER_KHRONOS_validation"));
                LongBuffer pSurface = stack.mallocLong(1);
                glfwCreateWindowSurface(vkInstance, handle,
                        null, pSurface);
                vkSurface = pSurface.get(0);
            }
        }
    }

    public GLFWSurface(Ref parent, String title, int width, int height, boolean resizable) {
        super(parent, title, width, height, resizable);


        if (!glfwInit()) {
            throw new RuntimeException(Logger.error(GLFWSurface.class, "Failed to initialize GLFW"));
        }

        errorCallback = GLFWErrorCallback.createPrint(System.err);
        errorCallback.set();

        glfwWindowHint(GLFW_RESIZABLE, glfwBool(resizable));
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        handle = glfwCreateWindow(width, height, title, NULL, NULL);
    }

    @Override
    public PointerBuffer getVulkanInstanceExtensions() {
        return glfwGetRequiredInstanceExtensions();
    }

    @Override
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
                vkDebugUtilsMessengerCallbackEXT = new VkDebugUtilsMessengerCallbackEXT() {
                    @Override
                    public int invoke(int messageSeverity, int messageTypes, long pCallbackData, long pUserData) {
                        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                        Logger.info(VulkanRenderer.class, callbackData.pMessageString());

                        return VK_FALSE;
                    }
                };



                debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
                debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
                debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
                debugCreateInfo.pfnUserCallback(vkDebugUtilsMessengerCallbackEXT);

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
    @Override
    public boolean supportsRenderAPI(RenderAPI api) {
        if(api == RenderAPI.Vulkan) return glfwVulkanSupported();
        return false;
    }

    @Override
    public double getTime() {
        return glfwGetTime();
    }


    private int glfwBool(boolean b){
        return b ? GLFW_TRUE : GLFW_FALSE;
    }

    @Override
    public void display() {
        if (handle == NULL)
            throw new RuntimeException(Logger.error(GLFWSurface.class, "Failed to create GLFW window"));
        glfwShowWindow(handle);

    }

    @Override
    public boolean update() {

        double[] x = new double[1], y = new double[1];
        glfwGetCursorPos(handle, x, y);

        cursorPos.set(x[0], y[0]);
        glfwPollEvents();

        int newWidth = getWidth();
        int newHeight = getHeight();

        while(newWidth == 0 || newHeight == 0) {
            newWidth = getWidth();
            newHeight = getHeight();

            glfwPollEvents();
        }


        if(newWidth != width || newHeight != height) {
            width = newWidth;
            height = newHeight;

            return true;
        }


        return false;
    }

    @Override
    public int getWidth() {
        int[] width = new int[1], height = new int[1];
        glfwGetWindowSize(handle, width, height);
        return width[0];
    }

    @Override
    public int getHeight() {
        int[] width = new int[1], height = new int[1];
        glfwGetWindowSize(handle, width, height);
        return height[0];
    }

    @Override
    public boolean getKeyPressed(int key) {
        return glfwGetKey(handle, key) == GLFW_PRESS;
    }

    @Override
    public boolean getKeyReleased(int key) {
        return glfwGetKey(handle, key) == GLFW_RELEASE;
    }

    @Override
    public boolean getMousePressed(int button) {
        return glfwGetMouseButton(handle, button) == GLFW_PRESS;
    }

    @Override
    public boolean getMouseReleased(int button) {
        return glfwGetMouseButton(handle, button) == GLFW_RELEASE;
    }

    @Override
    public Vector2f getMousePos() {
        return cursorPos;
    }

    @Override
    public void setCursor(Cursor cursor) {
        if(this.cursor != cursor) {
            switch (cursor) {
                case Default -> glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_ARROW_CURSOR));
                case ResizeWE -> glfwSetCursor(handle, glfwCreateStandardCursor(GLFW_RESIZE_EW_CURSOR));
            }
            this.cursor = cursor;
        }
    }

    @Override
    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    @Override
    public void dispose() {
        if(vkDebugUtilsMessengerCallbackEXT != null) vkDebugUtilsMessengerCallbackEXT.free();
        errorCallback.free();
        glfwDestroyWindow(handle);
        glfwTerminate();
    }


}
