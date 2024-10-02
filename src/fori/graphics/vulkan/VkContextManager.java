package fori.graphics.vulkan;

import org.lwjgl.vulkan.*;

public class VkContextManager {

    private static VkDevice currentDevice;
    private static VkPhysicalDevice currentPhysicalDevice;
    private static int graphicsFamilyIndex;
    private static VkQueue graphicsQueue;
    private static VkPhysicalDeviceProperties physicalDeviceProperties;

    private VkContextManager(){}

    public static VkDevice getCurrentDevice() {
        return currentDevice;
    }

    public static void setCurrentDevice(VkDevice currentDevice) {
        VkContextManager.currentDevice = currentDevice;
    }

    public static VkPhysicalDevice getCurrentPhysicalDevice() {
        return currentPhysicalDevice;
    }

    public static void setCurrentPhysicalDevice(VkPhysicalDevice currentPhysicalDevice) {
        VkContextManager.currentPhysicalDevice = currentPhysicalDevice;
    }

    public static int getGraphicsFamilyIndex() {
        return graphicsFamilyIndex;
    }

    public static void setGraphicsFamilyIndex(int graphicsFamilyIndex) {
        VkContextManager.graphicsFamilyIndex = graphicsFamilyIndex;
    }

    public static VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public static void setGraphicsQueue(VkQueue graphicsQueue) {
        VkContextManager.graphicsQueue = graphicsQueue;
    }

    public static VkPhysicalDeviceProperties getPhysicalDeviceProperties() {
        return physicalDeviceProperties;
    }

    public static void setPhysicalDeviceProperties(VkPhysicalDeviceProperties physicalDeviceProperties) {
        VkContextManager.physicalDeviceProperties = physicalDeviceProperties;
    }
}
