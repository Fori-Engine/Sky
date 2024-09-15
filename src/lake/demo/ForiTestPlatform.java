package lake.demo;

import lake.FlightRecorder;
import lake.Time;
import lake.asset.AssetPack;
import lake.asset.AssetPacks;
import lake.graphics.*;

import java.io.File;


public class ForiTestPlatform {

    public static void main(String[] args) {
        AssetPacks.open("core", AssetPack.openLocal(new File("assets")));

        FlightRecorder.setEnabled(true);

        PlatformWindow window = new PlatformWindow(640, 480, "ForiEngine", true);
        SceneRenderer sceneRenderer = SceneRenderer.newSceneRenderer(window, window.getWidth(), window.getHeight(), new RendererSettings(RenderAPI.Vulkan).validation(true).vsync(false));
        window.setIcon(AssetPacks.getAsset("core:assets/ForiEngine.png"));

        Node root = new Node();


        Node node1 = new Node(new Mesh(
            new float[]{
                    0.0f,
                    -0.5f,

                    0.5f,
                    0.5f,

                    -0.5f,
                    0.5f
            },
            new int[]{

            }
        ));

        Node node2 = new Node(new Mesh(
                new float[]{
                        0.5f,
                        -0.5f,

                        1.0f,
                        0.5f,

                        0.0f,
                        0.5f
                },
                new int[]{

                }
        ));


        root.addNode(node1);
        node1.addNode(node2);



        Scene scene = new Scene(root, new Camera());


        sceneRenderer.openScene(scene);










        while(!window.shouldClose()){

            System.out.println(Time.framesPerSecond());

            sceneRenderer.update();
            window.update();
        }

        window.close();
    }
}
