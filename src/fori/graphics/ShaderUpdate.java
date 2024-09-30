package fori.graphics;

public class ShaderUpdate<T> {
    public String friendlyName;
    public int set;
    public int binding;
    public T update;

    public ShaderUpdate(String friendlyName, int set, int binding, T update) {
        this.friendlyName = friendlyName;
        this.set = set;
        this.binding = binding;
        this.update = update;
    }
}
