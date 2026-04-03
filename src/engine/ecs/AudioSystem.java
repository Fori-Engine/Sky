package engine.ecs;

import engine.asset.AssetRegistry;
import engine.audio.*;
import engine.audio.wav.WavUtil;
import engine.logging.Logger;
import engine.logging.SkyRuntimeException;
import org.joml.Vector3f;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AudioSystem extends ActorSystem {
    private AudioDevice device;
    private AudioContext context;
    private Thread thread;

    //Volatile thread shared variables
    //private volatile org.joml.Vector3f receiverPos = new Vector3f();
    private volatile float rx, ry, rz;
    private volatile boolean running = true;


    public AudioSystem() {
        device = AudioDevice.getSystemAudioDevice();
        context = new AudioContext(device);

        thread = new Thread(() -> {
            Logger.info(AudioSystem.class, "Acquired device " + device.getName());


            AudioBuffer audioBuffer = null;
            audioBuffer = WavUtil.loadAudioWAV(device, new ByteArrayInputStream((byte[]) AssetRegistry.getAsset("nsp:nspassets/audio/cheering.wav").getObject()));


            AudioSource source = new AudioSource(device);
            {
                source.setPosition(0f, 0f, 0f);
            }

            context.addAudioSource(source);

            source.play(audioBuffer, new AudioPlayback(true));

            while(running) {
                AudioReceiver.getInstance()
                        .setPosition(
                                rx,
                                ry,
                                rz
                        );

                Optional<AudioDeviceEvent> event = context.pollEvent();

                if(event.isPresent()) {


                    if(event.get().hasFlag(AudioDeviceEvent.DeviceChanged)) {
                        System.out.println("Lost " + device.getName());
                        device.free();
                        context.free();

                        device = AudioDevice.getSystemAudioDevice();
                        context = new AudioContext(device);

                        System.out.println("Acquired device " + device.getName());
                        System.out.println(device.isStereo());

                    }
                    else if(event.get().hasFlag(AudioDeviceEvent.SourcePlayStart)) {
                        System.out.println("Started playing");
                    }
                    else if(event.get().hasFlag(AudioDeviceEvent.SourcePlayEnd)) {
                        System.out.println("Ended playing");
                        break;
                    }
                }


            }
        });
        thread.start();

    }
    @Override
    public void run(Actor root) {
        root.previsitAllActors(actor -> {

            if(actor.has(AudioReceiverComponent.class)) {
                AudioReceiverComponent audioReceiverComponent = actor.getComponent(AudioReceiverComponent.class);
                org.joml.Vector3f pos = (audioReceiverComponent.position());
                rx = pos.x;
                ry = pos.y;
                rz = pos.z;
            }



        });

    }

    @Override
    public void dispose() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new SkyRuntimeException(e);
        }
        context.free();
        device.free();
    }
}
