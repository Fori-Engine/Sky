package lake.graphics;

public class RenderSettings {
    public boolean enableValidation = true;
    public int quadsPerBatch = 20000;
    public boolean msaa = false;
    public RendererBackend backend;

    public RenderSettings(RendererBackend backend) {
        this.backend = backend;
    }

    public RenderSettings enableValidation(boolean enableValidation){
        this.enableValidation = enableValidation;
        return this;
    }

    public RenderSettings msaa(boolean msaa){
        this.msaa = msaa;
        return this;
    }

    public RenderSettings backend(RendererBackend backend){
        this.backend = backend;
        return this;
    }
    public RenderSettings batchSize(int batchSize){
        this.quadsPerBatch = batchSize;
        return this;
    }




}
