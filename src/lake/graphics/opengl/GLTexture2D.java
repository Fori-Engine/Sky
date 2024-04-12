package lake.graphics.opengl;

import lake.graphics.Disposer;
import lake.graphics.Texture2D;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL46.*;

/***
 * Represents an OpenGL 2D Texture. This is a Disposable OpenGL object and will be disposed by the Window.
 */
public class GLTexture2D extends Texture2D {
    private int texID;
    private int slot;


    /***
     * Create a Texture from a path. This defaults to a default Linear filter
     * @param path
     */
    public GLTexture2D(String path){
        this(path, Filter.LINEAR);
    }

    /***
     * Creates a Texture from a path using the specified Filter
     * @param path
     * @param filter
     */
    public GLTexture2D(String path, Texture2D.Filter filter) {
        Disposer.add("managedResources", this);


        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer channelsInFile = BufferUtils.createIntBuffer(1);
        ByteBuffer texture = STBImage.stbi_load(path, w, h, channelsInFile, 4);
        int width = w.get();
        int height = h.get();
        setProperties(path, width, height);
        checkSTBError();

        texID = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, texID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, texture);
        glGenerateMipmap(GL_TEXTURE_2D);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter == Filter.NEAREST ? GL_NEAREST : GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter == Filter.NEAREST ? GL_NEAREST : GL_LINEAR);

        STBImage.stbi_image_free(texture);
    }

    /***
     * Creates a Texture with no uploaded data
     */
    public GLTexture2D(int width, int height) {
        setProperties(null, width, height);


        Disposer.add("managedResources", this);
        texID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texID);
    }

    /***
     * Uploads a ByteBuffer to the Texture
     * @param data
     * @param width
     * @param height
     */
    @Override
    public void setData(ByteBuffer data, int width, int height) {
        setWidth(width);
        setHeight(height);

        glBindTexture(GL_TEXTURE_2D, texID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        glGenerateMipmap(GL_TEXTURE_2D);
    }




    public void bind(){
        glBindTexture(GL_TEXTURE_2D, texID);
    }

    public int getTexID() {
        return texID;
    }


    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    @Override
    public void dispose() {
        glDeleteTextures(texID);
    }
}
