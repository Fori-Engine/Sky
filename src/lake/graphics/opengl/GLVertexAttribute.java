package lake.graphics.opengl;

/***
 * Represents a GLSL Vertex Attribute
 */
public class GLVertexAttribute {
    public int index;
    public int size;
    public boolean normalized;
    public String name;

    public GLVertexAttribute(int index, int size, boolean normalized, String name) {
        this.index = index;
        this.size = size;
        this.normalized = normalized;
        this.name = name;
    }
}
