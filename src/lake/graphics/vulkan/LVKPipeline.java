package lake.graphics.vulkan;

public class LVKPipeline {
    public long pipelineLayout;
    public long pipeline;

    public LVKPipeline(long pipelineLayout, long pipeline) {
        this.pipelineLayout = pipelineLayout;
        this.pipeline = pipeline;
    }
}
