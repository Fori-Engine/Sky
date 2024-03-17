package lake.vulkan;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class VulkanUtils {
    public static ByteBuffer UTF8(String text){
        // Encode the string to UTF-8
        Charset utf8Charset = Charset.forName("UTF-8");
        ByteBuffer utf8Buffer = utf8Charset.encode(text);

        // Create a new ByteBuffer to include the null terminator
        ByteBuffer nullTerminatedBuffer = ByteBuffer.allocate(utf8Buffer.remaining() + 1);
        nullTerminatedBuffer.put(utf8Buffer);
        nullTerminatedBuffer.put((byte) 0); // Append null terminator

        // Print the UTF-8 encoded bytes with null terminator
        nullTerminatedBuffer.flip(); // Prepare for reading

        return nullTerminatedBuffer;
    }
}
