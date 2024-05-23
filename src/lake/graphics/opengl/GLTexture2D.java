package lake.graphics.opengl;

import lake.asset.Asset;
import lake.asset.TextureData;
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

    public GLTexture2D(int width, int height){
        super(width, height);
        texID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texID);
        setTexParameters();
    }

    public GLTexture2D(Asset<TextureData> textureData, Filter filter){
        super(textureData, filter);

        texID = glGenTextures();

        ByteBuffer buffer = BufferUtils.createByteBuffer(textureData.asset.data.length);
        buffer.put(textureData.asset.data);
        buffer.flip();

        glBindTexture(GL_TEXTURE_2D, texID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glGenerateMipmap(GL_TEXTURE_2D);

        setTexParameters();
    }

    public void setTexParameters(){
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter == Filter.Nearest ? GL_NEAREST : GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter == Filter.Nearest ? GL_NEAREST : GL_LINEAR);
    }





    @Override
    public void setData(byte[] data) {

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
