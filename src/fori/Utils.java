package fori;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Utils {
    public static String exceptionToString(Throwable e){
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();



        return exceptionAsString;
    }
}
