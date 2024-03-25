package lake.graphics.opengl;

import lake.graphics.*;
import org.joml.*;
import org.lwjgl.BufferUtils;
import lake.FileReader;

import java.lang.Math;
import java.nio.IntBuffer;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL46.*;
/***
 * A Renderer2D is a Batch Renderer responsible for drawing 2D Graphics to the screen. This manages lots of OpenGL state internally including the current shader
 * VertexArray, VertexBuffer, and the Model, View and Projection Matrices. It also includes some debugging utilities to track draw calls using setDebug()
 */
public class GLRenderer2D extends Renderer2D implements Disposable {
    private GLVertexArray vertexArray;
    private GLVertexBuffer vertexBuffer;
    private float[] vertexData;
    private int maxTextureSlots;
    public GLShaderProgram defaultGLShaderProgram, currentGLShaderProgram;
    private ArrayList<String> renderCallNames = new ArrayList<>(50);
    private int RECT = -1;
    private int CIRCLE = -2;
    private FastTextureLookup textureLookup;
    private Framebuffer2D framebuffer2D;


    public GLRenderer2D(int width, int height, boolean msaa, Framebuffer2D framebuffer2D){
        this(width, height, msaa);
        this.framebuffer2D = framebuffer2D;
    }

    public GLRenderer2D(int width, int height, boolean msaa) {
        super(width, height, msaa);
        Disposer.add("renderer", this);

        //GLUtil.setupDebugMessageCallback();

        if(msaa)
            glEnable(GL_MULTISAMPLE);
        else
            glDisable(GL_MULTISAMPLE);

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);


        proj = new Matrix4f().ortho(0, width, height, 0, 0, 1);
        camera = new Camera();
        translation = new Matrix4f().identity();


        IntBuffer d = BufferUtils.createIntBuffer(1);
        glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS, d);
        maxTextureSlots  = d.get();
        textureLookup = new FastTextureLookup(maxTextureSlots);



        defaultGLShaderProgram = new GLShaderProgram(
                FileReader.readFile(GLRenderer2D.class.getClassLoader().getResourceAsStream("BatchVertexShader.glsl")),
                FileReader.readFile(GLRenderer2D.class.getClassLoader().getResourceAsStream("BatchFragmentShader.glsl"))
        );
        defaultGLShaderProgram.prepare();

        currentGLShaderProgram = defaultGLShaderProgram;
        currentGLShaderProgram.bind();
        updateCamera2D();

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

            System.out.println("Stride: " + vertexArray.getStride());

            vertexBuffer = new GLVertexBuffer(1000, vertexArray.getStride() / Float.BYTES);
            vertexArray.build();

            vertexData = new float[vertexBuffer.getNumOfVertices() * vertexBuffer.getVertexDataSize()];
        }

    }

    @Override
    public void setShader(ShaderProgram shaderProgram) {
        if(currentGLShaderProgram != shaderProgram) {
            currentGLShaderProgram = (GLShaderProgram) shaderProgram;
            currentGLShaderProgram.bind();
            updateCamera2D();
        }
    }

    public void updateCamera2D(){
        currentGLShaderProgram.setMatrix4f("v_model", getTranslation());
        currentGLShaderProgram.setMatrix4f("v_view", getView());
        currentGLShaderProgram.setMatrix4f("v_projection", getProj());
        currentGLShaderProgram.setIntArray("u_textures", createTextureSlots());
    }

    @Override
    public ShaderProgram getDefaultShader() {
        return defaultGLShaderProgram;
    }

    @Override
    public ShaderProgram getCurrentShaderProgram() {
        return currentGLShaderProgram;
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
    private int quadIndex;
    private int nextTextureSlot;
    public void drawTexture(float x, float y, float w, float h, Texture2D texture){
        drawTexture(x, y, w, h, texture, Color.WHITE);
    }
    public void drawRect(float x, float y, float w, float h, Color color, int thickness){

        //Left
        drawFilledRect(x - ((float) thickness / 2), y, thickness, h, color);
        //Top
        drawFilledRect(x, y - ((float) thickness / 2), w, thickness, color);
        //Bottom
        drawFilledRect(x, y - ((float) thickness / 2) + h, w, thickness, color);
        //Right
        drawFilledRect(x - ((float) thickness / 2) + w, y, thickness, h, color);

    }
    public void drawLine(float x1, float y1, float x2, float y2, Color color, int thickness, boolean round){

        float ox = originX;
        float oy = originY;

        {
            float dx = x2 - x1;
            float dy = y2 - y1;

            float angle = (float) Math.atan2(dy, dx);

            setOrigin(ox + x1, oy + y1);
            rotate(angle);

            float hypotenuse = (float) Math.sqrt((dx * dx) + (dy * dy));

            drawFilledRect(x1, y1 - (thickness / 2), hypotenuse, thickness, color);

            rotate(-angle);
            setOrigin(ox, oy);
        }

        if(round){
            drawFilledEllipse(x1 - (thickness / 2f), y1 - (thickness / 2f), thickness, thickness, color);
            drawFilledEllipse(x2 - (thickness / 2f), y2 - (thickness / 2f), thickness, thickness, color);
        }




    }
    public void drawTexture(float x, float y, float w, float h, Texture2D texture, Color color){
        drawTexture(x, y, w, h, texture, color, new Rect2D(0, 0, 1, 1), false, false);
    }
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
            render("Next Batch Render [No more rebel.engine.graphics.Texture slots out of " + maxTextureSlots + "]");

    }
    public void drawFilledRect(float x, float y, float w, float h, Color color){
        drawQuad(x, y, w, h, RECT, color, originX, originY, new Rect2D(0, 0, 1, 1), -1, false, false, 0);
    }
    public void drawFilledEllipse(float x, float y, float w, float h, Color color) {
        drawQuad(x, y, w, h, CIRCLE, color, originX, originY, new Rect2D(0, 0, 1, 1), 1, false, false, 0);
    }
    public void drawEllipse(float x, float y, float w, float h, Color color, float thickness) {
        drawQuad(x, y, w, h, CIRCLE, color, originX, originY, new Rect2D(0, 0, 1, 1), thickness, false, false, 0);
    }

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


        //TODO: Bloom



        //Translate back by origin (for rotation math)
        //This usually takes everything near (0, 0)

        Rect2D copy = new Rect2D(region.x, region.y, region.w, region.h);

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

        Vector4f topLeft = new Vector4f(x - originX, y - originY, 0, 1);
        Vector4f topRight = new Vector4f(x + w - originX, y - originY, 0, 1);
        Vector4f bottomLeft = new Vector4f(x - originX, y + h - originY, 0, 1);
        Vector4f bottomRight = new Vector4f(x + w - originX, y + h - originY, 0, 1);

        topLeft.mul(transform);
        topRight.mul(transform);
        bottomLeft.mul(transform);
        bottomRight.mul(transform);



        //Translate forward by origin back to the current position
        topLeft.x += originX;
        topRight.x += originX;
        bottomLeft.x += originX;
        bottomRight.x += originX;

        topLeft.y += originY;
        topRight.y += originY;
        bottomLeft.y += originY;
        bottomRight.y += originY;


        {
            int dataPerQuad = vertexBuffer.getVertexDataSize() * 4;


            vertexData[(quadIndex * dataPerQuad) + 0] = topLeft.x;
            vertexData[(quadIndex * dataPerQuad) + 1] = topLeft.y;
            vertexData[(quadIndex * dataPerQuad) + 2] = copy.x;
            vertexData[(quadIndex * dataPerQuad) + 3] = copy.y;
            vertexData[(quadIndex * dataPerQuad) + 4] = slot;
            vertexData[(quadIndex * dataPerQuad) + 5] = color.r;
            vertexData[(quadIndex * dataPerQuad) + 6] = color.g;
            vertexData[(quadIndex * dataPerQuad) + 7] = color.b;
            vertexData[(quadIndex * dataPerQuad) + 8] = color.a;
            vertexData[(quadIndex * dataPerQuad) + 9] = thickness;
            vertexData[(quadIndex * dataPerQuad) + 10] = bloom;


            vertexData[(quadIndex * dataPerQuad) + 11] = bottomLeft.x;
            vertexData[(quadIndex * dataPerQuad) + 12] = bottomLeft.y;
            vertexData[(quadIndex * dataPerQuad) + 13] = copy.x;
            vertexData[(quadIndex * dataPerQuad) + 14] = copy.h;
            vertexData[(quadIndex * dataPerQuad) + 15] = slot;
            vertexData[(quadIndex * dataPerQuad) + 16] = color.r;
            vertexData[(quadIndex * dataPerQuad) + 17] = color.g;
            vertexData[(quadIndex * dataPerQuad) + 18] = color.b;
            vertexData[(quadIndex * dataPerQuad) + 19] = color.a;
            vertexData[(quadIndex * dataPerQuad) + 20] = thickness;
            vertexData[(quadIndex * dataPerQuad) + 21] = bloom;


            vertexData[(quadIndex * dataPerQuad) + 22] = bottomRight.x;
            vertexData[(quadIndex * dataPerQuad) + 23] = bottomRight.y;
            vertexData[(quadIndex * dataPerQuad) + 24] = copy.w;
            vertexData[(quadIndex * dataPerQuad) + 25] = copy.h;
            vertexData[(quadIndex * dataPerQuad) + 26] = slot;
            vertexData[(quadIndex * dataPerQuad) + 27] = color.r;
            vertexData[(quadIndex * dataPerQuad) + 28] = color.g;
            vertexData[(quadIndex * dataPerQuad) + 29] = color.b;
            vertexData[(quadIndex * dataPerQuad) + 30] = color.a;
            vertexData[(quadIndex * dataPerQuad) + 31] = thickness;
            vertexData[(quadIndex * dataPerQuad) + 32] = bloom;


            vertexData[(quadIndex * dataPerQuad) + 33] = topRight.x;
            vertexData[(quadIndex * dataPerQuad) + 34] = topRight.y;
            vertexData[(quadIndex * dataPerQuad) + 35] = copy.w;
            vertexData[(quadIndex * dataPerQuad) + 36] = copy.y;
            vertexData[(quadIndex * dataPerQuad) + 37] = slot;
            vertexData[(quadIndex * dataPerQuad) + 38] = color.r;
            vertexData[(quadIndex * dataPerQuad) + 39] = color.g;
            vertexData[(quadIndex * dataPerQuad) + 40] = color.b;
            vertexData[(quadIndex * dataPerQuad) + 41] = color.a;
            vertexData[(quadIndex * dataPerQuad) + 42] = thickness;
            vertexData[(quadIndex * dataPerQuad) + 43] = bloom;

        }
        quadIndex++;


        if(quadIndex == vertexBuffer.maxQuads()) render("Next Batch Render");
    }


    public void render() {
        render("Final Draw Call [rebel.engine.graphics.Renderer2D.render()]");
        if(isDebug()){
            System.out.println("Renderer2D (" + this + ") - Debug");

            for(String call : getRenderCalls()){
                System.out.print("\t" + call + "\n");
            }
            System.out.println("\n");
        }
        renderCallNames.clear();

        if(framebuffer2D != null)
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    public void render(String renderName) {
        renderCallNames.add(renderName);


        currentGLShaderProgram.bind();
        vertexArray.bind();

        glBindBuffer(GL_ARRAY_BUFFER, getVertexBuffer().myVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexData);


        int numOfIndices = quadIndex * 6;
        int[] indices = new int[numOfIndices];
        int offset = 0;

        for (int j = 0; j < numOfIndices; j += 6) {

            indices[j] =         offset;
            indices[j + 1] = 1 + offset;
            indices[j + 2] = 2 + offset;
            indices[j + 3] = 2 + offset;
            indices[j + 4] = 3 + offset;
            indices[j + 5] =     offset;

            offset += 4;
        }

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, getVertexBuffer().myEbo);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indices);

        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);


        vertexData = new float[vertexBuffer.getNumOfVertices() * vertexBuffer.getVertexDataSize()];
        quadIndex = 0;
        nextTextureSlot = 0;
        textureLookup.clear();

    }
    public void clear(Color color) {

        if(framebuffer2D != null){
            framebuffer2D.bind();
            glClearTexImage(framebuffer2D.getTexture2D().getTexID(), 0, GL_RGBA, GL_FLOAT, new float[]{color.r, color.g, color.b, color.a});
            return;
        }

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glClearColor(color.r, color.g, color.b, color.a);
    }
    public void drawText(float x, float y, String text, Color color, Font2D font) {

        float xc = x;

        for(char c : text.toCharArray()){

            if(c == '\n'){
                y += font.getLineHeight();
                xc = x;
                continue;
            }

            GLTexture2D texture = font.getGlyphs().get((int) c);




            drawTexture(xc, y, texture.getWidth(), texture.getHeight(), texture, color);
            xc += texture.getWidth();
        }

    }
    @Override
    public void dispose() {

    }
}
