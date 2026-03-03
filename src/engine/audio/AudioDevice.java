package engine.audio;

import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

import static org.lwjgl.openal.ALC11.*;
import static org.lwjgl.openal.EXTEfx.ALC_MAX_AUXILIARY_SENDS;
import static org.lwjgl.openal.SOFTOutputMode.ALC_STEREO_SOFT;

public class AudioDevice implements ALResourceHolder {
    private String name;
    private long handle;

    public AudioDevice(String name, long handle) {
        this.name = name.substring(15);
        this.handle = handle;
    }

    public String getName() {
        return name;
    }

    public long getHandle() {
        return handle;
    }

    public boolean isStereo() {
        boolean stereo = false;
        try(MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pOutputMode = stack.mallocInt(1);
            alcGetIntegerv(handle, SOFTOutputMode.ALC_OUTPUT_MODE_SOFT, pOutputMode);

            if(pOutputMode.get(0) == ALC_STEREO_SOFT)
                stereo = true;
        }

        return true;
    }

    public static AudioDevice getSystemAudioDevice() {
        String deviceName = alcGetString(MemoryUtil.NULL, ALC_ALL_DEVICES_SPECIFIER);
        long handle = alcOpenDevice(deviceName);

        ALCCapabilities deviceCapabilities = ALC.createCapabilities(handle);
        IntBuffer contextAttribList = MemoryUtil.memAllocInt(16);

        contextAttribList.put(ALC_REFRESH);
        contextAttribList.put(60);

        contextAttribList.put(ALC_SYNC);
        contextAttribList.put(ALC_FALSE);

        contextAttribList.put(ALC_MAX_AUXILIARY_SENDS);
        contextAttribList.put(2);

        contextAttribList.put(0);
        contextAttribList.flip();

        long context = alcCreateContext(handle, contextAttribList);
        MemoryUtil.memFree(contextAttribList);

        if(!alcMakeContextCurrent(context)) {
            throw new RuntimeException("Failed to create OpenAL Context");
        }

        AL.createCapabilities(deviceCapabilities);

        return new AudioDevice(deviceName, handle);
    }

    @Override
    public void free() {
        alcCloseDevice(handle);
    }
}
