package app;

public class Main {
    public static void main(String[] args) {


        Launcher launcher = new Launcher();
        launcher.initLogging();
        launcher.enableDebugOptionsIfAttached();

        launcher.launch(args);
    }
}
