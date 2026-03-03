package engine.audio;

public class AudioPlayback {
    private boolean loop;

    public AudioPlayback(boolean loop) {
        this.loop = loop;
    }

    public boolean isLoop() {
        return loop;
    }
}
