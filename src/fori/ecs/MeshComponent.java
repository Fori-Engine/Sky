package fori.ecs;

import fori.graphics.Material;
import fori.graphics.Mesh;
import fori.graphics.ShaderProgram;
import org.joml.Matrix4f;

public class MeshComponent extends Component {
    public Mesh mesh;
    public ShaderProgram shaderProgram;
    public Material[] materials;
    public Matrix4f transform = new Matrix4f().identity();
    boolean queued = false;




    public MeshComponent(Mesh mesh, ShaderProgram shaderProgram, Material... materials) {
        this.mesh = mesh;
        this.shaderProgram = shaderProgram;
        this.materials = materials;


    }
}
