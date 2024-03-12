package lake;

import lake.graphics.Color;
import lake.graphics.Renderer2D;
import lake.graphics.StandaloneWindow;
import lake.graphics.Window;
import org.joml.Matrix4f;

public class Test {
    public static void main(String[] args) {
        Window window = new StandaloneWindow(640, 480, "Test");
        Renderer2D renderer2D = new Renderer2D(640, 480, true);


        int tx = 200;
        int ty = 60;

        while(!window.shouldClose()){
            renderer2D.clear(Color.WHITE);

            renderer2D.setTransform(new Matrix4f().translate(tx, ty, 0));
            renderer2D.setOrigin(0, 0);
            renderer2D.rotate((float) java.lang.Math.toRadians(45));
            renderer2D.scale(2f, 2f, 1f);


            renderer2D.drawLine(0, 0, 60, 0, Color.RED, 5, true);


            renderer2D.render();
            renderer2D.resetTransform();

            window.update();
        }
        window.close();





    }
}
