package testbench;


import fori.Scene;
import fori.Surface;
import fori.asset.AssetPacks;
import fori.ecs.*;
import fori.graphics.*;
import fori.ui.Adapter;
import fori.ui.DarkMode;
import fori.ui.EdgeLayout;
import fori.ui.FlowLayout;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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
    private List<Texture> textureList = new ArrayList<>();
    private int textureIndex;
    private Surface surface;
    private float elapsedTime;

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

        setTheme(new DarkMode());
    }



    @Override
    public void update(Scene scene, MessageQueue messageQueue) {

        if(renderer.getRenderQueueByShaderProgram(shaderProgram) == null){

            RenderQueueFlags renderQueueFlags = new RenderQueueFlags();
            renderQueueFlags.shaderProgram = shaderProgram;
            renderQueueFlags.maxVertices = RenderQueue.MAX_VERTEX_COUNT;
            renderQueueFlags.maxIndices = RenderQueue.MAX_INDEX_COUNT;
            renderQueueFlags.depthOp = RenderQueueFlags.DepthOp.Always;
            renderQueueFlags.depthTest = true;


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
                public void drawRect(float x, float y, float w, float h, Color color) {
                    int thickness = 1;

                    drawFilledRect(x, y, thickness, h, color);
                    drawFilledRect(x, y, w, thickness, color);
                    drawFilledRect(x + w - thickness, y, thickness, h, color);
                    drawFilledRect(x, y + h - thickness, w, thickness, color);


                }

                @Override
                public void drawText(float x, float y, String text, Font font, Color color) {
                    UISystem.this.drawText(renderQueue, x, y, text, font, color);
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



        setAdapter(adapter);
        setSurface(surface);


        long start;

        newContext();
        {


            newWindow("Fori Engine Demo", 60, 60, font, new EdgeLayout());
            {
                newPanel(new EdgeLayout(), North);
                {
                    newPanel(new FlowLayout(Vertical), Center);
                    {

                        text("Renderer: " + renderer.getDeviceName(), font);
                        text("Host: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"), font);
                        text("API: " + Renderer.getRenderAPI(), font);
                        text("AmberUI Render: " + elapsedTime + "ms", font);
                        text(
                                "Java VM: " +
                                        System.getProperty("java.vendor") + " "
                                        + System.getProperty("java.vm.name") + " "
                                        + System.getProperty("java.version"),
                                font
                        );
                        if(button("Just another button", font)) {
                            System.out.println("Doing a thing");
                        }
                        if(button("Just another button", font)) {
                            System.out.println("Doing another thing");
                        }


                    }
                    endPanel();
                }
                endPanel();
            }

            endWindow();



            newWindow("Another another window", 60, 600, font, new EdgeLayout());
            {
                newPanel(new EdgeLayout(), North);
                {
                    newPanel(new FlowLayout(Vertical), Center);
                    {

                        text("Renderer: " + renderer.getDeviceName(), font);
                        text("Host: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"), font);
                        text("API: " + Renderer.getRenderAPI(), font);
                        text("AmberUI Render: " + elapsedTime + "ms", font);
                        text(
                                "Java VM: " +
                                        System.getProperty("java.vendor") + " "
                                        + System.getProperty("java.vm.name") + " "
                                        + System.getProperty("java.version"),
                                font
                        );
                        if(button("Just another button", font)) {
                            System.out.println("Doing a thing");
                        }
                        if(button("Just another button", font)) {
                            System.out.println("Doing another thing");
                        }


                    }
                    endPanel();
                }
                endPanel();
            }

            endWindow();

            newWindow("Another another \n another window", 900, 60, font, new EdgeLayout());
            {
                newPanel(new EdgeLayout(), North);
                {
                    newPanel(new FlowLayout(Vertical), Center);
                    {

                        text("Renderer: " + renderer.getDeviceName(), font);
                        text("Host: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"), font);
                        text("API: " + Renderer.getRenderAPI(), font);
                        text("AmberUI Render: " + elapsedTime + "ms", font);
                        text(
                                "Java VM: " +
                                        System.getProperty("java.vendor") + " "
                                        + System.getProperty("java.vm.name") + " "
                                        + System.getProperty("java.version"),
                                font
                        );
                        if(button("Just another button", font)) {
                            System.out.println("Doing a thing");
                        }
                        if(button("Just another button", font)) {
                            System.out.println("Doing another thing");
                        }


                    }
                    endPanel();
                }
                endPanel();
            }

            endWindow();


            newWindow("Another another \n another window", 900, 60, font, new EdgeLayout());
            {
                newPanel(new EdgeLayout(), North);
                {
                    newPanel(new FlowLayout(Vertical), Center);
                    {

                        text("Renderer: " + renderer.getDeviceName(), font);
                        text("Host: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"), font);
                        text("API: " + Renderer.getRenderAPI(), font);
                        text("AmberUI Render: " + elapsedTime + "ms", font);
                        text(
                                "Java VM: " +
                                        System.getProperty("java.vendor") + " "
                                        + System.getProperty("java.vm.name") + " "
                                        + System.getProperty("java.version"),
                                font
                        );
                        if(button("Just another button", font)) {
                            System.out.println("Doing a thing");
                        }
                        if(button("Just another button", font)) {
                            System.out.println("Doing another thing");
                        }


                    }
                    endPanel();
                }
                endPanel();
            }

            endWindow();

            newWindow("Another another \n another window", 900, 60, font, new EdgeLayout());
            {
                newPanel(new EdgeLayout(), North);
                {
                    newPanel(new FlowLayout(Vertical), Center);
                    {

                        text("Renderer: " + renderer.getDeviceName(), font);
                        text("Host: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"), font);
                        text("API: " + Renderer.getRenderAPI(), font);
                        text("AmberUI Render: " + elapsedTime + "ms", font);
                        text(
                                "Java VM: " +
                                        System.getProperty("java.vendor") + " "
                                        + System.getProperty("java.vm.name") + " "
                                        + System.getProperty("java.version"),
                                font
                        );
                        if(button("Just another button", font)) {
                            System.out.println("Doing a thing");
                        }
                        if(button("Just another button", font)) {
                            System.out.println("Doing another thing");
                        }


                    }
                    endPanel();
                }
                endPanel();
            }

            endWindow();

            newWindow("Another another \n another window", 900, 60, font, new EdgeLayout());
            {
                newPanel(new EdgeLayout(), North);
                {
                    newPanel(new FlowLayout(Vertical), Center);
                    {

                        text("Renderer: " + renderer.getDeviceName(), font);
                        text("Host: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"), font);
                        text("API: " + Renderer.getRenderAPI(), font);
                        text("AmberUI Render: " + elapsedTime + "ms", font);
                        text(
                                "Java VM: " +
                                        System.getProperty("java.vendor") + " "
                                        + System.getProperty("java.vm.name") + " "
                                        + System.getProperty("java.version"),
                                font
                        );
                        if(button("Just another button", font)) {
                            System.out.println("Doing a thing");
                        }
                        if(button("Just another button", font)) {
                            System.out.println("Doing another thing");
                        }


                    }
                    endPanel();
                }
                endPanel();
            }

            endWindow();



            start = System.currentTimeMillis();

            render();

            elapsedTime = System.currentTimeMillis() - start;
        }
        endContext();




        renderQueue.updateQueue(quadCount * 4, quadCount * 6);
        quadCount = 0;
        renderQueue.getDefaultVertexBuffer().get().clear();
        renderQueue.getDefaultIndexBuffer().get().clear();
    }

    private void drawText(RenderQueue renderQueue, float x, float y, String text, Font font, Color color) {
        Texture glyphsTexture = font.getTexture();

        Glyph[] glyphs = font.getGlyphs();
        float xc = x;

        StringBuilder line = new StringBuilder();

        float spaceXAdvance = glyphs[' '].xadvance;


        for(char c : text.toCharArray()) {

            if (c == '\t') {
                xc += spaceXAdvance * 4;
                continue;
            }

            if (c == '\r') {
                xc = x;
                continue;
            }

            Glyph glyph = glyphs[c];

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

    private int quadCount = 0;

    private void drawFilledRect(RenderQueue renderQueue, float x, float y, float w, float h, Color color) {
        drawQuad(renderQueue, x, y, w, h, color, 2, -1, new Rect2D(0, 0, 1, 1), false, false, -1);
    }

    private void drawCircle(RenderQueue renderQueue, float x, float y, float w, float h, Color color, float thickness) {
        drawQuad(renderQueue, x, y, w, h, color, 1, thickness, new Rect2D(0, 0, 1, 1), false, false, -1);
    }

    public void drawTexture(RenderQueue renderQueue, float x, float y, float w, float h, float tx, float ty, float tw, float th, Texture texture, Color color) {
        int textureIndex = 0;

        if(textureList.contains(texture)) {
            textureIndex = textureList.indexOf(texture);
        }
        else {
            textureList.add(texture);
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

        int offset = (quadCount) * 4;


        indexBuffer.putInt(offset);
        indexBuffer.putInt(1 + offset);
        indexBuffer.putInt(2 + offset);
        indexBuffer.putInt(2 + offset);
        indexBuffer.putInt(3 + offset);
        indexBuffer.putInt(offset);


        quadCount++;

    }
}
