package engine.audio.wav;



import engine.audio.AudioBuffer;
import engine.audio.AudioDevice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class WavUtil {
    private WavUtil() {}

    public static AudioBuffer loadAudioWAV(AudioDevice device, InputStream inputStream){

        AudioInputStream stream;
        try {
            stream = AudioSystem.getAudioInputStream(new BufferedInputStream(inputStream));
        }
        catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
        AudioFormat audioFormat = stream.getFormat();
        WaveData waveData = WaveData.create(stream);

        float durationMs = 1000 * (stream.getFrameLength() / audioFormat.getFrameRate());

        AudioBuffer audioBuffer = new AudioBuffer();

        audioBuffer.setDuration(durationMs);
        audioBuffer.setSampleRate(waveData.samplerate);
        audioBuffer.setFormat(waveData.format);
        audioBuffer.setData(waveData.data);

        return audioBuffer;
    }
}
