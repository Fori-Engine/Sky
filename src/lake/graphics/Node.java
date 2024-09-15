package lake.graphics;

import org.joml.Matrix4f;

import java.util.LinkedList;
import java.util.List;

public class Node {
    private List<Node> children = new LinkedList<>();
    private Matrix4f transform = new Matrix4f();
    private Mesh mesh;

    public Node(){

    }

    public Node(Mesh mesh) {
        this.mesh = mesh;
    }

    public void addNode(Node child){
        children.add(child);
    }

    public void removeChild(Node child){
        children.remove(child);
    }

    public List<Node> getChildren() {
        return children;
    }

    public Mesh getMesh() {
        return mesh;
    }

    public Matrix4f getTransform() {
        return transform;
    }

    public void setTransform(Matrix4f transform) {
        this.transform = transform;
    }
}
