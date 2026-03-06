package engine.ecs;

import engine.graphics.*;
import engine.graphics.pipelines.SceneFeatures;

import java.util.List;

public class RenderSystem extends EcsSystem {
    private Renderer renderer;
    private RenderPipeline renderPipeline;
    private Scene scene;

    public RenderSystem(Renderer renderer, RenderPipeline renderPipeline, Scene scene) {
        this.renderer = renderer;
        this.renderPipeline = renderPipeline;
        this.scene = scene;
        renderPipeline.init(renderer);
    }

    @Override
    public void run(List<Entity> entities) {

        for(Entity entity : entities) {
            if(entity.has(ShaderComponent.class)) {
                ShaderComponent shaderComponent = entity.getComponent(ShaderComponent.class);
                if (entity.has(MeshComponent.class))
                    setMaterialData(renderer.getFrameIndex(), shaderComponent.shaderProgram(), entity.getComponent(MaterialComponent.class));
                if (entity.has(MeshListComponent.class))
                    setMaterialData(renderer.getFrameIndex(), shaderComponent.shaderProgram(), entity.getComponent(MaterialComponent.class));
            }


        }


        renderPipeline.getFeatures(SceneFeatures.class).setScene(scene);
        renderPipeline.render(renderer);
    }

    private void setMaterialData(int frameIndex, ShaderProgram shaderProgram, MaterialComponent materialComponent) {
        Sampler sampler = materialComponent.material.getSampler();

        List<Texture> textures = materialComponent.material.getTextures();
        for (int i = 0; i < textures.size(); i++) {
            Texture texture = textures.get(i);

            shaderProgram.setTextures(frameIndex, new DescriptorUpdate<>("material", texture).arrayIndex(i));
            shaderProgram.setSamplers(frameIndex, new DescriptorUpdate<>("material_sampler", sampler));

        }


    }

    @Override
    public void dispose() {

    }
}
