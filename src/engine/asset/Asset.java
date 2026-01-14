package engine.asset;

public class Asset<T> {
    private T object;
    private AssetPackage assetPackage;
    private String path;

    public Asset() {}

    public Asset(AssetPackage assetPackage, String path, T object) {
        this.assetPackage = assetPackage;
        this.path = path;
        this.object = object;
    }

    public T getObject() {
        return object;
    }

    public AssetPackage getAssetPackage() {
        return assetPackage;
    }

    public String getPath() {
        return path;
    }

    public String getFQN() {
        return assetPackage.getNamespace() + ":" + path;
    }
}
