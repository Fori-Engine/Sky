package engine.ecs;

import engine.Time;
import engine.graphics.Color;
import engine.graphics.RenderPipeline;
import engine.graphics.Renderer;
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

    public UISystem(Renderer renderer, RenderPipeline renderPipeline, Scene scene) {
        this.renderer = renderer;
        this.renderPipeline = renderPipeline;
        this.scene = scene;
    }

    private float elapsedTime = 0;
    @Override
    public void run(List<Entity> entities) {
        ScreenSpaceFeatures screenSpaceFeatures = renderPipeline.getFeatures(ScreenSpaceFeatures.class);
        vertexBufferData = screenSpaceFeatures.getVertexBuffer().get();
        vertexBufferData.clear();
        indexBufferData = screenSpaceFeatures.getIndexBuffer().get();
        indexBufferData.clear();


        transform = new Matrix2f();
        drawQuad(0, 0, 1920, 1080, 0, Color.WHITE);


        transform = new Matrix2f()
                .rotate(elapsedTime * 1);

        elapsedTime += 2 * Time.deltaTime;
        setOrigin(100 + 50, 600 + 50);
        drawQuad(100, 600, (float) (100 * Math.sin(elapsedTime)), (float) (100 * Math.sin(elapsedTime)), -1, Color.RED);
        setOrigin(0, 0);

        transform = new Matrix2f();
        drawQuad((renderer.getWidth() / 2) - (30 / 2), (renderer.getHeight() / 2) - (30 / 2), 30, 30, -2, Color.WHITE);


        screenSpaceFeatures.setIndexCount(18);
        quadIndex = 0;
    }

    private void setOrigin(float x, float y) {
        origin.set(x, y);
    }

    private void drawQuad(float x, float y, float w, float h, int fillMode, Color color) {

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
        vertexBufferData.putFloat(0);
        vertexBufferData.putFloat(0);
        vertexBufferData.putFloat(fillMode);

        vertexBufferData.putFloat(bottomLeft.x);
        vertexBufferData.putFloat(bottomLeft.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(0);
        vertexBufferData.putFloat(1);
        vertexBufferData.putFloat(fillMode);


        vertexBufferData.putFloat(bottomRight.x);
        vertexBufferData.putFloat(bottomRight.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(1);
        vertexBufferData.putFloat(1);
        vertexBufferData.putFloat(fillMode);


        vertexBufferData.putFloat(topRight.x);
        vertexBufferData.putFloat(topRight.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(1);
        vertexBufferData.putFloat(0);
        vertexBufferData.putFloat(fillMode);


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
