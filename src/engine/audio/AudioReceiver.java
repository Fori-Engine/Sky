package engine.audio;

import static org.lwjgl.openal.AL11.*;
public class AudioReceiver {
    private static AudioReceiver instance;
    private float x, y, z;
    private float vx, vy, vz;


    private AudioReceiver() {}
    public static AudioReceiver getInstance() {
        if(instance == null) instance = new AudioReceiver();

        return instance;
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;

        alListener3f(AL_POSITION, x, y, z);
    }

    public void setVelocity(float vx, float vy, float vz) {
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;

        alListener3f(AL_VELOCITY, vx, vy, vz);
    }

}
