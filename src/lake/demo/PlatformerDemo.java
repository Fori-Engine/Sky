package lake.demo;

import lake.FileReader;
import lake.FlightRecorder;
import lake.Time;
import lake.asset.AssetPack;
import lake.asset.AssetPacks;
import lake.asset.TextureData;
import lake.graphics.*;
import lake.graphics.vulkan.VulkanShaderProgram;
import lake.graphics.vulkan.VulkanTexture2D;
import lake.physics.CircleBody2D;
import lake.physics.RectBody2D;
import lake.physics.RigidBody2D;
import lake.physics.World;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import static lake.graphics.vulkan.VulkanRenderer2D.TOTAL_SIZE_BYTES;

public class PlatformerDemo {


    static float t = 0;
    static float elapsedTime = 0;
    static int currentIndex = 0;
    public static void main(String[] args) throws InterruptedException {
        AssetPacks.open("core", AssetPack.openLocal(new File("assets")));

        FlightRecorder.setEnabled(true);
        FlightRecorder.meltdown(PlatformerDemo.class, "This is very very bad");
        FlightRecorder.error(PlatformerDemo.class, "This is very bad");
        FlightRecorder.info(PlatformerDemo.class, "This isn't bad");
        FlightRecorder.todo(PlatformerDemo.class, "I'm just being lazy");



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
                    .count(32);

            shaderProgram1.addResource(modelViewProj);
            shaderProgram1.addResource(sampler2DArray);

            renderer2D.createResources(shaderProgram1);


            Texture2D emptyTexture = Texture2D.newTexture2D(AssetPacks.getAsset("core:assets/empty.png"), Texture2D.Filter.Nearest);
            shaderProgram1.updateEntireSampler2DArrayWithOnly(sampler2DArray, emptyTexture);
            renderer2D.updateMatrices(shaderProgram1, modelViewProj);

        }


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




        Random random = new Random();
        Matrix3x2f shear = new Matrix3x2f();
        shear.m10 = 1;



        float epsilon = 0.01f;

        //Transforms, Textures, lots to think about
        while(!window.shouldClose()){


            renderer2D.clear(Color.WHITE);

            elapsedTime += Time.deltaTime();
            t = (float) ((float) (Math.toRadians(45) * Math.sin(((2 * Math.PI) / 3) * elapsedTime)) + Math.toRadians(45));

            if(t >= Math.toRadians(90 - epsilon)){
                currentIndex = random.nextInt(5 * 5);
            }


            renderer2D.startBatch(shaderProgram1);
            {
                int width = 100, height = 100;

                for (int y = 0; y < 5; y++) {
                    for (int x = 0; x < 5; x++) {

                        int myIndex = (y * 5) + x;

                        float thickness = 0.5f;

                        if (myIndex == currentIndex) {

                            renderer2D.setOrigin((x * width) + width / 2f, (y * height) + height / 2f);
                            thickness = (float) ((1 / Math.toRadians(180f)) * (t + Math.toRadians(90)));

                        }

                        renderer2D.drawEllipse(x * width, y * height, width, height, colors.get((y * 5) + x), thickness);
                        renderer2D.resetTransform();
                        renderer2D.setOrigin(0, 0);
                    }

                }
            }
            renderer2D.endBatch();

            renderer2D.startBatch(shaderProgram2);
            {

                renderer2D.setTransform(new Matrix4f().identity().mul(shear));

                int w = 60, h = 60;

                for (int y = 0; y < 10; y++) {
                    for (int x = 0; x < 10; x++) {
                        renderer2D.drawFilledRect(((x * w) + 1 + 300), ((y * h) + 1 + 150), w - 2, h - 2, Color.WHITE);
                    }
                }

                renderer2D.resetTransform();
            }
            renderer2D.endBatch();



            renderer2D.render();



            window.update();
        }

        window.close();
    }
}
