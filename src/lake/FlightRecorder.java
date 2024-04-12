package lake;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;

public class FlightRecorder {

    private static PrintWriter writer = new PrintWriter(System.out);
    private FlightRecorder(){}

    private static String timeStr(){
        return new Date().toString();
    }

    private static void logGeneric(String source, String type, String message){
        writer.println("[" + timeStr() + "] " + source + " " + "(" + type + ") " + message);
        writer.flush();
    }

    public static void info(Class source, String message){
        logGeneric(source.getSimpleName(), "info", message);
    }
    public static void error(Class source, String message){
        logGeneric(source.getSimpleName(), "error", message);
    }
    public static void meltdown(Class source, String message){
        logGeneric(source.getSimpleName(), "!!MELTDOWN!!", message);
    }

    public static void todo(Class source, String message){
        logGeneric(source.getSimpleName(), "todo", message);
    }



}
