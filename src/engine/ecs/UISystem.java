package engine.ecs;

import engine.Input;
import engine.Surface;
import static engine.gameui.TextValue.*;

import engine.SystemState;
import engine.asset.AssetRegistry;
import engine.gameui.*;
import engine.graphics.*;
import engine.graphics.pipelines.ScreenSpaceFeatures;
import engine.graphics.text.*;
import org.joml.Matrix2f;
import org.joml.Vector2f;
import java.nio.ByteBuffer;

public class UISystem extends ActorSystem {

    private Renderer renderer;
    private RenderPipeline renderPipeline;
    private Scene scene;
    private int quadCount = 0;
    private ByteBuffer vertexBufferData, indexBufferData;
    private Matrix2f transform = new Matrix2f();
    private Vector2f origin = new Vector2f();
    private MsdfFont msdfFont;
    private Surface surface;
    private Loop menuLoop;
    private GfxPlatform gfxPlatform;
    private Theme theme;
    private TextureBindings textureBindings;



    public UISystem(Renderer renderer, RenderPipeline renderPipeline, Surface surface, Scene scene) {
        this.renderer = renderer;
        this.renderPipeline = renderPipeline;
        this.surface = surface;
        this.scene = scene;
        surface.setCaptureMouse(SystemState.running);

        surface.addKeyCallback(key -> {
            if(key == Input.KEY_ESCAPE) SystemState.running = !SystemState.running;
        });

        msdfFont = new MsdfFont(
                renderer,
                AssetRegistry.getAsset("core:assets/fonts/Roboto/roboto-atlas.png"),
                AssetRegistry.getAsset("core:assets/fonts/Roboto/roboto-atlas.json")
        );

        theme = ThemeLoader.loadTheme((String) AssetRegistry.getAsset("core:assets/themes/DarkMode.json").getObject());


        textureBindings = new TextureBindings();

        //Menu UI
        {
            menuLoop = new Loop();
            menuLoop.setWidget(
                    new ContainerWidget().setLayoutEngine(new EdgeLayoutEngine())
                            .addWidgets(new ContainerWidget().setLayoutEngine(new LineLayoutEngine(LineLayoutEngine.Line.Vertical)).addHint(EdgeLayoutEngine.Top)
                                    .addWidgets(
                                            new Text(text("\"Lorem ipsum dolor sit amet, consectetur adipiscing elit,\nsed do eiusmod tempor incididunt ut labore et dolore magna aliqua. "), msdfFont),
                                            new Text(text("Text 2"), msdfFont),
                                            new Text(text("Text 3"), msdfFont),
                                            new Text(text("Text 4"), msdfFont),
                                            new Button(text("Resume"), msdfFont)
                                                    .addEventHandler(new EventHandler() {
                                                        @Override
                                                        public void onClick() {
                                                            SystemState.running = true;
                                                        }
                                                    })
                                    ))
            );
        }



        //Gfx Platform
        {
            gfxPlatform = new GfxPlatform() {
                @Override
                public int getMouseX() {
                    return (int) surface.getMousePos().x;
                }

                @Override
                public int getMouseY() {
                    return (int) surface.getMousePos().y;
                }

                @Override
                public boolean isMousePressed(int mouseButton) {
                    return surface.getMousePressed(mouseButton);
                }

                @Override
                public void drawRect(float x, float y, float w, float h, Color color) {
                    UISystem.this.drawQuad(
                            x,
                            y,
                            w,
                            h,
                            -1, -1,
                            -1, -1,
                            -1, -1,
                            -1, -1,
                            -1,
                            -1,
                            -1,
                            color
                    );
                }


                public void drawTexture(float x,
                                      float y,
                                      float w,
                                      float h,

                                      float uvtlx,
                                      float uvtly,

                                      float uvblx,
                                      float uvbly,

                                      float uvtrx,
                                      float uvtry,

                                      float uvbrx,
                                      float uvbry,
                                      float op1,
                                      Color color,
                                      Texture texture,
                                      Sampler sampler, boolean msdf) {

                    int index = textureBindings.getTextureBinding(texture);

                    ScreenSpaceFeatures screenSpaceFeatures = renderPipeline.getFeatures(ScreenSpaceFeatures.class);
                    screenSpaceFeatures.getShaderProgram().setTextures(renderer.getFrameIndex(), new DescriptorUpdate<>("input_textures", texture).arrayIndex(index));
                    screenSpaceFeatures.getShaderProgram().setSamplers(renderer.getFrameIndex(), new DescriptorUpdate<>("input_samplers", sampler).arrayIndex(index));


                    drawQuad(
                            x,
                            y,
                            w,
                            h,
                            uvtlx,
                            uvtly,
                            uvblx,
                            uvbly,
                            uvtrx,
                            uvtry,
                            uvbrx,
                            uvbry,
                            msdf ? -3 : 0,
                            index,
                            op1,
                            color
                    );
                }

                @Override
                public void drawTexture(float x, float y, float w, float h, Color color, Texture texture, Sampler sampler) {
                    drawTexture(x, y, w, h, 0, 0,
                            0, 1,
                            1, 0,
                            1, 1,
                            -1,
                            color,
                            texture,
                            sampler,
                            false
                    );
                }

                private void drawGlyph(float x, float y, float w, float h, MsdfFont msdfFont, MsdfJsonLoader.Character character, Color color) {

                    MsdfJsonLoader.MsdfData msdfData = msdfFont.getMSDFData();
                    int msdfScreenPxRange = (int) ((32 / msdfData.width) * msdfData.size);

                    drawTexture(
                            x,
                            y,
                            w,
                            h,
                            character.atlasBounds.left / msdfData.width,
                            1 - character.atlasBounds.top / msdfData.height,
                            character.atlasBounds.left / msdfData.width,
                            1 - character.atlasBounds.bottom / msdfData.height,
                            character.atlasBounds.right / msdfData.width,
                            1 - character.atlasBounds.top / msdfData.height,
                            character.atlasBounds.right / msdfData.width,
                            1 - character.atlasBounds.bottom / msdfData.height,
                            msdfScreenPxRange,
                            color,
                            msdfFont.getTexture(),
                            msdfFont.getSampler(),
                            true
                    );
                }

                @Override
                public void drawString(float x, float y, String text, MsdfFont font, TextEffect textEffect, Color color) {
                    if(textEffect != null)
                        textEffect.update();

                    float xl = 0;
                    float yl = y + (msdfFont.getMSDFData().lineHeight + msdfFont.getMSDFData().descender) * msdfFont.getMSDFData().size;
                    float spaceXAdvance = msdfFont.getMSDFData().characters[' '].advance;

                    for (int i = 0; i < text.length(); i++) {
                        char c = text.charAt(i);

                        if(c == '\n') {
                            yl += msdfFont.getMSDFData().lineHeight * msdfFont.getMSDFData().size;
                            xl = 0;
                            continue;
                        }
                        if(c == '\t') {
                            xl = msdfFont.getTabWidth() * spaceXAdvance;
                            continue;
                        }

                        MsdfJsonLoader.Character character = msdfFont.getMSDFData().characters[c];
                        if(character == null) character = msdfFont.getMSDFData().characters['?'];

                        MsdfJsonLoader.Rect planeBounds = character.planeBounds;
                        if(planeBounds != null) {

                            float sw = (planeBounds.right - planeBounds.left) * msdfFont.getMSDFData().size;
                            float sh = (planeBounds.top - planeBounds.bottom) * msdfFont.getMSDFData().size;
                            float yo = planeBounds.bottom * msdfFont.getMSDFData().size;

                            float effectOffsetX = 0, effectOffsetY = 0;

                            if(textEffect != null) {
                                Vector2f effectOffset = textEffect.offset(i, text.length());
                                effectOffsetX = effectOffset.x;
                                effectOffsetY = effectOffset.y;
                            }

                            drawGlyph(
                                    x + xl + effectOffsetX,
                                    yl - sh - yo + effectOffsetY,
                                    sw,
                                    sh,
                                    msdfFont,
                                    character,
                                    color
                            );
                        }

                        xl += character.advance * msdfFont.getMSDFData().size;
                    }


                }

                @Override
                public Theme getTheme() {
                    return theme;
                }
            };
        }
        menuLoop.setGfxPlatform(gfxPlatform);
    }

    @Override
    public void run(Actor root) {
        ScreenSpaceFeatures screenSpaceFeatures = renderPipeline.getFeatures(ScreenSpaceFeatures.class);
        vertexBufferData = screenSpaceFeatures.getVertexBuffers()[renderer.getFrameIndex()].get();
        vertexBufferData.clear();
        indexBufferData = screenSpaceFeatures.getIndexBuffers()[renderer.getFrameIndex()].get();
        indexBufferData.clear();

        surface.setCaptureMouse(SystemState.running);

        transform = new Matrix2f();
        setOrigin(0, 0);

        drawQuad(
                0,
                0,
                renderer.getWidth(),
                renderer.getHeight(),
                0, 0,
                0, 1,
                1, 0,
                1, 1,
                0,
                0,
                -1,
                Color.WHITE
        );




        if(SystemState.running) {
            root.previsitAllActors(actor -> {
                if(actor.has(UIComponent.class)) {
                    UIComponent uiComponent = actor.getComponent(UIComponent.class);
                    if (!uiComponent.active) {

                        uiComponent.loop = new Loop();
                        uiComponent.loop.setWidget(uiComponent.widget);
                        uiComponent.loop.setGfxPlatform(gfxPlatform);

                        uiComponent.active = true;
                    }

                    if (uiComponent.rect2D.isPresent()) {
                        Rect2D rect2D = uiComponent.rect2D.get();
                        uiComponent.loop.update(
                                (int) rect2D.x,
                                (int) rect2D.y,
                                (int) rect2D.w,
                                (int) rect2D.h
                        );
                    } else {
                        uiComponent.loop.update(
                                0, 0
                        );
                    }
                }



            });

        }
        else {
            menuLoop.update((renderer.getWidth() / 2) - (renderer.getWidth() / 4), (renderer.getHeight() / 2) - (renderer.getHeight() / 4), renderer.getWidth() / 2, renderer.getHeight() / 2);
        }



        screenSpaceFeatures.setIndexCount(6 * quadCount);
        quadCount = 0;
    }


    private void setOrigin(float x, float y) {
        origin.set(x, y);
    }


    private void drawQuad(float x,
                          float y,
                          float w,
                          float h,

                          float uvtlx,
                          float uvtly,

                          float uvblx,
                          float uvbly,

                          float uvtrx,
                          float uvtry,

                          float uvbrx,
                          float uvbry,

                          int shapeMode,
                          int op0,
                          float op1,
                          Color color) {

        Vector2f
                topLeft = new Vector2f(x, y).sub(origin).mul(transform).add(origin),
                bottomLeft = new Vector2f(x, y + h).sub(origin).mul(transform).add(origin),
                bottomRight = new Vector2f(x + w, y + h).sub(origin).mul(transform).add(origin),
                topRight = new Vector2f(x + w, y).sub(origin).mul(transform).add(origin);

        vertexBufferData.putFloat(topLeft.x);
        vertexBufferData.putFloat(topLeft.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvtlx);
        vertexBufferData.putFloat(uvtly);
        vertexBufferData.putFloat(shapeMode);
        vertexBufferData.putFloat(op0);
        vertexBufferData.putFloat(op1);

        vertexBufferData.putFloat(bottomLeft.x);
        vertexBufferData.putFloat(bottomLeft.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvblx);
        vertexBufferData.putFloat(uvbly);
        vertexBufferData.putFloat(shapeMode);
        vertexBufferData.putFloat(op0);
        vertexBufferData.putFloat(op1);

        vertexBufferData.putFloat(bottomRight.x);
        vertexBufferData.putFloat(bottomRight.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvbrx);
        vertexBufferData.putFloat(uvbry);
        vertexBufferData.putFloat(shapeMode);
        vertexBufferData.putFloat(op0);
        vertexBufferData.putFloat(op1);

        vertexBufferData.putFloat(topRight.x);
        vertexBufferData.putFloat(topRight.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvtrx);
        vertexBufferData.putFloat(uvtry);
        vertexBufferData.putFloat(shapeMode);
        vertexBufferData.putFloat(op0);
        vertexBufferData.putFloat(op1);

        indexBufferData.putInt(0 + (4 * quadCount));
        indexBufferData.putInt(1 + (4 * quadCount));
        indexBufferData.putInt(2 + (4 * quadCount));
        indexBufferData.putInt(2 + (4 * quadCount));
        indexBufferData.putInt(3 + (4 * quadCount));
        indexBufferData.putInt(0 + (4 * quadCount));
        quadCount++;
    }

    @Override
    public void dispose() {

    }
}
