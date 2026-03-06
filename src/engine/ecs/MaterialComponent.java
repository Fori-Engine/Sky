package engine.ecs;

import engine.graphics.Material;

@ComponentArray(mask = 1 << 8)
public class MaterialComponent {
    public Material material;

    public MaterialComponent(Material material) {
        this.material = material;
    }
}
