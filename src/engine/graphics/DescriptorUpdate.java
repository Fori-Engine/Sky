package engine.graphics;

public class DescriptorUpdate<DescriptorResource> {
    private String name;
    private DescriptorResource resource;

    private int arrayIndex = 0;
    private int updateCount = 1;

    public DescriptorUpdate(String name, DescriptorResource resource) {
        this.name = name;
        this.resource = resource;
    }

    public String getName() {
        return name;
    }

    public DescriptorResource getResource() {
        return resource;
    }

    public int getArrayIndex() {
        return arrayIndex;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    public DescriptorUpdate<DescriptorResource> arrayIndex(int arrayIndex){
        this.arrayIndex = arrayIndex;
        return this;
    }

    public DescriptorUpdate<DescriptorResource> updateCount(int updateCount){
        this.updateCount = updateCount;
        return this;
    }
}
