package lake;

import lake.graphics.Color;
import lake.graphics.opengl.GLRenderer2D;
import lake.graphics.StandaloneWindow;
import lake.graphics.Window;
import org.joml.Matrix4f;

public class Test {
    public static void main(String[] args) {
        Window window = new StandaloneWindow(640, 480, "Test");
        GLRenderer2D GLRenderer2D = new GLRenderer2D(640, 480, true);


        int tx = 200;
        int ty = 60;

        while(!window.shouldClose()){
            GLRenderer2D.clear(Color.WHITE);

            GLRenderer2D.setTransform(new Matrix4f().translate(tx, ty, 0));
            GLRenderer2D.setOrigin(0, 0);
            GLRenderer2D.rotate((float) java.lang.Math.toRadians(45));
            GLRenderer2D.scale(2f, 2f, 1f);


            GLRenderer2D.drawLine(0, 0, 60, 0, Color.RED, 5, true);


            GLRenderer2D.render();
            GLRenderer2D.resetTransform();

            window.update();
        }
        window.close();





    }
}
