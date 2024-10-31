package fori.graphics;

public class Material {
    private Texture albedo, metallic, normal, roughness;
    private String name;
    public static final int SIZE = 4, MAX_MATERIALS = 3;


    public Material(String name, Texture albedo, Texture metallic, Texture normal, Texture roughness) {
        this.name = name;
        this.albedo = albedo;
        this.metallic = metallic;
        this.normal = normal;
        this.roughness = roughness;
    }

    public String getName() {
        return name;
    }

    public Texture getAlbedo() {
        return albedo;
    }

    public Texture getMetallic() {
        return metallic;
    }

    public Texture getNormal() {
        return normal;
    }

    public Texture getRoughness() {
        return roughness;
    }
}
