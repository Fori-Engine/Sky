package testbench;


import fori.Scene;
import fori.asset.AssetPacks;
import fori.ecs.EntitySystem;
import fori.ecs.MessageQueue;
import fori.graphics.*;
import org.joml.Matrix4f;
import org.lwjgl.nuklear.*;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.Map;

import static fori.graphics.Attributes.Type.*;
import static fori.graphics.ShaderRes.ShaderStage.VertexStage;
import static fori.graphics.ShaderRes.Type.*;
import static org.lwjgl.nuklear.Nuklear.*;



public class UISystem extends EntitySystem {

    private ShaderProgram shaderProgram;
    private NkContext context;
    private NkAllocator allocator;
    private Renderer renderer;
    private NkBuffer commands;
    private NkDrawNullTexture drawNullTexture;
    private NkDrawVertexLayoutElement.Buffer vertexLayout;


    public UISystem(Renderer renderer) {
        this.renderer = renderer;
        Matrix4f proj = new Matrix4f().ortho(0, renderer.getWidth(), 0, renderer.getHeight(), 0, 1, true);

        allocator = NkAllocator.create();

        context = NkContext.create();
        nk_init(context, allocator, null);

        drawNullTexture = NkDrawNullTexture.create();
        drawNullTexture.texture().id(0);
        drawNullTexture.uv().set(0, 0);

        commands = NkBuffer.create();

        vertexLayout = NkDrawVertexLayoutElement.create(4)
                .position(0).attribute(NK_VERTEX_POSITION).format(NK_FORMAT_FLOAT).offset(0)
                .position(1).attribute(NK_VERTEX_TEXCOORD).format(NK_FORMAT_FLOAT).offset(8)
                .position(2).attribute(NK_VERTEX_COLOR).format(NK_FORMAT_R8G8B8A8).offset(16)
                .position(3).attribute(NK_VERTEX_ATTRIBUTE_COUNT).format(NK_FORMAT_COUNT).offset(0)
                .flip();


        {
            ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                    AssetPacks.<String> getAsset("core:assets/shaders/vulkan/UI.glsl").asset
            );

            shaderProgram = ShaderProgram.newShaderProgram(renderer.getRef(), shaderSources.vertexShader, shaderSources.fragmentShader);
            shaderProgram.bind(
                    new Attributes.Type[]{
                            PositionFloat2,
                            UVFloat2,
                            ColorFloat1

                    },
                    new ShaderResSet(
                            0,
                            new ShaderRes(
                                    "proj",
                                    0,
                                    UniformBuffer,
                                    VertexStage
                            ).sizeBytes(SizeUtil.MATRIX_SIZE_BYTES)
                    )
            );

            Buffer buffer = Buffer.newBuffer(renderer.getRef(), SizeUtil.MATRIX_SIZE_BYTES, Buffer.Usage.UniformBuffer, Buffer.Type.CPUGPUShared, false);
            proj.get(buffer.get());

            for (int i = 0; i < renderer.getMaxFramesInFlight(); i++) {
                shaderProgram.updateBuffers(i, new ShaderUpdate<>("proj", 0, 0, buffer));
            }



        }

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


        if(renderer.getRenderQueueByShaderProgram(shaderProgram) == null){
            renderer.newRenderQueue(shaderProgram);
        }

        RenderQueue renderQueue = renderer.getRenderQueueByShaderProgram(shaderProgram);
        renderQueue.reset();

        ByteBuffer vertexBuffer = renderQueue.getDefaultVertexBuffer().get();
        ByteBuffer indexBuffer = renderQueue.getDefaultIndexBuffer().get();

        try(MemoryStack stack = MemoryStack.stackPush()) {

            NkConvertConfig config = NkConvertConfig.calloc(stack)
                    .vertex_layout(vertexLayout)
                    .vertex_size(20)
                    .vertex_alignment(4)
                    .tex_null(drawNullTexture)
                    .circle_segment_count(22)
                    .curve_segment_count(22)
                    .arc_segment_count(22)
                    .global_alpha(1.0f)
                    .shape_AA(nk_true)
                    .line_AA(nk_true);

            // setup buffers to load vertices and elements
            NkBuffer vbuf = NkBuffer.malloc(stack);
            NkBuffer ebuf = NkBuffer.malloc(stack);

            nk_buffer_init_fixed(vbuf, vertexBuffer);
            nk_buffer_init_fixed(ebuf, indexBuffer);
            nk_convert(context, commands, vbuf, ebuf, config);

            for (NkDrawCommand cmd = nk__draw_begin(context, commands); cmd != null; cmd = nk__draw_next(cmd, commands, context)) {
                if (cmd.elem_count() == 0) {
                    continue;
                }

                //glDrawElements(GL_TRIANGLES, cmd.elem_count(), GL_UNSIGNED_SHORT, offset);
                //offset += cmd.elem_count() * 2;
            }
            nk_clear(context);
            nk_buffer_clear(commands);

        }










    }
}
