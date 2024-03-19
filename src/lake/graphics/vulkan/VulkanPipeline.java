package lake.graphics.vulkan;

public class VulkanPipeline {
    public long pipelineLayout;
    public long pipeline;

    public VulkanPipeline(long pipelineLayout, long pipeline) {
        this.pipelineLayout = pipelineLayout;
        this.pipeline = pipeline;
    }
}
