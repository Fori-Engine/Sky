package lake.demo;

import lake.FileReader;
import lake.graphics.*;
import org.lwjgl.opengl.GL20;

import static org.lwjgl.opengl.GL46.*;

public class Demo {
    public static void main(String[] args) {
        Window window = new StandaloneWindow(1920, 1080, "Renderer2D needs work");

        //Who manages the OpenGL context? Is it Window or Renderer2D?
        //How do we manage complex state? Do we want to pass bloom/hdr/instancing and all this stuff to every method?
        //Do we want to stick with no ECS and just let the user design the game how they want?
        //How do we manage different render APIs (OpenGL/Vulkan)?
        //Renderers don't resize all that well





        Framebuffer2D inputFB = new Framebuffer2D(1920, 1080, GL_RGBA32F);
        Renderer2D renderer2D = new Renderer2D(1920, 1080, true, inputFB);
        Texture2D texture2D = new Texture2D("project/logo.png");
        Renderer2D realRenderer2D = new Renderer2D(1920, 1080, true);


        int computeShader = glCreateShader(GL_COMPUTE_SHADER);
        GL20.glShaderSource(computeShader, FileReader.readFile(Renderer2D.class.getClassLoader().getResourceAsStream("BloomComputeShader.glsl")));
        glCompileShader(computeShader);
        System.err.println(glGetShaderInfoLog(computeShader));

        int computeShaderProgram = glCreateProgram();
        glAttachShader(computeShaderProgram, computeShader);
        glDeleteShader(computeShader);
        glLinkProgram(computeShaderProgram);



        Framebuffer2D outputFB = new Framebuffer2D(1920 / 5, 1080 / 5, GL_RGBA32F);
        glBindImageTexture(0, outputFB.getTexture2D().getTexID(), 0, false, 0, GL_READ_WRITE, GL_RGBA32F);
        glBindImageTexture(1, inputFB.getTexture2D().getTexID(), 0, false, 0, GL_READ_WRITE, GL_RGBA32F);







        while(!window.shouldClose()){

            {
                renderer2D.clear(Color.BLACK);
                renderer2D.drawTexture(0, 0, 300, 300, texture2D);
                renderer2D.render();
            }


            glUseProgram(computeShaderProgram);
            glDispatchCompute(1920, 1080, 1);
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

            realRenderer2D.clear(Color.WHITE);
            realRenderer2D.drawTexture(0, 0,outputFB.getWidth(), outputFB.getHeight(), outputFB.getTexture2D());
            realRenderer2D.render();

            System.out.println(glGetError());
            System.out.println(window.getFPS());






            window.update();
        }
        window.close();




    }
}
