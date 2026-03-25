package engine.mio;

public class MioCompiler {
    private MioCompiler() {}

    public static IRGen compile(String source) {
        Analyzer tokenizer = new Analyzer(source);

        RDParser parser = new RDParser();
        parser.parseContinuous(tokenizer);

        return parser.getIR();
    }

}
