package engine.mio;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public record Instruction(Opcode opcode, Object[] operands) {
    @NotNull
    @Override
    public String toString() {
        return opcode.toString() + " " + Arrays.toString(operands);
    }
}
