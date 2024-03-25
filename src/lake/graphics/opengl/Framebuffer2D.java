package lake.graphics.opengl;

import lake.graphics.Disposable;
import lake.graphics.Disposer;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL46.*;

public class Framebuffer2D implements Disposable {

    private int framebufferID;
    private int renderbufferID;
    private GLTexture2D texture2D;
    private int width, height;

    public Framebuffer2D(int width, int height){
        this(width, height, GL_RGBA);
    }

    public Framebuffer2D(int width, int height, int format) {
        Disposer.add("managedResources", this);
        this.width = width;
        this.height = height;
        framebufferID = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);


        texture2D = new GLTexture2D(width, height);
        texture2D.bind();


        glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, MemoryUtil.NULL);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);




        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture2D.getTexID(), 0);

        renderbufferID = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, renderbufferID);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, renderbufferID);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new RuntimeException("Incomplete Framebuffer");

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

    }

    public int getFramebufferID() {
        return framebufferID;
    }

    public void bind(){
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);
    }

    public GLTexture2D getTexture2D() {
        return texture2D;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public void dispose() {
        glDeleteFramebuffers(framebufferID);
        glDeleteRenderbuffers(renderbufferID);
    }
}
