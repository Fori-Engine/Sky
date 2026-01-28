package app;

import engine.Logger;
import engine.Stage;
import engine.Surface;
import org.lwjgl.system.Configuration;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.List;


public class Launcher {


    public void initLogging() {
        Logger.setFileTarget(new File("engine.log"));
    }


    public void launch(String[] args) {

        Stage stage = new ExampleStage();
        Surface surface = Surface.newSwingSurface(stage, "SkySOFT Editor", 1920, 1080); //Surface.newSurface(stage, "SkySOFT Engine", 1920, 1080);

        stage.launch(args, surface);


        while(true){
            boolean success = stage.update();
            System.out.println("Tick");



            if(!success) break;
        }

        stage.closing();
        stage.close();




    }

    private boolean isRunningInDebug() {
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();

        boolean isRunningInDebug = false;

        for(String arg : jvmArgs){
            if(arg.contains("jdwp=")) return true;
        }

        return isRunningInDebug;
    }

    public void enableDebugOptionsIfAttached() {
        if(isRunningInDebug()) {
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);

            Logger.info(Launcher.class, "A debugger is attached over JDWP. LWJGL memory allocations will ONLY appear on the console");
        }
    }

    public void logPlatformInfo() {
        Logger.info(Launcher.class,
                "JVM info: " + System.getProperty("java.vendor") + " " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version") +
                        " [" + System.getProperty("os.name") + " " + System.getProperty("os.arch") + "]"
        );
    }
}
