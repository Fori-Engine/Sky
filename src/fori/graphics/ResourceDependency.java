package fori.graphics;

public class ResourceDependency<ResourceType> {
    private String name;
    private ResourceType dependency;
    private int type;
    private Pass passMetadata;

    public ResourceDependency(String name, ResourceType dependency, int type) {
        this.name = name;
        this.dependency = dependency;
        this.type = type;
    }

    public Pass getPassMetadata() {
        return passMetadata;
    }

    public void setPassMetadata(Pass passMetadata) {
        this.passMetadata = passMetadata;
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
