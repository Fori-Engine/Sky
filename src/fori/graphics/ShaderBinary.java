package fori.graphics;

import java.nio.ByteBuffer;

import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_release;

public class ShaderBinary {
    public long handle;
    public ByteBuffer bytecode;

    public ShaderBinary(long handle, ByteBuffer bytecode) {
        this.handle = handle;
        this.bytecode = bytecode;
    }

    public void cleanup() {
        shaderc_result_release(handle);
    }
}