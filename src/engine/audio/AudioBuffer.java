package engine.audio;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.openal.AL11.*;

public class AudioBuffer implements ALResourceHolder {
    private float duration;
    private int format;
    private ByteBuffer pData;
    private int handle;
    private int sampleRate;

    public AudioBuffer() {
        handle = alGenBuffers();
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getFormat() {
        return format;
    }

    public void setFormat(int format) {
        this.format = format;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public float getDuration() {
        return duration;
    }

    public ByteBuffer getData() {
        return pData;
    }

    public void setData(ByteBuffer pData) {
        alBufferData(handle, format, pData, sampleRate);
        this.pData = pData;
    }

    @Override
    public void free() {
        MemoryUtil.memFree(pData);
        alDeleteBuffers(new int[]{1, handle});
    }

    public int getHandle() {
        return handle;
    }
}
