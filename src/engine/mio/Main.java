package engine.mio;

public class Main {
    public static void main(String[] args) {

        long start = System.currentTimeMillis();
        IRGen ir = MioCompiler.compile("""
                actor "MySpotlight"
                    "SpotlightComponent" data(
                        "fovDeg" float1(15),
                        "eye" float3(-4.0, 10.0, 1.0),
                        "center" float3(0.0, 0.0, 0.0),
                        "up" float3(0.0, 1.0, 0.0),
                        "aspectRatio" float1(1.0),
                        "zNear" float1(0.1),
                        "zFar" float1(10.0),
                        "zZeroToOne" bool(true),
                        "invertY" bool(true)
                    )
                end
               """);

        System.out.println("[" + (System.currentTimeMillis() - start) + " ms]");
        for(Instruction frame : ir.getList()) {
            System.out.println(frame);
        }





        
    }
}