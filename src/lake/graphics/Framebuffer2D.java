package lake.graphics;

import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.opengl.GL46.*;

public class Framebuffer2D implements Disposable {

    private int framebufferID;
    private int renderbufferID;
    private Texture2D texture2D;

    public Framebuffer2D(int width, int height) {
        Disposer.add(this);
        framebufferID = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);


        texture2D = new Texture2D(width, height);
        texture2D.bind();


        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, MemoryUtil.NULL);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);




        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture2D.getTexID(), 0);

        renderbufferID = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, renderbufferID);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height); // use a single renderbuffer object for both a depth AND stencil buffer.
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, renderbufferID); // now actually attach it
        // now that we actually created the framebuffer and added all attachments we want to check if it is actually complete now
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new RuntimeException("Incomplete Framebuffer");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);





    }

    public void bind(){
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferID);
    }

    public Texture2D getTexture2D() {
        return texture2D;
    }

    @Override
    public void dispose() {
        glDeleteFramebuffers(framebufferID);
        glDeleteRenderbuffers(renderbufferID);
    }
}
