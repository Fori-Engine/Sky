package engine.graphics;

import engine.Logger;
import engine.SkyRuntimeException;
import engine.ecs.Scene;
import engine.graphics.pipelines.Features;

import java.util.List;

public abstract class RenderPipeline {
    protected RenderGraph renderGraph;
    protected List<Features> supportedFeatures;
    public abstract void init(Renderer renderer);
    public abstract void render(Renderer renderer);

    public <T> T getFeatures(Class<T> c) {

        for(Features features : supportedFeatures){
            if(features.getClass() == c || c.isAssignableFrom(features.getClass())) {
                return c.cast(features);
            }
        }

        throw new SkyRuntimeException("This pipeline does not support the feature set " + c.getName());
    }

}
