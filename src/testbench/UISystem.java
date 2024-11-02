package testbench;

import fori.Scene;
import fori.asset.AssetPacks;
import fori.ecs.EntitySystem;
import fori.ecs.MessageQueue;
import fori.graphics.*;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.Map;

import static fori.graphics.Attributes.Type.*;
import static fori.graphics.Attributes.Type.MaterialBaseIndexFloat1;
import static fori.graphics.ShaderRes.ShaderStage.FragmentStage;
import static fori.graphics.ShaderRes.ShaderStage.VertexStage;
import static fori.graphics.ShaderRes.Type.*;

public class UISystem extends EntitySystem {

    private Renderer renderer;
    private ShaderProgram shaderProgram;
    private Texture texture;
    private Camera camera;
    private Font font;

    public UISystem(Renderer renderer) {
        this.renderer = renderer;
        camera = new Camera(
                new Matrix4f().identity(),
                new Matrix4f().ortho(0, renderer.getWidth(), 0, renderer.getHeight(), 0, 1, true),
                false
        );

        //Shader Setup
        {
            ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                    AssetPacks.<String> getAsset("core:assets/shaders/vulkan/UI.glsl").asset
            );

            shaderProgram = ShaderProgram.newShaderProgram(renderer.getRef(), shaderSources.vertexShader, shaderSources.fragmentShader);
            shaderProgram.bind(
                    new Attributes.Type[]{
                            PositionFloat3,
                            ColorFloat4,
                            TransformIndexFloat1,
                            UVFloat2,
                            MaterialBaseIndexFloat1

                    },
                    new ShaderResSet(
                            0,
                            new ShaderRes(
                                    "camera",
                                    0,
                                    UniformBuffer,
                                    VertexStage
                            ).sizeBytes(2 * SizeUtil.MATRIX_SIZE_BYTES),
                            new ShaderRes(
                                    "transforms",
                                    1,
                                    ShaderStorageBuffer,
                                    VertexStage
                            ).sizeBytes(1 * SizeUtil.MATRIX_SIZE_BYTES),
                            new ShaderRes(
                                    "materials",
                                    2,
                                    CombinedSampler,
                                    FragmentStage
                            ).count(1)
                    )
            );

        }


        font = new Font(
                renderer.getRef(),
                AssetPacks.getAsset("core:assets/fonts/kanit-lightitalic/kanit.png"),
                AssetPacks.getAsset("core:assets/fonts/kanit-lightitalic/kanit.fnt")
        );

    }

    public void drawText(float x, float y, String text, Font font, Color color, RenderQueue renderQueue){

        ByteBuffer vertexBufferData = renderQueue.getDefaultVertexBuffer().get();
        vertexBufferData.clear();
        Texture glyphTexture = font.getTexture();
        Map<Integer, Glyph> glyphs = font.getGlyphs();
        float xc = x;

        StringBuilder line = new StringBuilder();

        float spaceXAdvance = glyphs.get((int) ' ').xadvance;


        for(char c : text.toCharArray()){

            if(c == '\t'){
                xc += spaceXAdvance * 4;
                continue;
            }

            if(c == '\r'){
                xc = x;
                continue;
            }

            Glyph glyph = glyphs.get((int) c);

            if(c == '\n'){

                float height = font.getLineHeight(line.toString());

                y += height;


                line = new StringBuilder();
                xc = x;
                continue;
            }


            float xt = glyph.x;
            float yt = glyph.y;

            float texX = xt / glyphTexture.getWidth();
            float texY = yt / glyphTexture.getHeight();

            float texW = (xt + glyph.w) / glyphTexture.getWidth();
            float texH = (yt + glyph.h) / glyphTexture.getHeight();

            //drawTexture(xc + glyph.xo, y + (glyph.yo), glyph.w, glyph.h, glyphTexture, color, new Rect2D(texX, texY, texW, texH), false, false);

            {



                /*
                PositionFloat3,
                            TransformIndexFloat1,
                            UVFloat2,
                            MaterialBaseIndexFloat1
                 */

                {
                    vertexBufferData.putFloat(xc + glyph.xo);
                    vertexBufferData.putFloat(y + glyph.yo);
                    vertexBufferData.putFloat(0);

                    vertexBufferData.putFloat(color.r);
                    vertexBufferData.putFloat(color.g);
                    vertexBufferData.putFloat(color.b);
                    vertexBufferData.putFloat(color.a);

                    vertexBufferData.putFloat(0);
                    vertexBufferData.putFloat(texX);
                    vertexBufferData.putFloat(texY);
                    vertexBufferData.putFloat(0);

                    //
                    vertexBufferData.putFloat(xc + glyph.xo);
                    vertexBufferData.putFloat(y + glyph.yo + glyph.h);
                    vertexBufferData.putFloat(0);

                    vertexBufferData.putFloat(color.r);
                    vertexBufferData.putFloat(color.g);
                    vertexBufferData.putFloat(color.b);
                    vertexBufferData.putFloat(color.a);

                    vertexBufferData.putFloat(0);
                    vertexBufferData.putFloat(texX);
                    vertexBufferData.putFloat(texH);
                    vertexBufferData.putFloat(0);

                    //
                    vertexBufferData.putFloat(xc + glyph.xo + glyph.w);
                    vertexBufferData.putFloat(y + glyph.yo + glyph.h);
                    vertexBufferData.putFloat(0);

                    vertexBufferData.putFloat(color.r);
                    vertexBufferData.putFloat(color.g);
                    vertexBufferData.putFloat(color.b);
                    vertexBufferData.putFloat(color.a);

                    vertexBufferData.putFloat(0);
                    vertexBufferData.putFloat(texW);
                    vertexBufferData.putFloat(texH);
                    vertexBufferData.putFloat(0);

                    //
                    vertexBufferData.putFloat(xc + glyph.xo + glyph.w);
                    vertexBufferData.putFloat(y + glyph.yo);
                    vertexBufferData.putFloat(0);

                    vertexBufferData.putFloat(color.r);
                    vertexBufferData.putFloat(color.g);
                    vertexBufferData.putFloat(color.b);
                    vertexBufferData.putFloat(color.a);

                    vertexBufferData.putFloat(0);
                    vertexBufferData.putFloat(texW);
                    vertexBufferData.putFloat(texY);
                    vertexBufferData.putFloat(0);




                }




            }



            xc += glyph.xadvance;

            line.append(c);


        }

        int numOfIndices = text.length() * 6;
        int offset = 0;

        ByteBuffer indexBufferData = renderQueue.getDefaultIndexBuffer().get();
        indexBufferData.clear();

        for (int j = 0; j < numOfIndices; j += 6) {

            indexBufferData.putInt(offset);
            indexBufferData.putInt(1 + offset);
            indexBufferData.putInt(2 + offset);
            indexBufferData.putInt(2 + offset);
            indexBufferData.putInt(3 + offset);
            indexBufferData.putInt(offset);

            offset += 4;
        }

    }

    @Override
    public void update(Scene scene, MessageQueue messageQueue) {

        /*
        if(renderer.getRenderQueueByShaderProgram(shaderProgram) == null){
            renderer.newRenderQueue(shaderProgram);
        }

        RenderQueue renderQueue = renderer.getRenderQueueByShaderProgram(shaderProgram);
        renderQueue.reset();

        String text = "Frames Per Sec: " + Time.framesPerSecond() + "\nThis is a test of my text renderer \nsupporting all the hot new things like \nnew lines and \t\t tabs";





        drawText(0, 40, text, font, Color.WHITE, renderQueue);



        ByteBuffer cameraBuffer = renderQueue.getCameraBuffer(renderer.getFrameIndex()).get();
        {
            camera.getView().get(0, cameraBuffer);
            camera.getProj().get(SizeUtil.MATRIX_SIZE_BYTES, cameraBuffer);
        }

        ByteBuffer transformBuffer = renderQueue.getTransformsBuffer(renderer.getFrameIndex()).get();
        {
            new Matrix4f().identity().get(0, transformBuffer);
        }

        renderQueue.addTexture(0, font.getTexture());


        long start = System.currentTimeMillis();

        renderQueue.updateQueue(4 * text.length(), 6 * text.length());


        long end = System.currentTimeMillis() - start;

        //System.out.println("UISystem.update[renderQueue.updateQueue] " + end + "ms");



         */





    }
}
