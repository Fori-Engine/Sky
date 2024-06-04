package lake.demo;

import lake.FlightRecorder;
import lake.Time;
import lake.asset.AssetPack;
import lake.asset.AssetPacks;
import lake.graphics.*;
import org.joml.Matrix4f;

import java.io.File;
import java.util.ArrayList;

import static lake.graphics.vulkan.VulkanRenderer2D.TOTAL_SIZE_BYTES;

public class Testbed {

    public static void main(String[] args) throws InterruptedException {
        AssetPacks.open("core", AssetPack.openPack(new File("assets.pkg")));

        FlightRecorder.setEnabled(true);
        FlightRecorder.meltdown(Testbed.class, "This is very very bad");
        FlightRecorder.error(Testbed.class, "This is very bad");
        FlightRecorder.info(Testbed.class, "This isn't bad");
        FlightRecorder.todo(Testbed.class, "I'm just being lazy");



        Window window = new Window(1920, 1080, "Showcase Demo", false);



        Renderer2D renderer2D = Renderer2D.newRenderer2D(window, window.getWidth(), window.getHeight(), new RenderSettings(RenderAPI.Vulkan).msaa(true).enableValidation(false));
        ShaderReader.ShaderSources shaderSources1 = ShaderReader.readCombinedVertexFragmentSources(
                AssetPacks.<String> getAsset("core:assets/shaders/vulkan/Default.glsl").asset
        );

        ShaderProgram shaderProgram1 = ShaderProgram.newShaderProgram(
                shaderSources1.vertexShader,
                shaderSources1.fragmentShader
        );

        {


            shaderProgram1.prepare();


            ShaderResource modelViewProj = new ShaderResource(0)
                    .type(ShaderResource.Type.UniformBuffer)
                    .shaderStage(ShaderResource.ShaderStage.VertexStage)
                    .sizeBytes(TOTAL_SIZE_BYTES)
                    .count(1);

            ShaderResource sampler2DArray = new ShaderResource(1)
                    .type(ShaderResource.Type.CombinedSampler)
                    .shaderStage(ShaderResource.ShaderStage.FragmentStage)
                    .count(shaderProgram1.getMaxBindlessSamplers());

            shaderProgram1.addResource(modelViewProj);
            shaderProgram1.addResource(sampler2DArray);

            renderer2D.createResources(shaderProgram1);


            Texture2D emptyTexture = Texture2D.newTexture2D(AssetPacks.getAsset("core:assets/empty.png"), Texture2D.Filter.Nearest);
            shaderProgram1.updateEntireSampler2DArrayWithOnly(sampler2DArray, emptyTexture);
            renderer2D.updateMatrices(shaderProgram1, modelViewProj);

        }


        /*
        ShaderReader.ShaderSources shaderSources2 = ShaderReader.readCombinedVertexFragmentSources(
                AssetPacks.<String> getAsset("core:assets/shaders/vulkan/DoItAgain.glsl").asset
        );

        ShaderProgram shaderProgram2 = ShaderProgram.newShaderProgram(
                shaderSources2.vertexShader,
                shaderSources2.fragmentShader
        );


        {


            shaderProgram2.prepare();


            ShaderResource modelViewProj = new ShaderResource(0)
                    .type(ShaderResource.Type.UniformBuffer)
                    .shaderStage(ShaderResource.ShaderStage.VertexStage)
                    .sizeBytes(TOTAL_SIZE_BYTES)
                    .count(1);

            ShaderResource sampler2DArray = new ShaderResource(1)
                    .type(ShaderResource.Type.CombinedSampler)
                    .shaderStage(ShaderResource.ShaderStage.FragmentStage)
                    .count(32);

            shaderProgram2.addResource(modelViewProj);
            shaderProgram2.addResource(sampler2DArray);

            renderer2D.createResources(shaderProgram2);

            Texture2D emptyTexture = Texture2D.newTexture2D(AssetPacks.getAsset("core:assets/empty.png"), Texture2D.Filter.Nearest);
            shaderProgram2.updateEntireSampler2DArrayWithOnly(sampler2DArray, emptyTexture);
            renderer2D.updateMatrices(shaderProgram2, modelViewProj);

        }


         */

        ArrayList<Color> colors = new ArrayList<>();
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                colors.add(new Color(
                        (float) Math.random(),
                        (float) Math.random(),
                        (float) Math.random(),
                        1)
                );
            }
        }

        ArrayList<Boolean> circles = new ArrayList<>();
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                circles.add(Math.random() >= 0.5f);
            }
        }



        Texture2D texture2D = Texture2D.newTexture2D(AssetPacks.getAsset("core:assets/logo.png"), Texture2D.Filter.Linear);




        while(!window.shouldClose()){


            renderer2D.clear(Color.WHITE);






            renderer2D.startBatch(shaderProgram1);
            {
                int width = 100, height = 100;



                for (int y = 0; y < 5; y++) {
                    for (int x = 0; x < 5; x++) {
                        //renderer2D.drawFilledRect(x * width, y * height, width, height, colors.get((y * 5) + x));

                        renderer2D.drawTexture(x * width, y * height, width, height, texture2D);
                    }

                }

                renderer2D.drawText(0, 0, "This is some text " + Time.framesPerSecond(), Color.RED, Font2D.getDefault());
            }
            renderer2D.endBatch();

            /*
            renderer2D.startBatch(shaderProgram2);
            {



                int w = 60, h = 60;

                for (int y = 0; y < 10; y++) {
                    for (int x = 0; x < 10; x++) {

                        Boolean b = circles.get((y * 5) + x);

                        if(b)
                            renderer2D.drawEllipse(((x * w) + 1 + 300), ((y * h) + 1 + 150), w - 2, h - 2, Color.WHITE, 0.5f);
                        else
                            renderer2D.drawRect(((x * w) + 1 + 300), ((y * h) + 1 + 150), w - 2, h - 2, Color.WHITE, 15);
                    }
                }


            }
            renderer2D.endBatch();

             */




            renderer2D.render();



            window.update();
        }

        window.close();
    }
}
