package fori.graphics.vulkan;

import org.lwjgl.vulkan.*;

public class VulkanDeviceManager {

    private static VkDevice currentDevice;
    private static VkPhysicalDevice currentPhysicalDevice;
    private static int graphicsFamilyIndex;
    private static VkQueue graphicsQueue;
    private static VkPhysicalDeviceProperties physicalDeviceProperties;

    private VulkanDeviceManager(){}

    public static VkDevice getCurrentDevice() {
        return currentDevice;
    }

    public static void setCurrentDevice(VkDevice currentDevice) {
        VulkanDeviceManager.currentDevice = currentDevice;
    }

    public static VkPhysicalDevice getCurrentPhysicalDevice() {
        return currentPhysicalDevice;
    }

    public static void setCurrentPhysicalDevice(VkPhysicalDevice currentPhysicalDevice) {
        VulkanDeviceManager.currentPhysicalDevice = currentPhysicalDevice;
    }

    public static int getGraphicsFamilyIndex() {
        return graphicsFamilyIndex;
    }

    public static void setGraphicsFamilyIndex(int graphicsFamilyIndex) {
        VulkanDeviceManager.graphicsFamilyIndex = graphicsFamilyIndex;
    }

    public static VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public static void setGraphicsQueue(VkQueue graphicsQueue) {
        VulkanDeviceManager.graphicsQueue = graphicsQueue;
    }

    public static VkPhysicalDeviceProperties getPhysicalDeviceProperties() {
        return physicalDeviceProperties;
    }

    public static void setPhysicalDeviceProperties(VkPhysicalDeviceProperties physicalDeviceProperties) {
        VulkanDeviceManager.physicalDeviceProperties = physicalDeviceProperties;
    }
}
