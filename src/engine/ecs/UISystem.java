package engine.ecs;

import engine.asset.AssetRegistry;
import engine.graphics.*;
import engine.graphics.pipelines.ScreenSpaceFeatures;
import org.joml.Matrix2f;
import org.joml.Vector2f;
import java.nio.ByteBuffer;
import java.util.List;

public class UISystem extends EcsSystem {

    private Renderer renderer;
    private RenderPipeline renderPipeline;
    private Scene scene;
    private int quadIndex = 0;
    private ByteBuffer vertexBufferData, indexBufferData;
    private Matrix2f transform = new Matrix2f();
    private Vector2f origin = new Vector2f();

    private Texture texture;
    private Sampler sampler;

    public UISystem(Renderer renderer, RenderPipeline renderPipeline, Scene scene) {
        this.renderer = renderer;
        this.renderPipeline = renderPipeline;
        this.scene = scene;

        texture = Texture.newColorTextureFromAsset(renderer, AssetRegistry.getAsset("core:assets/fonts/B612/b612-atlas.png"), TextureFormatType.ColorR8G8B8A8);
        sampler = Sampler.newSampler(texture, Texture.Filter.Linear, Texture.Filter.Linear, true);






    }


    @Override
    public void run(List<Entity> entities) {
        ScreenSpaceFeatures screenSpaceFeatures = renderPipeline.getFeatures(ScreenSpaceFeatures.class);
        vertexBufferData = screenSpaceFeatures.getVertexBuffer().get();
        vertexBufferData.clear();
        indexBufferData = screenSpaceFeatures.getIndexBuffer().get();
        indexBufferData.clear();

        screenSpaceFeatures.getShaderProgram().setTextures(renderer.getFrameIndex(), new DescriptorUpdate<>("input_textures", texture).arrayIndex(1));
        screenSpaceFeatures.getShaderProgram().setSamplers(renderer.getFrameIndex(), new DescriptorUpdate<>("input_samplers", sampler).arrayIndex(1));


        transform = new Matrix2f();
        setOrigin(0, 0);

        drawQuad(
                0,
                0,
                1920,
                1080,
                0, 0,
                0, 1,
                1, 1,
                1, 0,
                0,
                0,
                Color.WHITE
        );

        drawQuad(
                0,
                0,
                300,
                300,
                0, 0,
                0, 1,
                1, 1,
                1, 0,
                0,
                1,
                Color.WHITE
        );






        screenSpaceFeatures.setIndexCount(6 * 1000);
        quadIndex = 0;
    }



    private void setOrigin(float x, float y) {
        origin.set(x, y);
    }

    private void drawQuad(float x,
                          float y,
                          float w,
                          float h,

                          float uvtlx,
                          float uvtly,

                          float uvblx,
                          float uvbly,

                          float uvtrx,
                          float uvtry,

                          float uvbrx,
                          float uvbry,

                          int fillMode,
                          int textureIndex,
                          Color color) {

        Vector2f
                topLeft = new Vector2f(x, y).sub(origin).mul(transform).add(origin),
                bottomLeft = new Vector2f(x, y + h).sub(origin).mul(transform).add(origin),
                bottomRight = new Vector2f(x + w, y + h).sub(origin).mul(transform).add(origin),
                topRight = new Vector2f(x + w, y).sub(origin).mul(transform).add(origin);

        vertexBufferData.putFloat(topLeft.x);
        vertexBufferData.putFloat(topLeft.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvtlx);
        vertexBufferData.putFloat(uvtly);
        vertexBufferData.putFloat(fillMode);
        vertexBufferData.putFloat(textureIndex);

        vertexBufferData.putFloat(bottomLeft.x);
        vertexBufferData.putFloat(bottomLeft.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvblx);
        vertexBufferData.putFloat(uvbly);
        vertexBufferData.putFloat(fillMode);
        vertexBufferData.putFloat(textureIndex);

        vertexBufferData.putFloat(bottomRight.x);
        vertexBufferData.putFloat(bottomRight.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvtrx);
        vertexBufferData.putFloat(uvtry);
        vertexBufferData.putFloat(fillMode);
        vertexBufferData.putFloat(textureIndex);

        vertexBufferData.putFloat(topRight.x);
        vertexBufferData.putFloat(topRight.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvbrx);
        vertexBufferData.putFloat(uvbry);
        vertexBufferData.putFloat(fillMode);
        vertexBufferData.putFloat(textureIndex);

        indexBufferData.putInt(0 + (4 * quadIndex));
        indexBufferData.putInt(1 + (4 * quadIndex));
        indexBufferData.putInt(2 + (4 * quadIndex));
        indexBufferData.putInt(2 + (4 * quadIndex));
        indexBufferData.putInt(3 + (4 * quadIndex));
        indexBufferData.putInt(0 + (4 * quadIndex));
        quadIndex++;
    }

    @Override
    public void dispose() {

    }
}
