package fori.graphics;

public class RendererSettings {
    public boolean validation = true;
    public RenderAPI backend;
    public boolean vsync;

    public RendererSettings(RenderAPI backend) {
        this.backend = backend;
    }

    public RendererSettings validation(boolean validation){
        this.validation = validation;
        return this;
    }

    public RendererSettings vsync(boolean vsync){
        this.vsync = vsync;
        return this;
    }


}
