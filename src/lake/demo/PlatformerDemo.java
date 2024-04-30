package lake.demo;

import lake.FileReader;
import lake.FlightRecorder;
import lake.graphics.*;
import lake.graphics.Color;
import static org.lwjgl.nuklear.Nuklear.*;

import lake.graphics.vulkan.LVKRenderer2D;
import lake.physics.CircleBody2D;
import lake.physics.RectBody2D;
import lake.physics.RigidBody2D;
import lake.physics.World;
import org.joml.Vector2f;
import org.lwjgl.nuklear.Nuklear;


import java.util.ArrayList;

public class PlatformerDemo {


    public static void main(String[] args) throws InterruptedException {

        FlightRecorder.setEnabled(true);




        StandaloneWindow window = new StandaloneWindow(1920, 1080, "Showcase Demo", false, true);
        Renderer2D renderer2D = Renderer2D.createRenderer(RendererType.OPENGL, window, window.getWidth(), window.getHeight(), true);


        /*ShaderProgram shaderProgram = ShaderProgram.newShaderProgram(
                FileReader.readFile("test/VertexShader.glsl"),
                FileReader.readFile("test/FragmentShader.glsl"));
        shaderProgram.prepare();


         */

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





        boolean pause = true;





        while(!window.shouldClose()){
            renderer2D.clear(Color.WHITE);





            //renderer2D.setShader(shaderProgram);





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


            //renderer2D.render();







            //renderer2D.setShader(renderer2D.getDefaultShader());
            //renderer2D.drawText(0, 0, "This is some text boi", Color.GREEN, Font2D.getDefault());
            renderer2D.render();



            window.update();
        }

        window.close();
    }
}
