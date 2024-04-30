package lake.graphics;

import lake.graphics.opengl.GLTexture2D;
import lake.graphics.vulkan.LVKTexture2D;
import org.lwjgl.stb.STBImage;

import java.awt.*;
import java.awt.Color;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;

public abstract class Texture2D implements Disposable {
    private String path;
    private int width, height;
    public enum Filter {
        LINEAR,
        NEAREST
    }


    public void setProperties(String path, int width, int height) {
        if(path != null)
            if(!new File(path).exists()) throw new RuntimeException("The file " + path + " does not exist!");

        this.path = path;
        this.width = width;
        this.height = height;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    /***
     * Uploads a BufferedImage to the Texture. For compatibility with Java2D
     * @param bufferedImage
     */
    public void setData(BufferedImage bufferedImage){
        setData(toByteBuffer(bufferedImage), bufferedImage.getWidth(), bufferedImage.getHeight());
    }

    public abstract void setData(ByteBuffer data, int width, int height);



        //COPIED! From https://stackoverflow.com/questions/5194325/how-do-i-load-an-image-for-use-as-an-opengl-texture-with-lwjgl
    //TODO: Replace this!
    /**
     * Convert the buffered image to a texture
     */
    private ByteBuffer toByteBuffer(BufferedImage bufferedImage) {
        ByteBuffer imageBuffer;
        WritableRaster raster;
        BufferedImage texImage;

        ColorModel glAlphaColorModel = new ComponentColorModel(ColorSpace
                .getInstance(ColorSpace.CS_sRGB), new int[] { 8, 8, 8, 8 },
                true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);

        raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE,
                bufferedImage.getWidth(), bufferedImage.getHeight(), 4, null);
        texImage = new BufferedImage(glAlphaColorModel, raster, true,
                new Hashtable());

        // copy the source image into the produced image
        Graphics g = texImage.getGraphics();
        g.setColor(new Color(0f, 0f, 0f, 0f));
        g.fillRect(0, 0, 256, 256);
        g.drawImage(bufferedImage, 0, 0, null);

        // build a byte buffer from the temporary image
        // that be used by OpenGL to produce a texture.
        byte[] data = ((DataBufferByte) texImage.getRaster().getDataBuffer())
                .getData();

        imageBuffer = ByteBuffer.allocateDirect(data.length);
        imageBuffer.order(ByteOrder.nativeOrder());
        imageBuffer.put(data, 0, data.length);
        imageBuffer.flip();

        return imageBuffer;
    }

    public static Texture2D newTexture(String path){
        if(Renderer2D.getRenderBackend() == RendererBackend.OpenGL) return new GLTexture2D(path);
        if(Renderer2D.getRenderBackend() == RendererBackend.Vulkan) return new LVKTexture2D(path);

        return null;
    }

    public static Texture2D newTexture(String path, Filter filter){
        if(Renderer2D.getRenderBackend() == RendererBackend.OpenGL) return new GLTexture2D(path, filter);
        if(Renderer2D.getRenderBackend() == RendererBackend.Vulkan) return new LVKTexture2D(path, filter);

        return null;
    }

    protected void checkSTBError(){
        String message = STBImage.stbi_failure_reason();

        if(message != null){


            throw new RuntimeException(path + " " + message);
        }
    }

    public static Texture2D newTexture(int width, int height){
        if(Renderer2D.getRenderBackend() == RendererBackend.OpenGL) return new GLTexture2D(width, height);
        if(Renderer2D.getRenderBackend() == RendererBackend.Vulkan) return new LVKTexture2D(width, height);

        return null;
    }
}
