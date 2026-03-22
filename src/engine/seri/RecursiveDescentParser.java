package engine.seri;

public class RecursiveDescentParser {

    public RecursiveDescentParser() {}

    private Analyzer.Token nextProperToken(Analyzer analyzer) {
        Analyzer.Token token;
        while ((token = analyzer.next()) != null) {
            if(!token.type.equals("whitespace")) return token;
        }
        return null;
    }

    private Analyzer.Token expect(Analyzer analyzer, String type) {
        Analyzer.Token token = nextProperToken(analyzer);
        if(token == null) return null;

        if(!token.type.equals(type)) throw new RuntimeException("Expected " + type + " next");
        return token;
    }


    public void parse(Analyzer analyzer) {
        Analyzer.Token token;


        while((token = analyzer.next()) != null) {
            switch (token.type) {
                case "actor_keyword": {
                    expect(analyzer, "string_literal");
                    parse(analyzer);

                    expect(analyzer, "end_keyword");
                }
                case "string_literal": {
                    expect(analyzer, "tuple_keyword");
                    expect(analyzer, "left_paren");
                    //parse(analyzer);
                    expect(analyzer, "right_paren");
                }



            }

        }

    }

}
