package lake.demo;

import lake.FileReader;
import lake.FlightRecorder;
import lake.Time;
import lake.asset.AssetPack;
import lake.asset.AssetPacks;
import lake.asset.TextureData;
import lake.graphics.*;
import lake.physics.CircleBody2D;
import lake.physics.RectBody2D;
import lake.physics.RigidBody2D;
import lake.physics.World;
import org.joml.Vector2f;

import java.io.File;
import java.util.ArrayList;

public class PlatformerDemo {


    public static void main(String[] args) throws InterruptedException {
        AssetPacks.open("core", AssetPack.openLocal(new File("assets")));

        FlightRecorder.setEnabled(true);
        FlightRecorder.meltdown(PlatformerDemo.class, "This is very very bad");
        FlightRecorder.error(PlatformerDemo.class, "This is very bad");
        FlightRecorder.info(PlatformerDemo.class, "This isn't bad");
        FlightRecorder.todo(PlatformerDemo.class, "I'm just being lazy");



        Window window = new Window(1920, 1080, "Showcase Demo", false);



        Renderer2D renderer2D = Renderer2D.newRenderer2D(window, window.getWidth(), window.getHeight(), new RenderSettings(RenderAPI.Vulkan).msaa(true));

        window.setIcon(AssetPacks.getAsset("core:assets/logo.png"));

        World world = new World(0.1f, new Vector2f(0,9.8f * 12));


        ArrayList<RigidBody2D> bodies = new ArrayList<>();
        ArrayList<Color> colors = new ArrayList<>();

        for (int y = 0; y < 30; y++) {
            for (int x = 0; x < 10; x++) {

                float w = (float) ((680 / 4f) * (Math.random() + 0.1f));
                float h = (float) ((656 / 4f) * (Math.random() + 0.1f));


                RigidBody2D r1 = null;

                if(Math.random() > 0.5) {
                    r1 = world.newCircleBody2D(new Vector2f(x * w, y * w), w / 2, RigidBody2D.Type.DYNAMIC, true);
                }
                else {
                    r1 = world.newRectBody2D(new Rect2D(x * w, y * h, w, h), RigidBody2D.Type.DYNAMIC, true);
                }

                r1.setPhysicalProps(0.5f, 0.4f, 0.2f);
                colors.add(new Color((float) Math.random(), (float) Math.random(), (float) Math.random(), 1f));


                bodies.add(r1);
            }
        }

        RectBody2D r2 = world.newRectBody2D(new Rect2D(0, renderer2D.getHeight() - 20, renderer2D.getWidth(), 20), RigidBody2D.Type.STATIC, true);
        r2.setPhysicalProps(0.5f, 0.4f, 0.01f);

        RectBody2D r3 = world.newRectBody2D(new Rect2D(0, renderer2D.getHeight() - 520, renderer2D.getWidth() / 4f, 20), RigidBody2D.Type.STATIC, true);
        r3.setPhysicalProps(0.5f, 0.4f, 0.01f);

        Texture2D texture2D = Texture2D.newTexture2D(500, 500);
        texture2D.setData(AssetPacks.<TextureData> getAsset("core:assets/logo.png").asset.data);




        boolean pause = true;


        window.setTitle(renderer2D.getDeviceName());



        while(!window.shouldClose()){
            renderer2D.acquireNextImage();



            renderer2D.clear(Color.WHITE);
            renderer2D.drawTexture(0, 0, texture2D.getWidth() * 2, texture2D.getHeight() * 2, texture2D);











            //renderer2D.drawText(0, 0, "Box2D physics demo! FPS: " + window.getFPS() + "\n\n" + renderer2D.getDeviceName(), Color.RED, Font2D.getDefault());
            {

                for (int i = 0; i < bodies.size(); i++) {
                    RigidBody2D r = bodies.get(i);

                    renderer2D.setOrigin(r.getOrigin());
                    renderer2D.rotate(r.getRotation());

                    if (r instanceof CircleBody2D r1) {
                        renderer2D.drawFilledEllipse(r1.getPosition().x, r1.getPosition().y, r1.getRadius() * 2, r1.getRadius() * 2, colors.get(i));
                    }
                    if (r instanceof RectBody2D r1) {
                        renderer2D.drawFilledRect(r1.getPosition().x, r1.getPosition().y, r1.getWidth(), r1.getHeight(), colors.get(i));
                    }

                    renderer2D.resetTransform();
                    renderer2D.setOrigin(0, 0);
                }

                


                renderer2D.setOrigin(r2.getOrigin());
                renderer2D.rotate(r2.getRotation());
                renderer2D.drawFilledRect(r2.getPosition().x, r2.getPosition().y, r2.getWidth(), r2.getHeight(), Color.RED);
                renderer2D.resetTransform();
                renderer2D.setOrigin(0, 0);


                renderer2D.setOrigin(r3.getOrigin());
                renderer2D.rotate(r3.getRotation());
                renderer2D.drawFilledRect(r3.getPosition().x, r3.getPosition().y, r3.getWidth(), r3.getHeight(), Color.RED);
                renderer2D.resetTransform();
                renderer2D.setOrigin(0, 0);
                


            }






            if (pause) {
                world.update(1/60f);
            }











            renderer2D.drawText(0, 0, "FPS: " + Time.framesPerSecond(), Color.RED, Font2D.getDefault());
            renderer2D.render();

            renderer2D.renderFinished();
            window.update();
        }

        window.close();
    }
}
