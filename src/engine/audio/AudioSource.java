package engine.audio;

import static org.lwjgl.openal.AL11.*;

public class AudioSource implements ALResourceHolder {
    private int handle;
    private float x, y, z;
    private boolean playing;


    public AudioSource(AudioDevice device) {
        handle = alGenSources();
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;

        alSource3f(handle, AL_POSITION, x, y, z);
    }

    protected boolean isPlaying() {
        return playing;
    }

    protected void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public void play(AudioBuffer audioBuffer, AudioPlayback playback) {
        alSourcei(handle, AL_LOOPING, playback.isLoop() ? AL_TRUE : AL_FALSE);
        alSourcei(handle, AL_BUFFER, audioBuffer.getHandle());
        alSourcePlay(handle);
    }

    @Override
    public void free() {
        alDeleteSources(handle);
    }

    public int getHandle() {
        return handle;
    }
}
