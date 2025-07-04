package fori.graphics.vulkan;

import org.lwjgl.vulkan.VkExtent2D;

import java.util.ArrayList;
import java.util.List;

public class VulkanSwapchain {
    public long swapchain;
    public List<Long> images = new ArrayList<>();
    public int imageFormat;
    public VkExtent2D extent;

    public VulkanSwapchain(){

    }
}
