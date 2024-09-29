package fori.graphics.vulkan;

public class VkPipeline {
    public long pipelineLayout;
    public long pipeline;

    public VkPipeline(long pipelineLayout, long pipeline) {
        this.pipelineLayout = pipelineLayout;
        this.pipeline = pipeline;
    }
}
