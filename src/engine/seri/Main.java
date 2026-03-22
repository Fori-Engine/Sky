package engine.seri;

public class Main {
    public static void main(String[] args) {

        String dsl =
               """
               actor "Actor1"
                   actor "Actor2"
                   "Component" tuple(
                   
                   )
                   
                   end               
               end
               """;

        Analyzer tokenizer = new Analyzer(dsl);
        RecursiveDescentParser recursiveDescentParser = new RecursiveDescentParser();
        recursiveDescentParser.parse(tokenizer);

        
    }
}