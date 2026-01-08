package engine.graphics;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

public class DebugUtil {
    public static final void printPointerBuffer(String name, PointerBuffer pointerBuffer){
        System.out.println("[" + name + "] " + pointerBuffer.capacity());
        for (int i = 0; i < pointerBuffer.capacity(); i++) {
            long address = pointerBuffer.get(i);
            String str = MemoryUtil.memUTF8(address); // Read the string back from memory
            System.out.println("\t" + str);
        }
    }
}
