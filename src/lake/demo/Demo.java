package lake.demo;

import lake.FileReader;
import lake.graphics.*;
import org.lwjgl.opengl.GLUtil;

public class Demo {
    public static void main(String[] args) {
        Window window = new StandaloneWindow(640, 480, "Instance Test");
        Renderer2D renderer2D = new Renderer2D(640, 480, true);

        ShaderProgram instancingShader = new ShaderProgram(
                FileReader.readFile(Renderer2D.class.getClassLoader().getResourceAsStream("InstanceVertexShader.glsl")),
                FileReader.readFile(Renderer2D.class.getClassLoader().getResourceAsStream("InstanceFragmentShader.glsl"))
        );
        instancingShader.prepare();


        Texture2D texture2D = new Texture2D("project/logo.png");



        while(!window.shouldClose()){
            renderer2D.setShader(instancingShader);




            //Remember to change the uniform array size of vec2 offsets in InstanceVertexShader.glsl
            instancingShader.setVector2fArray("offsets", new float[]{100, 100, 200, 200, 300, 300, 400, 400});

            renderer2D.drawTexture(0, 0, 60, 60, texture2D);
            renderer2D.drawFilledRect(60, 0, 60, 60, Color.RED);





            renderer2D.renderInstanced(4);
            renderer2D.setShader(renderer2D.getDefaultShader());



            //renderer2D.render();
            window.update();
        }

        window.close();



    }
}
