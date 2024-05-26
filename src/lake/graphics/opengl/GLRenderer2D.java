package lake.graphics.opengl;

import lake.FlightRecorder;
import lake.asset.AssetPacks;
import lake.graphics.*;
import lake.graphics.vulkan.LVKRenderFrame;
import lake.graphics.vulkan.LVKTexture2D;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GLUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL46.*;
/***
 * A Renderer2D is a Batch Renderer responsible for drawing 2D Graphics to the screen. This manages lots of OpenGL state internally including the current shader
 * VertexArray, VertexBuffer, and the Model, View and Projection Matrices. It also includes some debugging utilities to track draw calls using setDebug()
 */
public class GLRenderer2D extends Renderer2D {
    private GLVertexArray vertexArray;
    private GLVertexBuffer vertexBuffer;
    private GLIndexBuffer indexBuffer;
    private int maxTextureSlots;
    private FastTextureLookup textureLookup;
    private Framebuffer2D framebuffer2D;

    public static ShaderResource modelViewProj;
    public static ShaderResource sampler2DArray;






    public GLRenderer2D(int width, int height, Framebuffer2D framebuffer2D, RenderSettings settings){
        this(width, height, settings);
        this.framebuffer2D = framebuffer2D;
    }
    public GLRenderer2D(int width, int height, RenderSettings settings) {
        super(width, height, settings);



        if(settings.enableValidation)
            GLUtil.setupDebugMessageCallback();

        if(settings.msaa)
            glEnable(GL_MULTISAMPLE);
        else
            glDisable(GL_MULTISAMPLE);

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);


        proj = new Matrix4f().ortho(0, width, height, 0, 0, 1);


        IntBuffer d = BufferUtils.createIntBuffer(1);
        glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS, d);
        maxTextureSlots  = d.get();
        textureLookup = new FastTextureLookup(maxTextureSlots);


        ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                AssetPacks.<String> getAsset("core:assets/shaders/opengl/Default.glsl").asset
        );

        modelViewProj = new ShaderResource(0)
                .type(ShaderResource.Type.UniformBuffer)
                .shaderStage(ShaderResource.ShaderStage.VertexStage)
                .sizeBytes(LVKRenderFrame.LVKFrameUniforms.TOTAL_SIZE_BYTES)
                .count(1);

        sampler2DArray = new ShaderResource(1)
                .count(32)
                .type(ShaderResource.Type.CombinedSampler)
                .shaderStage(ShaderResource.ShaderStage.FragmentStage);

        ShaderResource color = new ShaderResource(2)
                .type(ShaderResource.Type.UniformBuffer)
                .shaderStage(ShaderResource.ShaderStage.FragmentStage)
                .sizeBytes(4 * Float.BYTES)
                .count(1);

        defaultShaderProgram = new GLShaderProgram(
                shaderSources.vertexShader,
                shaderSources.fragmentShader
        );
        defaultShaderProgram.prepare();
        defaultShaderProgram.addResource(modelViewProj);
        defaultShaderProgram.addResource(sampler2DArray);
        defaultShaderProgram.addResource(color);


        setShaderProgram(defaultShaderProgram);

        ByteBuffer[] colorBuffer = currentShaderProgram.mapUniformBuffer(color);

        for(ByteBuffer buffer : colorBuffer){
            buffer.putFloat(0.91f);
            buffer.putFloat(0.01f);
            buffer.putFloat(0.01f);
            buffer.putFloat(1f);
        }

        currentShaderProgram.unmapUniformBuffer(color, colorBuffer);

        //This needs to go somewhere to bind each sampler to a texture slot
        currentShaderProgram.setIntArray("u_textures", GLShaderProgram.createTextureSlots(32));




        {
            vertexArray = new GLVertexArray();
            vertexArray.bind();
            vertexArray.setVertexAttributes(
                    new GLVertexAttribute(0, 2, false, "v_pos"),
                    new GLVertexAttribute(1, 2, false, "v_uv"),
                    new GLVertexAttribute(2, 1, false, "v_texindex"),
                    new GLVertexAttribute(3, 4, false, "v_color"),
                    new GLVertexAttribute(4, 1, false, "v_thickness")
                    //new GLVertexAttribute(5, 1, false, "v_bloom")
            );


            FlightRecorder.info(GLRenderer2D.class, "Calculated vertex stride is " + vertexArray.getStride() + " bytes");


            vertexBuffer = new GLVertexBuffer(settings.quadsPerBatch, vertexArray.getStride() / Float.BYTES);
            indexBuffer = new GLIndexBuffer(settings.quadsPerBatch, 6, Integer.BYTES);



            vertexArray.build();



        }

    }
    @Override
    public void setShaderProgram(ShaderProgram sp) {

        GLShaderProgram shaderProgram = (GLShaderProgram) sp;

        if(currentShaderProgram != shaderProgram) {
            currentShaderProgram = shaderProgram;
            currentShaderProgram.bind();

            if(!shaderProgram.isUbosInitialized()){
                shaderProgram.createUniformBufferMemory();
                shaderProgram.setUbosInitialized(true);
            }

            updateMatrices();
        }
    }
    public void updateMatrices(){
        ByteBuffer[] buffers = currentShaderProgram.mapUniformBuffer(modelViewProj);

        for(ByteBuffer buffer : buffers) {
            model.get(buffer);
            camera.getViewMatrix().get(LVKRenderFrame.LVKFrameUniforms.MATRIX_SIZE_BYTES, buffer);
            proj.get(2 * LVKRenderFrame.LVKFrameUniforms.MATRIX_SIZE_BYTES, buffer);
        }

        currentShaderProgram.unmapUniformBuffer(modelViewProj, buffers);
    }

    public GLVertexBuffer getVertexBuffer() {
        return vertexBuffer;
    }
    public Framebuffer2D getFramebuffer2D() {
        return framebuffer2D;
    }
    public boolean isUsingFramebuffer(){
        return getFramebuffer2D() != null;
    }
    private int nextTextureSlot;


    @Override
    public void drawTexture(float x, float y, float w, float h, Texture2D texture, Color color, Rect2D rect2D, boolean xFlip, boolean yFlip) {
        GLTexture2D glTexture2D = (GLTexture2D) texture;

        int slot = nextTextureSlot;
        boolean isUniqueTexture = false;



        //Existing texture
        if (textureLookup.hasTexture(glTexture2D)) {
            slot = textureLookup.getTexture(glTexture2D);
        }

        //Unique Texture
        else {
            currentShaderProgram.updateSampler2DArray(sampler2DArray, slot, glTexture2D);
            textureLookup.registerTexture(glTexture2D, slot);
            isUniqueTexture = true;
        }


        drawQuad(x, y, w, h, slot, color, originX, originY, rect2D, -1, xFlip, yFlip);

        if(isUniqueTexture) nextTextureSlot++;

        if(nextTextureSlot == maxTextureSlots)
            render("Next Batch Render");

    }



    @Override
    public void drawQuad(float x,
                         float y,
                         float w,
                         float h,
                         int quadTypeOrTextureIndex,
                         Color color,
                         float originX,
                         float originY,
                         Rect2D region,
                         float thickness,
                         boolean xFlip,
                         boolean yFlip){


        Quad quad = applyTransformations(x, y, w, h, originX, originY, region, xFlip, yFlip);

        Vector4f[] transformedPoints = quad.transformedPoints;
        Rect2D copy = quad.textureCoords;

        Vector4f topLeft = transformedPoints[0];
        Vector4f topRight = transformedPoints[1];
        Vector4f bottomLeft = transformedPoints[2];
        Vector4f bottomRight = transformedPoints[3];



        int dataPerQuad = vertexBuffer.getVertexDataSize() * 4;

        glBufferSubData(GL_ARRAY_BUFFER, quadCount * dataPerQuad * Float.BYTES, new float[]{
                topLeft.x,
                topLeft.y,
                copy.x,
                copy.y,
                quadTypeOrTextureIndex,
                color.r,
                color.g,
                color.b,
                color.a,
                thickness,
                bottomLeft.x,
                bottomLeft.y,
                copy.x,
                copy.h,
                quadTypeOrTextureIndex,
                color.r,
                color.g,
                color.b,
                color.a,
                thickness,
                bottomRight.x,
                bottomRight.y,
                copy.w,
                copy.h,
                quadTypeOrTextureIndex,
                color.r,
                color.g,
                color.b,
                color.a,
                thickness,
                topRight.x,
                topRight.y,
                copy.w,
                copy.y,
                quadTypeOrTextureIndex,
                color.r,
                color.g,
                color.b,
                color.a,
                thickness,
        });


        quadCount++;


        if(quadCount == vertexBuffer.getMaxQuads()) render("Next Batch Render");
    }
    public void render() {
        render("Final Render");

        if(framebuffer2D != null)
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    public void render(String renderName) {



        currentShaderProgram.bind();
        vertexArray.bind();

        glBindBuffer(GL_ARRAY_BUFFER, getVertexBuffer().myVbo);


        int[] indices = generateIndices(quadCount);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.myEbo);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indices);

        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);



        quadCount = 0;
        nextTextureSlot = 0;
        textureLookup.clear();


    }
    public void clear(Color clearColor) {
        super.clear(clearColor);


        if(framebuffer2D != null){
            framebuffer2D.bind();
            glClearTexImage(framebuffer2D.getTexture2D().getTexID(), 0, GL_RGBA, GL_FLOAT, new float[]{clearColor.r, clearColor.g, clearColor.b, clearColor.a});
            return;
        }

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a);
    }
    @Override
    public String getDeviceName() {
        return glGetString(GL_RENDERER);
    }
    @Override
    public void dispose() {

    }
}
