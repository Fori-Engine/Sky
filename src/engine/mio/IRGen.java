package engine.mio;

import java.util.ArrayList;
import java.util.List;

public class IRGen {

    private List<Instruction> frames = new ArrayList<>();

    public void emit(Instruction instruction) {
        frames.add(instruction);
    }

    public List<Instruction> getList() {
        return frames;
    }
}
