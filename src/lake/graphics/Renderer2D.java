package lake.graphics;

import lake.FlightRecorder;
import lake.graphics.opengl.GLRenderer2D;
import lake.graphics.vulkan.LVKRenderer2D;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Renderer2D implements Disposable {

    private int width;
    private int height;
    protected Matrix4f proj;
    protected Camera camera;
    protected Matrix4f translation;
    private ArrayList<String> renderCallNames = new ArrayList<>(50);
    private boolean debug = false;

    private static RendererBackend backend;

    protected Matrix4f transform = new Matrix4f().identity();
    protected float originX, originY;
    private static float spaceXAdvance = 0;
    private static final int spacesPerTab = 4;
    protected int quadIndex;
    protected float[] vertexData;

    public Renderer2D(int width, int height, RenderSettings renderSettings){
        this.width = width;
        this.height = height;
        Disposer.add("renderer", this);
    }

    public abstract void setShaderProgram(ShaderProgram shaderProgram);
    public abstract void updateCamera2D();
    public abstract ShaderProgram getDefaultShaderProgram();
    public abstract ShaderProgram getCurrentShaderProgram();

    protected int[] generateIndices(int quadIndex){
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

        return indices;
    }

    public RenderData applyTransformations(float x,
                                           float y,
                                           float w,
                                           float h,
                                           float originX,
                                           float originY,
                                           Rect2D region,
                                           boolean xFlip,
                                           boolean yFlip){
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

        return new RenderData(new Vector4f[]{topLeft, topRight, bottomLeft, bottomRight}, copy);
    }

    protected class RenderData {
        public Vector4f[] transformedPoints;
        public Rect2D copy;

        public RenderData(Vector4f[] transformedPoints, Rect2D copy) {
            this.transformedPoints = transformedPoints;
            this.copy = copy;
        }
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
    public abstract void clear(Color color);
    public float getOriginX() {
        return originX;
    }
    public float getOriginY() {
        return originY;
    }

    public void drawText(float x, float y, String text, Color color, Font2D font) {

        Texture2D glyphTexture = font.getTexture();
        Map<Integer, Glyph> glyphs = font.getGlyphs();
        float xc = x;

        String line = "";

        spaceXAdvance = glyphs.get((int) ' ').getXAdvance();


        for(char c : text.toCharArray()){

            if(c == '\t'){
                xc += spaceXAdvance * spacesPerTab;
                continue;
            }

            if(c == '\r'){
                xc = x;
                continue;
            }

            Glyph glyph = glyphs.get((int) c);

            if(c == '\n'){

                float height = font.getLineHeight(line);

                y += height;


                line = "";
                xc = x;
                continue;
            }


            float xt = glyph.getX();
            float yt = glyph.getY();

            float texX = xt / glyphTexture.getWidth();
            float texY = yt / glyphTexture.getHeight();

            float texW = (xt + glyph.getW()) / glyphTexture.getWidth();
            float texH = (yt + glyph.getH()) / glyphTexture.getHeight();

            drawTexture(xc + glyph.getxOffset(), y + (glyph.getyOffset()), glyph.getW(), glyph.getH(), glyphTexture, color, new Rect2D(texX, texY, texW, texH), false, false);


            xc += glyph.getXAdvance();

            line += c;
        }

    }

    public abstract String getDeviceName();

    public static Renderer2D createRenderer(StandaloneWindow window, int width, int height, RenderSettings settings){
        backend = settings.backend;
        FlightRecorder.info(Renderer2D.class, "Using renderer backend " + backend);

        if(settings.backend == RendererBackend.OpenGL){
            return new GLRenderer2D(width, height, settings);
        }
        else if(settings.backend == RendererBackend.Vulkan){
            return new LVKRenderer2D(window, width, height, settings);
        }
        else if(settings.backend == null){
            FlightRecorder.meltdown(Renderer2D.class, "The target graphics API was not specified in RenderSettings!");
        }
        else {
            FlightRecorder.meltdown(Renderer2D.class, "User requested renderer backend " + settings.backend + " but no backend could be initialized!");
        }



        return null;
    }

    public static RendererBackend getRenderBackend() {
        return backend;
    }
}
