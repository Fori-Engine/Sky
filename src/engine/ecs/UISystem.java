package engine.ecs;

import engine.asset.AssetRegistry;
import engine.graphics.*;
import engine.graphics.pipelines.ScreenSpaceFeatures;
import engine.graphics.text.MsdfFont;
import engine.graphics.text.MsdfJsonLoader;
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


    private MsdfFont msdfFont;

    public UISystem(Renderer renderer, RenderPipeline renderPipeline, Scene scene) {
        this.renderer = renderer;
        this.renderPipeline = renderPipeline;
        this.scene = scene;

        msdfFont = new MsdfFont(
                renderer,
                AssetRegistry.getAsset("core:assets/fonts/Roboto/roboto-atlas.png"),
                AssetRegistry.getAsset("core:assets/fonts/Roboto/roboto-atlas.json")
        );








    }


    @Override
    public void run(List<Entity> entities) {
        ScreenSpaceFeatures screenSpaceFeatures = renderPipeline.getFeatures(ScreenSpaceFeatures.class);
        vertexBufferData = screenSpaceFeatures.getVertexBuffer().get();
        vertexBufferData.clear();
        indexBufferData = screenSpaceFeatures.getIndexBuffer().get();
        indexBufferData.clear();

        screenSpaceFeatures.getShaderProgram().setTextures(renderer.getFrameIndex(), new DescriptorUpdate<>("input_textures", msdfFont.getTexture()).arrayIndex(1));
        screenSpaceFeatures.getShaderProgram().setSamplers(renderer.getFrameIndex(), new DescriptorUpdate<>("input_samplers", msdfFont.getSampler()).arrayIndex(1));


        transform = new Matrix2f();
        setOrigin(0, 0);

        drawQuad(
                0,
                0,
                1920,
                1080,
                0, 0,
                0, 1,
                1, 0,
                1, 1,
                0,
                0,
                -1,
                Color.WHITE
        );

        drawString(300, 300, " private void drawString(float x, float y, String text, Color color) {\n" +
                "\n" +
                "        float xl = 0;\n" +
                "        float yl = y;\n" +
                "\n" +
                "        float spaceXAdvance = msdfData.characters[' '].advance;\n" +
                "\n" +
                "        for (int i = 0; i < text.length(); i++) {\n" +
                "            char c = text.charAt(i);\n" +
                "\n" +
                "            if(c == '\\n') {\n" +
                "                yl += msdfData.lineHeight * msdfData.size;\n" +
                "                xl = 0;\n" +
                "                continue;\n" +
                "            }\n" +
                "            if(c == '\\t') {\n" +
                "                xl = 4 * spaceXAdvance;\n" +
                "                continue;\n" +
                "            }\n" +
                "\n" +
                "            MsdfJsonLoader.Character character = msdfData.characters[c];\n" +
                "            if(character == null) character = msdfData.characters['?'];\n" +
                "\n" +
                "            MsdfJsonLoader.Rect planeBounds = character.planeBounds;\n" +
                "            if(planeBounds != null) {\n" +
                "\n" +
                "                float sw = (planeBounds.right - planeBounds.left) * msdfData.size;\n" +
                "                float sh = (planeBounds.top - planeBounds.bottom) * msdfData.size;\n" +
                "                float yo = planeBounds.bottom * msdfData.size;\n" +
                "\n" +
                "                drawGlyph(x + xl, yl - sh - yo, sw, sh, 1, msdfData, character, color);\n" +
                "            }\n" +
                "            xl += character.advance * msdfData.size;\n" +
                "\n" +
                "\n" +
                "        }\n" +
                "\n" +
                "\n" +
                "    }\n", msdfFont, Color.WHITE);



        screenSpaceFeatures.setIndexCount(6 * 1000);
        quadIndex = 0;
    }

    private void drawString(float x, float y, String text, MsdfFont msdfFont, Color color) {

        float xl = 0;
        float yl = y;

        float spaceXAdvance = msdfFont.getMSDFData().characters[' '].advance;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if(c == '\n') {
                yl += msdfFont.getMSDFData().lineHeight * msdfFont.getMSDFData().size;
                xl = 0;
                continue;
            }
            if(c == '\t') {
                xl = 4 * spaceXAdvance;
                continue;
            }

            MsdfJsonLoader.Character character = msdfFont.getMSDFData().characters[c];
            if(character == null) character = msdfFont.getMSDFData().characters['?'];

            MsdfJsonLoader.Rect planeBounds = character.planeBounds;
            if(planeBounds != null) {

                float sw = (planeBounds.right - planeBounds.left) * msdfFont.getMSDFData().size;
                float sh = (planeBounds.top - planeBounds.bottom) * msdfFont.getMSDFData().size;
                float yo = planeBounds.bottom * msdfFont.getMSDFData().size;

                drawGlyph(x + xl, yl - sh - yo, sw, sh, 1, msdfFont.getMSDFData(), character, color);
            }
            xl += character.advance * msdfFont.getMSDFData().size;


        }


    }


    private void setOrigin(float x, float y) {
        origin.set(x, y);
    }

    private void drawGlyph(float x, float y, float w, float h, int msdfTextureIndex, MsdfJsonLoader.MsdfData msdfData, MsdfJsonLoader.Character character, Color color) {

        float msdfScreenPxRange = (w / msdfData.width) * msdfData.size;

        drawQuad(
                x,
                y,
                w,
                h,
                character.atlasBounds.left / msdfData.width,
                1 - character.atlasBounds.top / msdfData.height,
                character.atlasBounds.left / msdfData.width,
                1 - character.atlasBounds.bottom / msdfData.height,
                character.atlasBounds.right / msdfData.width,
                1 - character.atlasBounds.top / msdfData.height,
                character.atlasBounds.right / msdfData.width,
                1 - character.atlasBounds.bottom / msdfData.height,
                -3,
                msdfTextureIndex,
                msdfScreenPxRange,
                color
        );
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
                          float msdfScreenPxRange,
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
        vertexBufferData.putFloat(msdfScreenPxRange);

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
        vertexBufferData.putFloat(msdfScreenPxRange);

        vertexBufferData.putFloat(bottomRight.x);
        vertexBufferData.putFloat(bottomRight.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvbrx);
        vertexBufferData.putFloat(uvbry);
        vertexBufferData.putFloat(fillMode);
        vertexBufferData.putFloat(textureIndex);
        vertexBufferData.putFloat(msdfScreenPxRange);

        vertexBufferData.putFloat(topRight.x);
        vertexBufferData.putFloat(topRight.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvtrx);
        vertexBufferData.putFloat(uvtry);
        vertexBufferData.putFloat(fillMode);
        vertexBufferData.putFloat(textureIndex);
        vertexBufferData.putFloat(msdfScreenPxRange);

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
