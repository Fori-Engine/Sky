package engine.ecs;

import engine.Input;
import engine.Surface;
import static engine.gameui.TextValue.*;

import engine.SystemState;
import engine.Time;
import engine.asset.AssetRegistry;
import engine.gameui.*;
import engine.graphics.*;
import engine.graphics.pipelines.ScreenSpaceFeatures;
import engine.graphics.text.*;
import game.Colors;
import game.Settings;
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
                AssetRegistry.getAsset("core:assets/fonts/AirbusB612/b612-atlas.png"),
                AssetRegistry.getAsset("core:assets/fonts/AirbusB612/b612-atlas.json")
        );

        theme = ThemeLoader.loadTheme((String) AssetRegistry.getAsset("core:assets/themes/CozyRoom.json").getObject());



        //Menu UI
        {
            menuLoop = new Loop();
            menuLoop.setWidget(
                    new ContainerWidget().setLayoutEngine(new EdgeLayoutEngine())
                            .addWidget(new ContainerWidget()
                                    .setLayoutEngine(new LineLayoutEngine(LineLayoutEngine.Line.Horizontal))
                                    .addWidgets(
                                            new Text(text("Welcome to the menu!"), msdfFont),
                                            new Button(text("Quit"), msdfFont)
                                                    .addEventHandler(new EventHandler() {

                                                        @Override
                                                        public void onClick() {
                                                            System.exit(0);
                                                        }
                                                    }),
                                            new Button(text("Resume"), msdfFont)
                                                    .addEventHandler(new EventHandler() {
                                                        @Override
                                                        public void onClick() {
                                                            SystemState.running = true;
                                                        }
                                                    })
                                    ).addHint(EdgeLayoutEngine.Bottom))
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

                @Override
                public void drawString(float x, float y, String text, MsdfFont font, Color color) {
                    UISystem.this.drawString(x, y, text, font, null, color);
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
        screenSpaceFeatures.getShaderProgram().setTextures(renderer.getFrameIndex(), new DescriptorUpdate<>("input_textures", msdfFont.getTexture()).arrayIndex(1));
        screenSpaceFeatures.getShaderProgram().setSamplers(renderer.getFrameIndex(), new DescriptorUpdate<>("input_samplers", msdfFont.getSampler()).arrayIndex(1));

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

        //Crosshair
        {



            drawQuad(
                    ((float) renderer.getWidth() / 2) - 15,
                    ((float) renderer.getHeight() / 2) - 2,
                    30,
                    4,
                    -1, -1,
                    -1, -1,
                    -1, -1,
                    -1, -1,
                    -1,
                    -1,
                    -1,
                    Color.GRAY
            );

            drawQuad(
                    ((float) renderer.getWidth() / 2) - 2,
                    ((float) renderer.getHeight() / 2) - 15,
                    4,
                    30,
                    -1, -1,
                    -1, -1,
                    -1, -1,
                    -1, -1,
                    -1,
                    -1,
                    -1,
                    Color.GRAY
            );
        }


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
            drawQuad(
                    0,
                    0,
                    renderer.getWidth(),
                    renderer.getHeight(),
                    -1, -1,
                    -1, -1,
                    -1, -1,
                    -1, -1,
                    -1,
                    -1,
                    -1,
                    Colors.menuBackground
            );
            menuLoop.update((renderer.getWidth() / 2) - (renderer.getWidth() / 4), (renderer.getHeight() / 2) - (renderer.getHeight() / 4), renderer.getWidth() / 2, renderer.getHeight() / 2);
        }



        screenSpaceFeatures.setIndexCount(6 * quadCount);
        quadCount = 0;
    }

    private void drawString(float x, float y, String text, MsdfFont msdfFont, TextEffect textEffect, Color color) {
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
                        1,
                        msdfFont.getMSDFData(),
                        character,
                        color
                );
            }
            float left = character.planeBounds != null ? character.planeBounds.left : 0;
            float right = character.planeBounds != null ? character.planeBounds.right : 0;

            xl += character.advance * msdfFont.getMSDFData().size;
        }


    }


    private void setOrigin(float x, float y) {
        origin.set(x, y);
    }

    private void drawGlyph(float x, float y, float w, float h, int msdfTextureIndex, MsdfJsonLoader.MsdfData msdfData, MsdfJsonLoader.Character character, Color color) {

        int msdfScreenPxRange = (int) Math.ceil((w / msdfData.width) * msdfData.size);

        drawQuad(
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
                -3,
                msdfTextureIndex,
                msdfScreenPxRange,
                color
        );
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

                          int fillMode,
                          int textureIndex,
                          float msdfScreenPxRange,
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
        vertexBufferData.putFloat(fillMode);
        vertexBufferData.putFloat(textureIndex);
        vertexBufferData.putFloat(msdfScreenPxRange);

        vertexBufferData.putFloat(bottomLeft.x);
        vertexBufferData.putFloat(bottomLeft.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvblx);
        vertexBufferData.putFloat(uvbly);
        vertexBufferData.putFloat(fillMode);
        vertexBufferData.putFloat(textureIndex);
        vertexBufferData.putFloat(msdfScreenPxRange);

        vertexBufferData.putFloat(bottomRight.x);
        vertexBufferData.putFloat(bottomRight.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvbrx);
        vertexBufferData.putFloat(uvbry);
        vertexBufferData.putFloat(fillMode);
        vertexBufferData.putFloat(textureIndex);
        vertexBufferData.putFloat(msdfScreenPxRange);

        vertexBufferData.putFloat(topRight.x);
        vertexBufferData.putFloat(topRight.y);
        vertexBufferData.putFloat(color.r);
        vertexBufferData.putFloat(color.g);
        vertexBufferData.putFloat(color.b);
        vertexBufferData.putFloat(color.a);
        vertexBufferData.putFloat(uvtrx);
        vertexBufferData.putFloat(uvtry);
        vertexBufferData.putFloat(fillMode);
        vertexBufferData.putFloat(textureIndex);
        vertexBufferData.putFloat(msdfScreenPxRange);

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
