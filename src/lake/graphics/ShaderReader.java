package lake.graphics;

import java.util.Scanner;

public class ShaderReader {

    public static ShaderSources readCombinedVertexFragmentSources(String input){

        StringBuilder vertexShader = new StringBuilder();
        StringBuilder fragmentShader = new StringBuilder();
        Scanner scanner = new Scanner(input);

        int shaderStage = 0;
        int vertexStage = 1;
        int fragmentStage = 2;



        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();



            if(line.equals("#type vertex")){
                shaderStage = vertexStage;
                continue;
            }
            else if(line.equals("#type fragment")){
                shaderStage = fragmentStage;
                continue;
            }


            if(shaderStage == vertexStage){
                vertexShader.append(line + "\n");
            }
            else if(shaderStage == fragmentStage){
                fragmentShader.append(line + "\n");
            }
        }
        scanner.close();

        return new ShaderSources(vertexShader.toString(), fragmentShader.toString());
    }






    public static class ShaderSources {
        public String vertexShader;
        public String fragmentShader;


        public ShaderSources(String vertexShader, String fragmentShader) {
            this.vertexShader = vertexShader;
            this.fragmentShader = fragmentShader;
        }
    }

}
