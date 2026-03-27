package engine.mio;

public class MioCompiler {
    private MioCompiler() {}

    public static IRGen compile(String source) {
        Analyzer tokenizer = new Analyzer(source);

        RDParser parser = new RDParser();
        parser.parseContinuous(tokenizer);

        return parser.getIR();
    }

    public static void printTokens(String s) {
        Analyzer tokenizer = new Analyzer(s);
        Analyzer.Token t = null;

        while((t = tokenizer.next()) != null) {
            if(t.type != Analyzer.Token.TokenType.Whitespace) {
                System.out.println(t.type + "| " + t.content.toString());
            }
        }

    }
}
