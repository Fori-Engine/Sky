package lake.graphics.opengl;

import lake.FlightRecorder;
import lake.graphics.*;
import org.joml.*;
import org.lwjgl.BufferUtils;
import lake.FileReader;
import org.lwjgl.opengl.GLUtil;

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
    public GLShaderProgram defaultShaderProgram, currentShaderProgram;
    private FastTextureLookup textureLookup;
    private Framebuffer2D framebuffer2D;
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



        defaultShaderProgram = new GLShaderProgram(
                FileReader.readFile("assets/shaders/opengl/VertexShader.glsl"),
                FileReader.readFile("assets/shaders/opengl/FragmentShader.glsl")
        );
        defaultShaderProgram.prepare();

        currentShaderProgram = defaultShaderProgram;
        currentShaderProgram.bind();
        updateMatrices();

        {
            vertexArray = new GLVertexArray();
            vertexArray.bind();
            vertexArray.setVertexAttributes(
                    new GLVertexAttribute(0, 2, false, "v_pos"),
                    new GLVertexAttribute(1, 2, false, "v_uv"),
                    new GLVertexAttribute(2, 1, false, "v_texindex"),
                    new GLVertexAttribute(3, 4, false, "v_color"),
                    new GLVertexAttribute(4, 1, false, "v_thickness"),
                    new GLVertexAttribute(5, 1, false, "v_bloom")
            );


            FlightRecorder.info(GLRenderer2D.class, "Calculated vertex stride is " + vertexArray.getStride() + " bytes");


            vertexBuffer = new GLVertexBuffer(settings.quadsPerBatch, vertexArray.getStride() / Float.BYTES);
            indexBuffer = new GLIndexBuffer(settings.quadsPerBatch, 6, Integer.BYTES);



            vertexArray.build();



        }

    }
    @Override
    public void setShaderProgram(ShaderProgram shaderProgram) {
        if(currentShaderProgram != shaderProgram) {
            currentShaderProgram = (GLShaderProgram) shaderProgram;
            currentShaderProgram.bind();
            updateMatrices();
        }
    }
    public void updateMatrices(){
        currentShaderProgram.setMatrix4f("v_model", getModel());
        currentShaderProgram.setMatrix4f("v_view", getView());
        currentShaderProgram.setMatrix4f("v_projection", getProj());
        currentShaderProgram.setIntArray("u_textures", createTextureSlots());
    }
    @Override
    public ShaderProgram getDefaultShaderProgram() {
        return defaultShaderProgram;
    }
    @Override
    public ShaderProgram getCurrentShaderProgram() {
        return currentShaderProgram;
    }
    private int[] createTextureSlots() {
        int[] slots = new int[maxTextureSlots];
        for (int i = 0; i < maxTextureSlots; i++) {
            slots[i] = i;
        }
        return slots;
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
            glActiveTexture(GL_TEXTURE0 + slot);
            glTexture2D.bind();
            glTexture2D.setSlot(slot);
            textureLookup.registerTexture(glTexture2D, slot);
            isUniqueTexture = true;
        }


        drawQuad(x, y, w, h, slot, color, originX, originY, rect2D, -1, xFlip, yFlip, 0);

        if(isUniqueTexture) nextTextureSlot++;

        if(nextTextureSlot == maxTextureSlots)
            render("Next Batch Render");

    }



    @Override
    public void drawQuad(float x,
                         float y,
                         float w,
                         float h,
                         int slot,
                         Color color,
                         float originX,
                         float originY,
                         Rect2D region,
                         float thickness,
                         boolean xFlip,
                         boolean yFlip,
                         float bloom){


        Quad quad = applyTransformations(x, y, w, h, originX, originY, region, xFlip, yFlip);

        Vector4f[] transformedPoints = quad.transformedPoints;
        Rect2D copy = quad.textureCoords;

        Vector4f topLeft = transformedPoints[0];
        Vector4f topRight = transformedPoints[1];
        Vector4f bottomLeft = transformedPoints[2];
        Vector4f bottomRight = transformedPoints[3];



        int dataPerQuad = vertexBuffer.getVertexDataSize() * 4;

        glBufferSubData(GL_ARRAY_BUFFER, quadIndex * dataPerQuad * Float.BYTES, new float[]{
                topLeft.x,
                topLeft.y,
                copy.x,
                copy.y,
                slot,
                color.r,
                color.g,
                color.b,
                color.a,
                thickness,
                bloom,
                bottomLeft.x,
                bottomLeft.y,
                copy.x,
                copy.h,
                slot,
                color.r,
                color.g,
                color.b,
                color.a,
                thickness,
                bloom,
                bottomRight.x,
                bottomRight.y,
                copy.w,
                copy.h,
                slot,
                color.r,
                color.g,
                color.b,
                color.a,
                thickness,
                bloom,
                topRight.x,
                topRight.y,
                copy.w,
                copy.y,
                slot,
                color.r,
                color.g,
                color.b,
                color.a,
                thickness,
                bloom,
        });


        quadIndex++;


        if(quadIndex == vertexBuffer.getMaxQuads()) render("Next Batch Render");
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


        int[] indices = generateIndices(quadIndex);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.myEbo);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indices);

        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);



        quadIndex = 0;
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
