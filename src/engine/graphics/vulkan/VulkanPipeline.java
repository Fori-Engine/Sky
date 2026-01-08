package engine.graphics.vulkan;

import engine.graphics.Disposable;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanPipeline extends Disposable {
    private long pipelineLayout;
    private long pipeline;

    public VulkanPipeline(Disposable parent, long pipelineLayout, long pipeline) {
        super(parent);
        this.pipelineLayout = pipelineLayout;
        this.pipeline = pipeline;
    }

    public long getLayoutHandle() {
        return pipelineLayout;
    }

    public long getHandle() {
        return pipeline;
    }

    @Override
    public void dispose() {
        vkDeviceWaitIdle(VulkanRuntime.getCurrentDevice());
        vkDestroyPipeline(VulkanRuntime.getCurrentDevice(), pipeline, null);
        vkDestroyPipelineLayout(VulkanRuntime.getCurrentDevice(), pipelineLayout, null);
    }
}
