package noir.citizens;

import fori.ecs.Component;
import fori.graphics.ShaderProgram;
import fori.graphics.Texture;
import org.joml.Matrix4f;

import java.util.List;

public class MeshComponent extends Component {
    public List<Float> vertices;
    public List<Integer> indices;
    public ShaderProgram shaderProgram;
    public Texture texture;
    public Matrix4f transform = new Matrix4f().identity();
    boolean queued = false;
    int queueIndex = 0;

    public MeshComponent(List<Float> vertices, List<Integer> indices, ShaderProgram shaderProgram, Texture texture) {
        this.vertices = vertices;
        this.indices = indices;
        this.shaderProgram = shaderProgram;
        this.texture = texture;
    }
}
