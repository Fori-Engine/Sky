package lake.graphics.vulkan;

import lake.graphics.Context;
import lake.graphics.PlatformWindow;
import org.lwjgl.system.Platform;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.awt.AWTVKCanvas;
import org.lwjgl.vulkan.awt.PlatformVKCanvas;
import org.lwjgl.vulkan.awt.VKData;

import javax.swing.*;
import java.awt.*;

import static org.lwjgl.glfw.GLFW.*;

public class VulkanContext extends Context {

    private long platformWindowSurface, canvasSurface;
    private VkInstance platformWindowInstance, canvasInstance;

    @Override
    public void enableHints() {
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    }

    @Override
    public void setup(PlatformWindow window) {

    }

    @Override
    public void swapBuffers(PlatformWindow window) {

    }

    @Override
    public void readyDisplay(PlatformWindow window) {
        platformWindowInstance = VulkanUtil.createInstance(getClass().getName(), "Fori", true, SurfaceType.PlatformWindow);
        platformWindowSurface = VulkanUtil.createSurface(platformWindowInstance, window);
    }

    @Override
    public void readyCanvas(Canvas canvas) {
        canvasInstance = VulkanUtil.createInstance(getClass().getName(), "Fori", true, SurfaceType.Canvas);

        PlatformVKCanvas platformCanvas;
        String platformClassName;
        switch (Platform.get()) {
            case WINDOWS:
                platformClassName = "org.lwjgl.vulkan.awt.PlatformWin32VKCanvas";
                break;
            case LINUX:
                platformClassName = "org.lwjgl.vulkan.awt.PlatformX11VKCanvas";
                break;
            case MACOSX:
                platformClassName = "org.lwjgl.vulkan.awt.PlatformMacOSXVKCanvas";
                break;
            default:
                throw new AssertionError("NYI");
        }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends PlatformVKCanvas> clazz = (Class<? extends PlatformVKCanvas>) AWTVKCanvas.class.getClassLoader().loadClass(platformClassName);
            platformCanvas = clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Platform-specific VKCanvas class not found: " + platformClassName);
        } catch (InstantiationException e) {
            throw new AssertionError("Could not instantiate platform-specific VKCanvas class: " + platformClassName);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Could not instantiate platform-specific VKCanvas class: " + platformClassName);
        }


        VKData data = new VKData();
        data.instance = canvasInstance;


        if (canvasSurface == 0L) {
            try {
                canvasSurface = platformCanvas.create(canvas, data);
            } catch (AWTException e) {
                throw new RuntimeException("Exception while creating the Vulkan surface", e);
            }
        }
    }

    public long getPlatformWindowSurface() {
        return platformWindowSurface;
    }

    public long getCanvasSurface() {
        return canvasSurface;
    }

    public VkInstance getPlatformWindowInstance() {
        return platformWindowInstance;
    }

    public VkInstance getCanvasInstance() {
        return canvasInstance;
    }
}
