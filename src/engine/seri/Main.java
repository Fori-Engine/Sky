package engine.seri;

public class Main {
    public static void main(String[] args) {

        String dsl =
               """
               actor "Actor1"
                   actor "Actor2"
                   "Component" component(
                       "MyFloat" float2(1.0, 1.0),
                     
                   )
                   
                                  
               end
               """;

        Analyzer tokenizer = new Analyzer(dsl);
//
        //Analyzer.Token token;
        //while ((token = tokenizer.next()) != null) {
        //        if(!token.type.equals("whitespace")) System.out.println(token.type + "| " + token.content.toString());
        //    }

        RecursiveDescentParser recursiveDescentParser = new RecursiveDescentParser();
        recursiveDescentParser.parse(tokenizer);

        
    }
}