package fori.ecs;

import fori.graphics.Material;
import fori.graphics.Mesh;
import fori.graphics.ShaderProgram;
import org.joml.Matrix4f;

public class MeshComponent extends Component {
    public Mesh mesh;
    public ShaderProgram shaderProgram;
    public Material material;
    public Matrix4f transform = new Matrix4f().identity();
    public boolean queued = false;
    public boolean materialChanged = false;
    public int queueIndex = 0;

    public MeshComponent(Mesh mesh, ShaderProgram shaderProgram, Material material) {
        this.mesh = mesh;
        this.shaderProgram = shaderProgram;
        this.material = material;


    }
}
