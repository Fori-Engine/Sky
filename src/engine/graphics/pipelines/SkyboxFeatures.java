package engine.graphics.pipelines;

import engine.graphics.Sampler;
import engine.graphics.Texture;

public class SkyboxFeatures extends Features {
    private Texture skyboxTexture;
    private Sampler skyboxSampler;
    private float sampleIntensity = 5;
    protected SkyboxFeatures(boolean mandatory) {
        super(mandatory);
    }

    public Texture getSkyboxTexture() {
        return skyboxTexture;
    }

    public void setSkyboxTexture(Texture skyboxTexture) {
        this.skyboxTexture = skyboxTexture;
    }

    public Sampler getSkyboxSampler() {
        return skyboxSampler;
    }

    public void setSkyboxSampler(Sampler skyboxSampler) {
        this.skyboxSampler = skyboxSampler;
    }

    public float getSampleIntensity() {
        return sampleIntensity;
    }

    public void setSampleIntensity(float sampleIntensity) {
        this.sampleIntensity = sampleIntensity;
    }
}
