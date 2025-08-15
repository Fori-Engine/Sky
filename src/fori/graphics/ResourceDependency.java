package fori.graphics;

public class ResourceDependency<ResourceType> {
    private String name;
    private ResourceType dependency;
    private int type;

    public ResourceDependency(String name, ResourceType dependency, int type) {
        this.name = name;
        this.dependency = dependency;
        this.type = type;
    }

    public String getName() {
        return name;
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
