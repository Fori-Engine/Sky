package lake.demo;

import lake.Animation;
import lake.FlightRecorder;
import lake.Time;
import lake.graphics.*;
import lake.particles.Particle;
import lake.particles.ParticleSource;
import lake.particles.ParticleSourceConfig;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class ShowcaseDemo {
    public static void main(String[] args) {

        FlightRecorder.setEnabled(true);

        StandaloneWindow window = new StandaloneWindow(1920, 1080, "Showcase Demo", false, false);
        Renderer2D renderer2D = Renderer2D.createRenderer(window, window.getWidth(), window.getHeight(), new RenderSettings(RendererBackend.Vulkan).msaa(true));

        window.setIcon("assets/logo.png");
        window.setTitle("LakeEngine Demo [" + Renderer2D.getRenderBackend() + "]");


        ArrayList<Texture2D> textures = new ArrayList<>();
        Texture2D logo = Texture2D.newTexture("assets/logo.png");
        Texture2D opengl = Texture2D.newTexture(Renderer2D.getRenderBackend() == RendererBackend.OpenGL ? "demo_assets/opengl.png" : "demo_assets/vulkan.png");


        for (int i = 0; i < 32; i++) {
            textures.add(i % 2 == 0 ? logo : opengl);
        }


        CubicBezierCurve2D cubicBezierCurve = new CubicBezierCurve2D(
                new Vector2f(200, 800),
                new Vector2f(600, 400),
                new Vector2f(900, 1400),
                new Vector2f(1500, 800)
        );
        List<Line2D> line2DS = cubicBezierCurve.calculate(15);
        float rotation = 0f;


        ParticleSourceConfig particleSourceConfig = new ParticleSourceConfig(100, 100, 3500, 7, 5, 10);
        ParticleSource particleSource = new ParticleSource(particleSourceConfig, new Vector2f(700f, 700f));
        particleSource.addParticles(200);

        Animation animation = new Animation(Texture2D.newTexture("demo_assets/sprites.png", Texture2D.Filter.NEAREST));
        animation.create(2, 2, 4);
        animation.setDelay(150);
        animation.setPlaymode(Animation.Playmode.PLAY_REPEAT);


        while (!window.shouldClose()) {
            renderer2D.clear(new Color(0.5f, 0.5f, 0.5f, 1.0f));


            renderer2D.drawText(0, 0, "FPS: " + window.getFPS(), Color.RED, Font2D.getDefault());
            renderer2D.drawText(window.getMouseX(), window.getMouseY(), " \"Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. \nUt enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. \nDuis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. \nExcepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\" FPS: " + window.getFPS(), Color.BLUE, Font2D.getDefault());

            
            


            {
                renderer2D.drawRect(1000, 700, 300, 300, Color.GREEN, 3);
                renderer2D.drawFilledRect(1300, 700, 100, 100, Color.RED);

                for(Line2D line2D : line2DS){
                    renderer2D.drawLine(line2D.start.x, line2D.start.y, line2D.end.x, line2D.end.y, Color.WHITE, 40, true);
                }

                renderer2D.drawFilledEllipse(1400, 700, 200, 200, Color.GREEN);
                renderer2D.drawEllipse(1600, 700, 200, 200, Color.GREEN, 0.5f);
                particleSource.update();

                for(Particle particle : particleSource.getParticles()){
                    renderer2D.drawTexture(particle.rect2D.x, particle.rect2D.y, particle.rect2D.w, particle.rect2D.h, logo);
                }


            }

            {
                int tx = 0, ty = 0;
                for (int i = 0; i < textures.size(); i++) {
                    if ((tx / 100) % 8 == 0) {
                        ty += 100;
                        tx = 0;
                    }

                    renderer2D.drawTexture(tx, ty, 100, 100, textures.get(i));
                    tx += 100;
                }



                animation.update(Time.deltaTime);

                renderer2D.drawTexture(0, renderer2D.getHeight() - 512, 512, 512, animation.getTexture(), Color.WHITE, animation.getCurrentFrame(), false, false);


            }


            {
                renderer2D.setOrigin(1000 + 250 / 2f, 300 + 250 / 2f);
                renderer2D.scale(2f, 2f, 1f);
                renderer2D.rotate((float) Math.toRadians(rotation));
                renderer2D.drawTexture(1000, 300, 250, 250, logo);
                renderer2D.resetTransform();
                renderer2D.setOrigin(0, 0);

                rotation += 5 * Time.deltaTime;
            }






            renderer2D.render();
            window.update();
        }


        window.close();
    }

}
