package engine.mio;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {

        String dsl =
               """
               actor "Actor1"
                   actor "Actor2"
                       actor "Actor3"
                           actor "Actor4"
                               "Component4" component(
                                   "Pos" float2(1, 2),
                                   "Vel" float2(3, 4),
                               )
                           end
                       end
                   end
                   
                   actor "Actor5"
                       "Component5" component(
                            "Pos" float2(5, 6),
                            "Vel" float2(7, 8),
                       )
                   end                              
               end
              
               """;

        Analyzer tokenizer = new Analyzer(dsl);

        RDParser parser = new RDParser();
        parser.parseContinuous(tokenizer);

        IRSource ir = parser.getIR();
        for(Object[] frame : ir.getFrames()) {
            System.out.println(Arrays.toString(frame));
        }





        
    }
}