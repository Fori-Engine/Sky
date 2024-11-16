package testbench;


import fori.Input;
import fori.Scene;
import fori.Surface;
import fori.asset.AssetPacks;
import fori.ecs.*;
import fori.graphics.*;
import fori.ui.Adapter;
import fori.ui.EdgeLayout;
import fori.ui.FlowLayout;
import org.intellij.lang.annotations.Flow;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static fori.graphics.Attributes.Type.*;
import static fori.graphics.ShaderRes.ShaderStage.FragmentStage;
import static fori.graphics.ShaderRes.ShaderStage.VertexStage;
import static fori.graphics.ShaderRes.Type.*;
import static fori.ui.AmberUI.*;
import static fori.ui.Flags.*;


public class UISystem extends EntitySystem {

    private ShaderProgram shaderProgram;
    private Renderer renderer;
    private Adapter adapter;
    private Font font;
    private HashMap<Texture, Integer> textureLookup = new HashMap<>();
    private int textureIndex;
    private Surface surface;
    private float value;

    public UISystem(Surface surface, Renderer renderer) {
        this.surface = surface;
        this.renderer = renderer;
        Matrix4f proj = new Matrix4f().ortho(0, renderer.getWidth(), 0, renderer.getHeight(), 0, 1, true);
        font = new Font(renderer.getRef(), AssetPacks.getAsset("core:assets/fonts/open-sans/opensans.png"), AssetPacks.getAsset("core:assets/fonts/open-sans/opensans.fnt"));

        {
            ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                    AssetPacks.<String> getAsset("core:assets/shaders/vulkan/UI.glsl").asset
            );

            shaderProgram = ShaderProgram.newShaderProgram(renderer.getRef(), shaderSources.vertexShader, shaderSources.fragmentShader);
            shaderProgram.bind(
                    new Attributes.Type[]{
                            PositionFloat2,
                            QuadTypeFloat1,
                            ColorFloat4,
                            CircleThicknessFloat1,
                            UVFloat2,
                            TextureIndexFloat1


                    },
                    new ShaderResSet(
                            0,
                            new ShaderRes(
                                    "proj",
                                    0,
                                    UniformBuffer,
                                    VertexStage
                            ).sizeBytes(SizeUtil.MATRIX_SIZE_BYTES),
                            new ShaderRes(
                                    "textures",
                                    1,
                                    CombinedSampler,
                                    FragmentStage
                            ).count(1)
                    )

            );

            Buffer buffer = Buffer.newBuffer(renderer.getRef(), SizeUtil.MATRIX_SIZE_BYTES, Buffer.Usage.UniformBuffer, Buffer.Type.CPUGPUShared, false);
            proj.get(buffer.get());

            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                shaderProgram.updateBuffers(i, new ShaderUpdate<>("proj", 0, 0, buffer));
            }



        }




    }


    @Override
    public void update(Scene scene, MessageQueue messageQueue) {
        if(renderer.getRenderQueueByShaderProgram(shaderProgram) == null){

            RenderQueueFlags renderQueueFlags = new RenderQueueFlags();
            renderQueueFlags.shaderProgram = shaderProgram;
            renderQueueFlags.maxVertices = RenderQueue.MAX_VERTEX_COUNT;
            renderQueueFlags.maxIndices = RenderQueue.MAX_INDEX_COUNT;
            renderQueueFlags.depthTest = false;


            renderer.newRenderQueue(renderQueueFlags);
        }






        RenderQueue renderQueue = renderer.getRenderQueueByShaderProgram(shaderProgram);
        renderQueue.reset();


        {
            adapter = new Adapter() {
                @Override
                public Vector2f getSize() {
                    return new Vector2f(renderer.getWidth(), renderer.getHeight());
                }

                @Override
                public void drawFilledRect(float x, float y, float w, float h, Color color) {
                    UISystem.this.drawFilledRect(renderQueue, x, y, w, h, color);
                }

                @Override
                public void drawText(float x, float y, String text, Font font, Color color) {
                    Texture glyphsTexture = font.getTexture();

                    Map<Integer, Glyph> glyphs = font.getGlyphs();
                    float xc = x;

                    StringBuilder line = new StringBuilder();

                    float spaceXAdvance = glyphs.get((int) ' ').xadvance;


                    for(char c : text.toCharArray()) {

                        if (c == '\t') {
                            xc += spaceXAdvance * 4;
                            continue;
                        }

                        if (c == '\r') {
                            xc = x;
                            continue;
                        }

                        Glyph glyph = glyphs.get((int) c);

                        if (c == '\n') {

                            float height = font.getLineHeight(line.toString());

                            y += height;


                            line = new StringBuilder();
                            xc = x;
                            continue;
                        }


                        float xt = glyph.x;
                        float yt = glyph.y;

                        float texX = xt / glyphsTexture.getWidth();
                        float texY = yt / glyphsTexture.getHeight();

                        float texW = (xt + glyph.w) / glyphsTexture.getWidth();
                        float texH = (yt + glyph.h) / glyphsTexture.getHeight();

                        UISystem.this.drawTexture(renderQueue, xc + glyph.xo, y + glyph.yo, glyph.w, glyph.h, texX, texY, texW, texH, glyphsTexture, color);


                        xc += glyph.xadvance;

                        line.append(c);

                    }
                }

                @Override
                public void drawTexture(float x, float y, float w, float h, float tx, float ty, float tw, float th, Texture texture, Color color) {
                    UISystem.this.drawTexture(renderQueue, x, y, w, h, tx, ty, tw, th, texture, color);
                }

                @Override
                public void drawFilledCircle(float x, float y, float w, float h, float thickness, Color color) {
                    UISystem.this.drawCircle(renderQueue, x, y, w, h, color, thickness);
                }


            };
        }

        renderQueue.getDefaultVertexBuffer().get().clear();
        renderQueue.getDefaultIndexBuffer().get().clear();


        setAdapter(adapter);
        setSurface(surface);

        newContext("AmberUITest");
        newWindow("Window 1", 60, 60, font, new EdgeLayout());
        {
            newPanel(new EdgeLayout(), North);
            {


                newPanel(new FlowLayout(Vertical), Center);
                {
                    text("This is text", font, Color.WHITE);
                    if (button("Click me!", font, Color.BLUE)) {
                        System.out.println(9);
                    }
                    button("Button", font, Color.WHITE);
                    button("Button", font, Color.WHITE);
                    button("Button", font, Color.WHITE);

                    button("Button", font, Color.RED);
                    button("Button", font, Color.RED);
                    button("Button", font, Color.RED);
                    button("Button", font, Color.RED);

                    text("A really really really really really really really long string", font, Color.WHITE);

                }
                endPanel();
            }
            endPanel();
        }

        endWindow();

        newWindow("Window 2", 300, 300, font, new FlowLayout(Horizontal));
        {
            button("Button", font, Color.RED);
            button("Button", font, Color.LIGHT_GRAY);
        }
        endWindow();


        newWindow("Window 3", 600, 600, font, new EdgeLayout());
        {
            newPanel(new EdgeLayout(), North);
            {


                newPanel(new FlowLayout(Vertical), Center);
                {
                    text("This is text", font, Color.WHITE);
                    if (button("Click me!", font, Color.BLUE)) {
                        System.out.println(9);
                    }

                    newPanel(new FlowLayout(Horizontal));
                    {
                        button("Button", font, Color.WHITE);
                        button("Button", font, Color.WHITE);
                    }
                    endPanel();

                    newPanel(new FlowLayout(Horizontal));
                    {
                        button("Button", font, Color.RED);
                        button("Button", font, Color.RED);
                    }
                    endPanel();

                    newPanel(new FlowLayout(Horizontal));
                    {
                        button("Button", font, Color.GREEN);
                        button("Button", font, Color.GREEN);
                    }
                    endPanel();

                    newPanel(new FlowLayout(Horizontal));
                    {
                        button("Button", font, Color.BLUE);
                        button("Button", font, Color.BLUE);
                    }
                    endPanel();

                    newPanel(new FlowLayout(Horizontal));
                    {
                        button("Button", font, Color.WHITE);
                        button("Button", font, Color.WHITE);
                    }
                    endPanel();

                    newPanel(new FlowLayout(Horizontal));
                    {
                        button("Button", font, Color.WHITE);
                        button("Button", font, Color.WHITE);
                    }
                    endPanel();


                }
                endPanel();
            }
            endPanel();
        }
        endWindow();





        render();

        endContext();



        //drawFilledRect(renderQueue, 0, 0, 60, 60, Color.RED);


        renderQueue.updateQueue(quadIndex * 4, quadIndex * 12);
        quadIndex = 0;

    }

    private int quadIndex = 0;

    private void drawFilledRect(RenderQueue renderQueue, float x, float y, float w, float h, Color color) {
        drawQuad(renderQueue, x, y, w, h, color, 2, -1, new Rect2D(0, 0, 1, 1), false, false, -1);
    }

    private void drawCircle(RenderQueue renderQueue, float x, float y, float w, float h, Color color, float thickness) {
        drawQuad(renderQueue, x, y, w, h, color, 1, thickness, new Rect2D(0, 0, 1, 1), false, false, -1);
    }

    public void drawTexture(RenderQueue renderQueue, float x, float y, float w, float h, float tx, float ty, float tw, float th, Texture texture, Color color) {

        int textureIndex = 0;

        if(textureLookup.containsKey(texture)) {
             textureIndex = textureLookup.get(texture);
        }
        else {
            textureLookup.put(texture, this.textureIndex);
            renderer.waitForDevice();

            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                shaderProgram.updateTextures(i, new ShaderUpdate<>("textures", 0, 1, texture).arrayIndex(textureIndex));
            }

            this.textureIndex++;
        }


        drawQuad(renderQueue, x, y, w, h, color, 0, 0, new Rect2D(tx, ty, tw, th), false, false, textureIndex);




    }

    private void drawQuad(RenderQueue renderQueue, float x, float y, float w, float h, Color color, int type, float thickness, Rect2D uv, boolean xFlip, boolean yFlip, int textureIndex) {
        ByteBuffer vertexBuffer = renderQueue.getDefaultVertexBuffer().get();
        ByteBuffer indexBuffer = renderQueue.getDefaultIndexBuffer().get();

        Rect2D copy = new Rect2D(uv.x, uv.y, uv.w, uv.h);

        if(xFlip){
            float temp = copy.x;
            copy.x = copy.w;
            copy.w = temp;
        }

        if(yFlip){
            float temp = copy.y;
            copy.y = copy.h;
            copy.h = temp;
        }


        vertexBuffer.putFloat(x);
        vertexBuffer.putFloat(y);
        vertexBuffer.putFloat(type);
        vertexBuffer.putFloat(color.r);
        vertexBuffer.putFloat(color.g);
        vertexBuffer.putFloat(color.b);
        vertexBuffer.putFloat(color.a);
        vertexBuffer.putFloat(thickness);
        vertexBuffer.putFloat(copy.x);
        vertexBuffer.putFloat(copy.y);
        vertexBuffer.putFloat(textureIndex);

        vertexBuffer.putFloat(x);
        vertexBuffer.putFloat(y + h);
        vertexBuffer.putFloat(type);
        vertexBuffer.putFloat(color.r);
        vertexBuffer.putFloat(color.g);
        vertexBuffer.putFloat(color.b);
        vertexBuffer.putFloat(color.a);
        vertexBuffer.putFloat(thickness);
        vertexBuffer.putFloat(copy.x);
        vertexBuffer.putFloat(copy.h);
        vertexBuffer.putFloat(textureIndex);

        vertexBuffer.putFloat(x + w);
        vertexBuffer.putFloat(y + h);
        vertexBuffer.putFloat(type);
        vertexBuffer.putFloat(color.r);
        vertexBuffer.putFloat(color.g);
        vertexBuffer.putFloat(color.b);
        vertexBuffer.putFloat(color.a);
        vertexBuffer.putFloat(thickness);
        vertexBuffer.putFloat(copy.w);
        vertexBuffer.putFloat(copy.h);
        vertexBuffer.putFloat(textureIndex);

        vertexBuffer.putFloat(x + w);
        vertexBuffer.putFloat(y);
        vertexBuffer.putFloat(type);
        vertexBuffer.putFloat(color.r);
        vertexBuffer.putFloat(color.g);
        vertexBuffer.putFloat(color.b);
        vertexBuffer.putFloat(color.a);
        vertexBuffer.putFloat(thickness);
        vertexBuffer.putFloat(copy.w);
        vertexBuffer.putFloat(copy.y);
        vertexBuffer.putFloat(textureIndex);

        int offset = (quadIndex) * 4;


        indexBuffer.putInt(offset);
        indexBuffer.putInt(1 + offset);
        indexBuffer.putInt(2 + offset);
        indexBuffer.putInt(2 + offset);
        indexBuffer.putInt(3 + offset);
        indexBuffer.putInt(offset);

        quadIndex++;
    }
}
