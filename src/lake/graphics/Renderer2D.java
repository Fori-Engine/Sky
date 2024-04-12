package lake.graphics;

import lake.FlightRecorder;
import lake.graphics.opengl.*;
import lake.graphics.vulkan.LVKRenderer2D;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL44.glClearTexImage;

public abstract class Renderer2D {

    private int width;
    private int height;
    protected Matrix4f proj;
    protected Camera camera;
    protected Matrix4f translation;
    private ArrayList<String> renderCallNames = new ArrayList<>(50);
    private boolean debug = false;
    private boolean msaa;

    private static RendererType api;

    protected Matrix4f transform = new Matrix4f().identity();
    protected float originX, originY;

    public Renderer2D(int width, int height, boolean msaa){
        this.width = width;
        this.height = height;
        this.msaa = msaa;
    }

    public abstract void setShader(ShaderProgram shaderProgram);
    public abstract void updateCamera2D();
    public abstract ShaderProgram getDefaultShader();
    public abstract ShaderProgram getCurrentShaderProgram();
    public int getWidth() {
        return width;
    }
    public int getHeight() {
        return height;
    }
    public Matrix4f getProj() {
        return proj;
    }
    public Camera getCamera2D() {
        return camera;
    }
    public Matrix4f getView() {
        return camera.getViewMatrix();
    }
    public Matrix4f getTranslation() {
        return translation;
    }
    public Matrix4f getTransform() {
        return transform;
    }
    public void setTransform(Matrix4f transform) {
        this.transform = transform;
    }
    public void rotate(float radians){
        setTransform(getTransform().mul(new Matrix4f().rotate(radians, 0, 0, 1)));
    }
    public void scale(float x, float y, float z){
        setTransform(getTransform().mul(new Matrix4f().scale(x, y, z)));
    }
    public void resetTransform(){
        setTransform(new Matrix4f().identity());
    }
    public abstract void drawTexture(float x, float y, float w, float h, Texture2D texture);
    public void setOrigin(float x, float y){
        this.originX = x;
        this.originY = y;
    }
    public void setOrigin(Vector2f vector2f){
        this.originX = vector2f.x;
        this.originY = vector2f.y;
    }
    public abstract void drawRect(float x, float y, float w, float h, Color color, int thickness);
    public abstract void drawLine(float x1, float y1, float x2, float y2, Color color, int thickness, boolean round);
    public abstract void drawTexture(float x, float y, float w, float h, Texture2D texture, Color color);
    public abstract void drawTexture(float x, float y, float w, float h, Texture2D texture, Color color, Rect2D rect2D, boolean xFlip, boolean yFlip);
    public abstract void drawFilledRect(float x, float y, float w, float h, Color color);
    public abstract void drawFilledEllipse(float x, float y, float w, float h, Color color);
    public abstract void drawEllipse(float x, float y, float w, float h, Color color, float thickness);
    public abstract void render();
    public abstract void render(String renderName);
    public List<String> getRenderCalls(){
        return new ArrayList<>(renderCallNames);
    }
    public boolean isDebug() {
        return debug;
    }
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    public abstract void clear(Color color);
    public float getOriginX() {
        return originX;
    }
    public float getOriginY() {
        return originY;
    }
    public abstract void drawText(float x, float y, String text, Color color, Font2D font);

    public static Renderer2D createRenderer(RendererType type, StandaloneWindow window, int width, int height, boolean msaa){
        api = type;
        FlightRecorder.info(Renderer2D.class, "Using renderer backend " + api);

        if(type == RendererType.OPENGL){
            return new GLRenderer2D(width, height, msaa);
        }
        else if(type == RendererType.VULKAN){
            return new LVKRenderer2D(window, width, height, msaa);
        }

        FlightRecorder.meltdown(Renderer2D.class, "User requested renderer backend " + type + " but no backend could be initialized!");
        return null;
    }

    public static RendererType getAPI() {
        return api;
    }
}
