package noir.citizens;

import fori.ecs.Component;
import fori.graphics.Mesh;
import fori.graphics.ShaderProgram;
import fori.graphics.Texture;
import org.joml.Matrix4f;

public class MeshComponent extends Component {
    public Mesh mesh;
    public ShaderProgram shaderProgram;
    public Texture[] textures;
    public Matrix4f transform = new Matrix4f().identity();
    boolean queued = false;
    int queueIndex = 0;

    public MeshComponent(Mesh mesh, ShaderProgram shaderProgram, Texture... textures) {
        this.mesh = mesh;
        this.shaderProgram = shaderProgram;
        this.textures = textures;
    }
}
