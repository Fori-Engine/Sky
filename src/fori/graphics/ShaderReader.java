package fori.graphics;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ShaderReader {

    public static ShaderSources read(String shadersSources){

        ShaderSources shaderSources = new ShaderSources();

        Scanner scanner = new Scanner(shadersSources);

        ShaderType shaderType = null;



        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();



            if(line.equals("#type vertex")){
                shaderType = ShaderType.Vertex;
                shaderSources.sources.put(ShaderType.Vertex, new StringBuilder());
                continue;
            }
            else if(line.equals("#type fragment")){
                shaderType = ShaderType.Fragment;
                shaderSources.sources.put(ShaderType.Fragment, new StringBuilder());
                continue;
            }
            else if(line.equals("#type compute")){
                shaderType = ShaderType.Compute;
                shaderSources.sources.put(ShaderType.Compute, new StringBuilder());
                continue;
            }

            shaderSources.sources.get(shaderType).append(line + "\n");

        }
        scanner.close();



        return shaderSources;
    }






    public static class ShaderSources {
        private Map<ShaderType, StringBuilder> sources = new HashMap<>();

        public ShaderSources() {

        }

        public String getShaderSource(ShaderType shaderType) {
            return sources.get(shaderType).toString();
        }
    }

}
