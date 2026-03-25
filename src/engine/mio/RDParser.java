package engine.mio;

import java.util.Arrays;
import java.util.Stack;

public class RDParser {

    public RDParser() {}
    private IRSource ir = new IRSource();

    private Analyzer.Token nextProperToken(Analyzer analyzer) {
        Analyzer.Token token;
        while ((token = analyzer.next()) != null) {
            if(!token.type.equals("whitespace")) {
                return token;
            }
        }
        return null;
    }

    private Analyzer.Token expect(Analyzer analyzer, String... types) {
        Analyzer.Token token = nextProperToken(analyzer);
        if(token == null) {
            throw new RuntimeException("Expected any of " + Arrays.asList(types) + " next instead of EOF");
        }

        for(String type : types) {
            if(token.type.equals(type)) return token;
        }
        throw new RuntimeException("Expected any of " + Arrays.asList(types) + " next instead of " + token.type + " (" + token.content.toString() + ") on line " + token.line);
    }

    public IRSource getIR() {
        return ir;
    }

    private Stack<Analyzer.Token> tokens = new Stack<>();
    public void parseContinuous(Analyzer analyzer){
        while(true) {
            if(!parseSpecific(analyzer)) break;
        }
        if(!tokens.isEmpty()) throw new RuntimeException("An actor declaration is missing a matching 'end'");
    }
    public boolean parseSpecific(Analyzer analyzer) {
        Analyzer.Token token = nextProperToken(analyzer);
        if(token == null) return false;

        switch (token.type) {
            case "actor_keyword": {
                Analyzer.Token next = expect(analyzer, "string_literal");
                tokens.push(next);
                ir.addFrame(new Object[]{
                        IRSource.ACTOR_ADD,
                        next.content.toString(),
                });
                parseSpecific(analyzer);
                break;
            }
            case "end_keyword": {
                tokens.pop();
                ir.addFrame(new Object[]{
                        IRSource.ACTOR_END,
                        "",
                });
                break;
            }
            case "pass_keyword": {
                break;
            }

            case "string_literal": {
                //Strings can also be preceded by 'actor_keyword'
                Analyzer.Token next = expect(analyzer, "component_keyword", "float1_keyword", "float2_keyword", "float3_keyword", "float4_keyword", "euler3_keyword", "quat4_keyword");
                switch (next.type) {
                    case "component_keyword": {
                        ir.addFrame(new Object[]{
                                IRSource.COMPONENT_ADD,
                                token.content.toString(),
                        });
                        parseContinuous(analyzer);
                        break;
                    }
                    case "float1_keyword": {
                        expect(analyzer, "left_paren");
                        Analyzer.Token a1 = expect(analyzer, "numeric");
                        expect(analyzer, "right_paren");
                        expect(analyzer, "arg_delimiter", "right_paren");
                        ir.addFrame(new Object[]{
                                IRSource.PROP_ADD,
                                token.content.toString(),
                                Float.parseFloat(a1.content.toString())
                        });
                        break;
                    }

                    case "float2_keyword": {
                        expect(analyzer, "left_paren");
                        Analyzer.Token a1 = expect(analyzer, "numeric");
                        expect(analyzer, "arg_delimiter");
                        Analyzer.Token a2 = expect(analyzer, "numeric");
                        expect(analyzer, "right_paren");
                        expect(analyzer, "arg_delimiter", "right_paren");
                        ir.addFrame(new Object[]{
                                IRSource.PROP_ADD,
                                token.content.toString(),
                                Float.parseFloat(a1.content.toString()),
                                Float.parseFloat(a2.content.toString())
                        });
                        break;
                    }

                    case "float3_keyword", "euler3_keyword": {
                        expect(analyzer, "left_paren");
                        Analyzer.Token a1 = expect(analyzer, "numeric");
                        expect(analyzer, "arg_delimiter");
                        Analyzer.Token a2 = expect(analyzer, "numeric");
                        expect(analyzer, "arg_delimiter");
                        Analyzer.Token a3 = expect(analyzer, "numeric");
                        expect(analyzer, "right_paren");
                        expect(analyzer, "arg_delimiter", "right_paren");
                        ir.addFrame(new Object[]{
                                IRSource.PROP_ADD,
                                token.content.toString(),
                                Float.parseFloat(a1.content.toString()),
                                Float.parseFloat(a2.content.toString()),
                                Float.parseFloat(a3.content.toString())
                        });

                        break;
                    }

                    case "float4_keyword", "quat4_keyword": {
                        expect(analyzer, "left_paren");
                        Analyzer.Token a1 = expect(analyzer, "numeric");
                        expect(analyzer, "arg_delimiter");
                        Analyzer.Token a2 = expect(analyzer, "numeric");
                        expect(analyzer, "arg_delimiter");
                        Analyzer.Token a3 = expect(analyzer, "numeric");
                        expect(analyzer, "arg_delimiter");
                        Analyzer.Token a4 = expect(analyzer, "numeric");
                        expect(analyzer, "right_paren");
                        expect(analyzer, "arg_delimiter", "right_paren");
                        ir.addFrame(new Object[]{
                                IRSource.PROP_ADD,
                                token.content.toString(),
                                Float.parseFloat(a1.content.toString()),
                                Float.parseFloat(a2.content.toString()),
                                Float.parseFloat(a3.content.toString()),
                                Float.parseFloat(a4.content.toString())
                        });
                        break;
                    }

                }

                break;
            }






        }

        return true;
    }

}
