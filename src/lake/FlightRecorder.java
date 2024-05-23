package lake;

import java.io.*;
import java.util.Date;

public class FlightRecorder {

    private static PrintWriter writer = new PrintWriter(System.out);
    private static boolean enabled;
    private FlightRecorder(){}

    public static void useFile(File file){

        if(enabled) {

            try {
                writer = new PrintWriter(new FileOutputStream(file));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void useOutputStream(OutputStream outputStream){
        if(enabled) writer = new PrintWriter(outputStream);
    }

    public static void setEnabled(boolean b){
        enabled = b;
    }

    private static String timeStr(){
        return new Date().toString();
    }

    private static void logGeneric(String source, String type, String message){
        if(enabled) {
            writer.println("[" + timeStr() + "] " + source + " " + "(" + type + ") " + message);
            writer.flush();
        }
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
