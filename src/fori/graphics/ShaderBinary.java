package fori.graphics;

import java.nio.ByteBuffer;

import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_release;

public class ShaderBinary {
    public long handle;
    public ByteBuffer data;

    public ShaderBinary(long handle, ByteBuffer data) {
        this.handle = handle;
        this.data = data;
    }

    public void cleanup() {
        shaderc_result_release(handle);
    }
}