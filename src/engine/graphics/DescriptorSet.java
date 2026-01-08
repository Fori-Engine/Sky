package engine.graphics;

import java.util.ArrayList;
import java.util.List;

public class DescriptorSet {
    private final int setNum;
    private List<Descriptor> descriptors = new ArrayList<>();

    public DescriptorSet(int setNum) {
        this.setNum = setNum;
    }

    public int getSetNum() {
        return setNum;
    }

    public void addDescriptor(Descriptor descriptor) {
        descriptors.add(descriptor);
        descriptor.setDescriptorSet(this);
    }
    public List<Descriptor> getDescriptors() {
        return descriptors;
    }
}
