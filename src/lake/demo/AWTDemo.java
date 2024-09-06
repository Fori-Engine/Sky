
package lake.demo;

import lake.FlightRecorder;
import lake.Time;
import lake.asset.AssetPack;
import lake.asset.AssetPacks;
import lake.graphics.*;
import lake.graphics.Color;
import org.w3c.dom.Text;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import static lake.graphics.vulkan.VulkanRenderer2D.TOTAL_SIZE_BYTES;

public class AWTDemo {

    static Renderer2D renderer2D;
    static boolean active;
    static ShaderProgram shaderProgram1;
    static Texture2D texture2D;

    public static void main(String[] args) {


        AssetPacks.open("core", AssetPack.openLocal(new File("assets")));

        FlightRecorder.setEnabled(true);


        JFrame frame = new JFrame("AWT test");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());



        Canvas canvas = new Canvas(){
            @Override
            public void paint(Graphics g) {
                super.paint(g);







                if(!active){
                    active = true;


                    ShaderReader.ShaderSources shaderSources1 = ShaderReader.readCombinedVertexFragmentSources(
                            AssetPacks.<String> getAsset("core:assets/shaders/vulkan/Default.glsl").asset
                    );

                    shaderProgram1 = ShaderProgram.newShaderProgram(
                            shaderSources1.vertexShader,
                            shaderSources1.fragmentShader
                    );
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

                    texture2D = Texture2D.newTexture2D(AssetPacks.getAsset("core:assets/ForiEngine.png"), Texture2D.Filter.Linear);


                }






                renderer2D.clear(lake.graphics.Color.WHITE);

                renderer2D.startBatch(shaderProgram1);
                {
                    int width = 100, height = 100;
                    for (int y = 0; y < 5; y++) {
                        for (int x = 0; x < 5; x++) {
                            renderer2D.drawTexture(x * width, y * height, width, height, texture2D);
                        }

                    }
                    renderer2D.drawText(0, 0, "This is a really boring test of Fori!" + Time.framesPerSecond(), Color.RED, Font2D.getDefault());
                }
                renderer2D.endBatch();
                renderer2D.render();
            }
        };

        frame.add(canvas, BorderLayout.CENTER);
        frame.pack(); // Packing causes the canvas to be lockable, and is the earliest time it can be used
        frame.setSize(new Dimension(1920, 1080));


        renderer2D = Renderer2D.newRenderer2D(canvas, 1920, 1080, new RenderSettings(RenderAPI.Vulkan));










        frame.setVisible(true);

    }
}


