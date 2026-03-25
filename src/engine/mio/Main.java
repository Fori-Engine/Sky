package engine.mio;

public class Main {
    public static void main(String[] args) {

        IRGen ir = MioCompiler.compile("""
               actor "Actor1"
                   actor "Actor2"
                       actor "Actor3"
                           actor "Actor4"
                               "Component4" data("Pos" float2(1, 2), "Vel" float2(3, 4))
                           end
                       end
                   end
                   #This is a comment in MioDSL
                   actor "Actor5"
                       "Component5" data("Pos" float2(-5, 6), "Vel" float2(7, 8))
                   end
               end
               """);

        for(Instruction frame : ir.getList()) {
            System.out.println(frame);
        }





        
    }
}