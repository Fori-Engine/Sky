package engine.audio;

import org.lwjgl.openal.EXTDisconnect;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.lwjgl.openal.AL11.*;
import static org.lwjgl.openal.ALC11.*;

public class AudioContext implements ALResourceHolder {
    private AudioDevice device;
    private long handle;
    private List<AudioSource> sources = new ArrayList<>();
    public AudioContext(AudioDevice device) {
        this.device = device;
        handle = alcCreateContext(device.getHandle(), (int[]) null);
        alcMakeContextCurrent(device.getHandle());
    }

    public List<AudioSource> getAudioSources() {
        return sources;
    }

    public void addAudioSource(AudioSource source) {
        sources.add(source);
    }

    public void removeAudioSource(AudioSource source) {
        sources.remove(source);
    }

    public Optional<AudioDeviceEvent> pollEvent() {
        try(MemoryStack stack = MemoryStack.stackPush()) {

            //ALC_EXT_disconnect
            {
                IntBuffer pConnected = stack.mallocInt(1);
                alcGetIntegerv(device.getHandle(), EXTDisconnect.ALC_CONNECTED, pConnected);
                if (pConnected.get(0) == 0)
                    return Optional.of(new AudioDeviceEvent(AudioDeviceEvent.DeviceChanged));
            }

            for(AudioSource source : sources) {
                IntBuffer pPlayback = stack.mallocInt(1);
                alGetSourcei(source.getHandle(), AL_SOURCE_STATE, pPlayback);

                boolean playing = pPlayback.get(0) == AL_PLAYING;
                if(playing != source.isPlaying()) {
                    source.setPlaying(playing);
                    if(playing)
                        return Optional.of(new AudioDeviceEvent(AudioDeviceEvent.SourcePlayStart));
                    else
                        return Optional.of(new AudioDeviceEvent(AudioDeviceEvent.SourcePlayEnd));
                }
            }




        }

        return Optional.empty();
    }

    @Override
    public void free() {
        alcDestroyContext(handle);
    }
}
