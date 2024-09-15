package lake.graphics;

public class Scene {

    private Node node;
    private Camera camera;

    public Scene(Node node, Camera camera) {
        this.node = node;
        this.camera = camera;
    }

    public Node getNode() {
        return node;
    }
}
