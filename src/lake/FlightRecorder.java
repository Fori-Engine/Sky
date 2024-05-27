package lake;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class FlightRecorder {

    private static PrintWriter writer = new PrintWriter(System.out);
    private static boolean enabled;
    private FlightRecorder(){}
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED_BACKGROUD = "\u001b[41m";
    public static final String ANSI_WHITE = "\u001b[37m";





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

        return new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    private static void logGeneric(String ansiColor, String source, String type, String message){
        if(enabled) {
            writer.println(ansiColor + " [" + timeStr() + " " + type + " " + source + "] " + message + ANSI_RESET);
            writer.flush();
        }
    }

    public static void info(Class source, String message){
        logGeneric(ANSI_WHITE, source.getSimpleName(), "INFO", message);
    }
    public static void error(Class source, String message){
        logGeneric(ANSI_RED, source.getSimpleName(), "ERROR", message);
    }
    public static void meltdown(Class source, String message){
        logGeneric(ANSI_RED_BACKGROUD + ANSI_BLACK, source.getSimpleName(), "!!MELTDOWN!!", message);
    }

    public static void todo(Class source, String message){
        logGeneric(ANSI_YELLOW, source.getSimpleName(), "TODO", message);
    }



}
