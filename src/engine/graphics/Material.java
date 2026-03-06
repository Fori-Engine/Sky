package engine.graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Material {
    private List<Texture> textures = new ArrayList<>();
    private Sampler sampler;

    public Material(Sampler sampler, Texture... textures) {
        this.sampler = sampler;
        this.textures.addAll(Arrays.asList(textures));
    }

    public List<Texture> getTextures() {
        return textures;
    }

    public Sampler getSampler() {
        return sampler;
    }
}
