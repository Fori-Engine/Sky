package fori.graphics;

public class ResourceDependency<ResourceType> {
    private ResourceType dependency;
    private int type;

    public ResourceDependency(ResourceType dependency, int type) {
        this.dependency = dependency;
        this.type = type;
    }

    public ResourceType getDependency() {
        return dependency;
    }

    public void setDependency(ResourceType dependency) {
        this.dependency = dependency;
    }

    public int getType() {
        return type;
    }
}
