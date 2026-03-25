package app;

import engine.Logger;
import engine.Application;
import engine.Surface;
import engine.Time;
import engine.graphics.Session;
import org.lwjgl.system.Configuration;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;


public class Launcher {


    public void initLogging() {
        Logger.setFileTarget(new File("engine.log"));
    }


    public void launch(String[] args) throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException, MalformedURLException {
        System.setProperty("org.lwjgl.system.stackSize", "128");

        Application application;
        {
            Path path = Path.of(args[0]);
            URL url = path.toUri().toURL();

            System.out.println(url);
            ClassLoader classLoader = new URLClassLoader(new URL[]{url});

            Class clazz = classLoader.loadClass(args[1]);
            Constructor constructor = clazz.getConstructor();
            Object appImpl = constructor.newInstance();
            application = (Application) appImpl;
        }
        Surface surface = Surface.newSurface(application, "SkySOFT Engine", 1920, 1080);
        Session.setSurface(surface);


        application.launch(args, surface);



        while(true){
            boolean success = application.update();
            if(!success) break;
        }


        application.closing();
        application.close();




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
