package fori.graphics;

public class VertexAttribute {
    private String name;
    private int size;

    public VertexAttribute(String name, int size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }
}
