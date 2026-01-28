package engine;

import engine.graphics.Disposable;
import engine.graphics.RenderAPI;
import engine.graphics.vulkan.VulkanRenderer;
import org.joml.Vector2f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;
import org.lwjgl.vulkan.awt.AWTVK;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.lang.reflect.InvocationTargetException;
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

public class SwingSurface extends Surface {

    private JFrame frame;
    private Canvas canvas;
    private volatile boolean shouldClose;
    private long startTime;
    private volatile int width, height;
    private volatile float mouseX, mouseY;
    private volatile float scaleX, scaleY;
    private volatile boolean shouldResize;

    public SwingSurface(Disposable parent, String title, int width, int height, boolean resizable) {
        super(parent, title, width, height, resizable);
        Logger.info(SwingSurface.class, "SwingSurface is experimental and may have performance issues");
        startTime = System.currentTimeMillis();
        this.width = width;
        this.height = height;

        try {
            SwingUtilities.invokeAndWait((Runnable) () -> {
                frame = new JFrame(title);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLayout(new BorderLayout());
                frame.setResizable(resizable);

                canvas = new Canvas();

                GraphicsConfiguration gc =
                        GraphicsEnvironment.getLocalGraphicsEnvironment()
                                .getDefaultScreenDevice()
                                .getDefaultConfiguration();

                AffineTransform transform = gc.getDefaultTransform();

                scaleX = (float) transform.getScaleX();
                scaleY = (float) transform.getScaleY();

                canvas.setPreferredSize(new Dimension((int) (this.width / scaleX), (int) (this.height / scaleY)));
                frame.setSize((int) (this.width / scaleX), (int) (this.height / scaleY));

                canvas.addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseMoved(MouseEvent e) {
                        Point mousePosScr = MouseInfo.getPointerInfo().getLocation();
                        Point canvasPosScr = canvas.getLocationOnScreen();

                        mouseX = mousePosScr.x - canvasPosScr.x;
                        mouseY = mousePosScr.y - canvasPosScr.y;

                    }
                });



                JPanel panel = new JPanel(new BorderLayout());
                panel.add(canvas, BorderLayout.CENTER);
                panel.add(new JButton("Foo"), BorderLayout.NORTH);











                frame.setContentPane(panel);
                frame.pack();

                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        shouldClose = true;
                    }
                });

                canvas.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        if(SwingSurface.this.width != canvas.getWidth() || SwingSurface.this.height != canvas.getHeight()) {
                            SwingSurface.this.width = canvas.getWidth();
                            SwingSurface.this.height = canvas.getHeight();

                            shouldResize = true;
                        }
                    }
                });






            });
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public void requestRenderAPI(RenderAPI api) {
        if(api == RenderAPI.Vulkan) {
            vkInstance = createInstance(title, List.of("VK_LAYER_KHRONOS_validation"));
            try {
                SwingUtilities.invokeAndWait((Runnable) () -> {
                    try {
                        vkSurface = AWTVK.create(canvas, vkInstance);
                    } catch (AWTException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }


        }
    }

    @Override
    public boolean getKeyPressed(int key) {
        return false;
    }

    @Override
    public boolean getKeyReleased(int key) {
        return false;
    }

    @Override
    public boolean getMousePressed(int button) {
        return false;
    }

    @Override
    public boolean getMouseReleased(int button) {
        return false;
    }

    @Override
    public Vector2f getMousePos() {
        System.out.println(mouseX + " " + mouseY);
        return new Vector2f(mouseX, mouseY);
    }

    @Override
    public void setCursor(Cursor cursor) {

    }

    @Override
    public PointerBuffer getVulkanInstanceExtensions() {
        GLFW.glfwInit();
        return GLFWVulkan.glfwGetRequiredInstanceExtensions();
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
                    else {
                        Logger.info(VulkanRenderer.class, validationLayerName);
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

            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.calloc(stack);

            instanceCreateInfo.sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            instanceCreateInfo.pApplicationInfo(appInfo);




            PointerBuffer pInstanceExtensions;
            PointerBuffer pWindowingExtensions = getVulkanInstanceExtensions();


            int instanceExtensionCount = pWindowingExtensions.capacity();
            if(validation) instanceExtensionCount++;


            pInstanceExtensions = stack.mallocPointer(instanceExtensionCount);
            pInstanceExtensions.put(pWindowingExtensions);


            if(validation)
                pInstanceExtensions.put(stack.UTF8(VK_EXT_DEBUG_UTILS_EXTENSION_NAME));


            instanceCreateInfo.ppEnabledExtensionNames(pInstanceExtensions.rewind());




            VkDebugUtilsMessengerCreateInfoEXT debugCreateInfo = null;




            if (validation) {

                PointerBuffer pEnabledLayerNames = stack.mallocPointer(validationLayers.size());

                for(String validationLayerName : validationLayers){
                    System.out.println(validationLayerName);
                    pEnabledLayerNames.put(stack.UTF8(validationLayerName));
                }

                instanceCreateInfo.ppEnabledLayerNames(pEnabledLayerNames.rewind());

                vkDebugUtilsMessengerCallbackEXT = new VkDebugUtilsMessengerCallbackEXT() {
                    @Override
                    public int invoke(int messageSeverity, int messageTypes, long pCallbackData, long pUserData) {
                        VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);
                        Logger.info(VulkanRenderer.class, callbackData.pMessageString());
                        System.out.println();

                        return VK_FALSE;
                    }
                };



                debugCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack);
                debugCreateInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT);
                debugCreateInfo.messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT);
                debugCreateInfo.messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT);
                debugCreateInfo.pfnUserCallback(vkDebugUtilsMessengerCallbackEXT);

                instanceCreateInfo.pNext(debugCreateInfo.address());
            }





            PointerBuffer instancePtr = stack.mallocPointer(1);

            if (vkCreateInstance(instanceCreateInfo, null, instancePtr) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create instance");
            }

            instance = new VkInstance(instancePtr.get(0), instanceCreateInfo);



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
    @Override
    public boolean supportsRenderAPI(RenderAPI api) {
        if(api == RenderAPI.Vulkan) return true;

        return false;
    }

    @Override
    public double getTime() {
        return (System.currentTimeMillis() - startTime) / 1000f;
    }

    @Override
    public void display() {
        try {
            SwingUtilities.invokeAndWait((Runnable) () -> frame.setVisible(true));
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean update() {
        if(shouldResize) {
            shouldResize = false;
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldClose() {
        return shouldClose;
    }

    @Override
    public void dispose() {
        if(vkDebugUtilsMessengerCallbackEXT != null) vkDebugUtilsMessengerCallbackEXT.free();
    }
}
