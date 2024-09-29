package fori.graphics.vulkan;

import org.lwjgl.vulkan.VkExtent2D;

import java.util.List;

public class VkSwapchain {
    public long swapChain;
    public List<Long> swapChainImages;
    public int swapChainImageFormat;
    public VkExtent2D swapChainExtent;

    public VkSwapchain(long swapChain, List<Long> swapChainImages, int swapChainImageFormat, VkExtent2D swapChainExtent) {
        this.swapChain = swapChain;
        this.swapChainImages = swapChainImages;
        this.swapChainImageFormat = swapChainImageFormat;
        this.swapChainExtent = swapChainExtent;
    }

    public VkSwapchain(){

    }
}
