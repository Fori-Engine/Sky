package engine.seri;

public class Main {
    public static void main(String[] args) {

        String dsl =
               """
               actor "Actor1"
                   actor "Actor2"
                   "Component" component(
                       "MyFloat" float1(1.0),
                       "MyFloat2" float1(0.0),
                       "SomeProperty" euler3(1.0, 1.0, 1.0)
                   )
                   
                   end               
               end
               """;

        Analyzer tokenizer = new Analyzer(dsl);
        RecursiveDescentParser recursiveDescentParser = new RecursiveDescentParser();
        recursiveDescentParser.parse(tokenizer);

        
    }
}