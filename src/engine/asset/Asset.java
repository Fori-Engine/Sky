package engine.asset;

public class Asset<T> {
    public String name;
    public T asset;

    public Asset() {
    }

    public Asset(String name, T asset) {
        this.name = name;
        this.asset = asset;
    }
}
