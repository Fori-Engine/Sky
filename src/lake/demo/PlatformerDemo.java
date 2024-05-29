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
import org.joml.Vector2f;

import java.io.File;
import java.util.ArrayList;

import static lake.graphics.vulkan.VulkanRenderer2D.TOTAL_SIZE_BYTES;

public class PlatformerDemo {


    static float t = 0;
    public static void main(String[] args) throws InterruptedException {
        AssetPacks.open("core", AssetPack.openPack(new File("assets.pkg")));

        FlightRecorder.setEnabled(true);
        FlightRecorder.meltdown(PlatformerDemo.class, "This is very very bad");
        FlightRecorder.error(PlatformerDemo.class, "This is very bad");
        FlightRecorder.info(PlatformerDemo.class, "This isn't bad");
        FlightRecorder.todo(PlatformerDemo.class, "I'm just being lazy");



        Window window = new Window(1920, 1080, "Showcase Demo", false);



        Renderer2D renderer2D = Renderer2D.newRenderer2D(window, window.getWidth(), window.getHeight(), new RenderSettings(RenderAPI.Vulkan).msaa(true));
        ShaderReader.ShaderSources shaderSources = ShaderReader.readCombinedVertexFragmentSources(
                AssetPacks.<String> getAsset("core:assets/shaders/vulkan/Default.glsl").asset
        );


        ShaderProgram shaderProgram = ShaderProgram.newShaderProgram(
                shaderSources.vertexShader,
                shaderSources.fragmentShader
        );


        shaderProgram.prepare();



        ShaderResource modelViewProj = new ShaderResource(0)
                .type(ShaderResource.Type.UniformBuffer)
                .shaderStage(ShaderResource.ShaderStage.VertexStage)
                .sizeBytes(TOTAL_SIZE_BYTES)
                .count(1);

        ShaderResource sampler2DArray = new ShaderResource(1)
                .type(ShaderResource.Type.CombinedSampler)
                .shaderStage(ShaderResource.ShaderStage.FragmentStage)
                .count(32);

        shaderProgram.addResource(modelViewProj);
        shaderProgram.addResource(sampler2DArray);

        renderer2D.createResources(shaderProgram);


        Texture2D emptyTexture = Texture2D.newTexture2D(AssetPacks.getAsset("core:assets/empty.png"), Texture2D.Filter.Nearest);
        shaderProgram.updateEntireSampler2DArrayWithOnly(sampler2DArray, emptyTexture);

        Texture2D texture = Texture2D.newTexture2D(AssetPacks.getAsset("core:assets/logo.png"), Texture2D.Filter.Nearest);



        ArrayList<Color> colors = new ArrayList<>();
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                colors.add(new Color(
                        (float) Math.random(),
                        (float) Math.random(),
                        (float) Math.random(),
                        1)
                );
            }
        }






        while(!window.shouldClose()){

            FlightRecorder.info(PlatformerDemo.class, "FPS: " + Time.framesPerSecond());

            renderer2D.acquireNextImage();
            renderer2D.clear(Color.WHITE);

            t += Time.deltaTime();

            renderer2D.updateMatrices(shaderProgram, modelViewProj);
            renderer2D.submit(new RendererSubmit(shaderProgram, () -> {

                int width = 60, height = 60;

                for (int y = 0; y < 10; y++) {
                    for (int x = 0; x < 10; x++) {

                        renderer2D.setOrigin((x * width), (y * height));
                        renderer2D.rotate(t);
                        renderer2D.drawFilledRect(x * width, y * height, width, height, colors.get((y * 10) + x));
                        renderer2D.resetTransform();
                    }

                }

            }));

            t += Time.deltaTime();




            renderer2D.submit(new RendererSubmit(shaderProgram, () -> {
                int width = 60, height = 60;

                for (int y = 0; y < 10; y++) {
                    for (int x = 0; x < 10; x++) {
                        renderer2D.drawFilledRect((x * width) + 300, (y * height) + 150, width, height, Color.RED);
                    }
                }
            }));

            renderer2D.render();


            renderer2D.renderFinished();
            window.update();
        }

        window.close();
    }
}
