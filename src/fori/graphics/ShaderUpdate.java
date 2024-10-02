package fori.graphics;

public class ShaderUpdate<T> {
    public String friendlyName;
    public int set;
    public int binding;
    public T update;

    public int arrayIndex;
    public int updateCount = 1;

    public ShaderUpdate(String friendlyName, int set, int binding, T update) {
        this.friendlyName = friendlyName;
        this.set = set;
        this.binding = binding;
        this.update = update;
    }

    public ShaderUpdate<T> arrayIndex(int arrayIndex){
        this.arrayIndex = arrayIndex;
        return this;
    }

    public ShaderUpdate<T> updateCount(int updateCount){
        this.updateCount = updateCount;
        return this;
    }
}
