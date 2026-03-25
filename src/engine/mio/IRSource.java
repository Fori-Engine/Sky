package engine.mio;

import java.util.ArrayList;
import java.util.List;

public class IRSource {
    private List<Object[]> frames = new ArrayList<>();
    public static final String PROP_ADD = "PROP_ADD";
    public static final String COMPONENT_ADD = "COMPONENT_ADD";
    public static final String ACTOR_ADD = "ACTOR_ADD";
    public static final String ACTOR_END = "ACTOR_END";


    public void addFrame(Object[] frame) {
        frames.add(frame);
    }

    public List<Object[]> getFrames() {
        return frames;
    }
}
