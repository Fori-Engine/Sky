package engine.seri;

import java.util.Arrays;

public class RecursiveDescentParser {

    public RecursiveDescentParser() {}

    private Analyzer.Token nextProperToken(Analyzer analyzer) {
        Analyzer.Token token;
        while ((token = analyzer.next()) != null) {
            if(!token.type.equals("whitespace")) {
                return token;
            }
        }
        return null;
    }

    private Analyzer.Token expect(Analyzer analyzer, boolean lenient, String... types) {
        Analyzer.Token token = nextProperToken(analyzer);
        if(token == null) {
            throw new RuntimeException("Expected any of " + Arrays.asList(types) + " next instead of EOF");
        }

        for(String type : types) {
            if(token.type.equals(type)) return token;
        }
        if(lenient)
            return null;
        throw new RuntimeException("Expected any of " + Arrays.asList(types) + " next instead of " + token.type + " (" + token.content.toString() + ")");
    }



    public void parse(Analyzer analyzer) {
        Analyzer.Token token = nextProperToken(analyzer);

        switch (token.type) {
            case "actor_keyword": {
                expect(analyzer, false, "string_literal");
                parse(analyzer);
                expect(analyzer, false, "end_keyword");
                break;
            }
            case "pass_keyword": {
                System.out.println("Empty block?");
                break;
            }

            case "string_literal": {
                //Strings can also be preceded by 'actor_keyword'
                Analyzer.Token next = expect(analyzer, false, "component_keyword", "float1_keyword", "float2_keyword", "float3_keyword", "float4_keyword", "euler3_keyword", "quat4_keyword");
                if(next != null) {
                    switch (next.type) {
                        case "component_keyword": {
                            expect(analyzer, false, "left_paren");
                            parse(analyzer);
                            expect(analyzer, false,"right_paren");
                            break;
                        }
                        case "float1_keyword": {
                            expect(analyzer, false, "left_paren");
                            Analyzer.Token value = expect(analyzer, false, "numeric");
                            expect(analyzer, false,"right_paren");
                            expect(analyzer, false, "arg_delimiter", "right_paren");
                            break;
                        }

                        case "float2_keyword": {
                            expect(analyzer, false, "left_paren");
                            expect(analyzer, false, "numeric");
                            expect(analyzer, false,"arg_delimiter");
                            expect(analyzer, false, "numeric");
                            expect(analyzer, false,"right_paren");
                            expect(analyzer, false, "arg_delimiter", "right_paren");
                            break;
                        }

                        case "float3_keyword", "euler3_keyword" : {
                            expect(analyzer, false, "left_paren");
                            expect(analyzer, false, "numeric");
                            expect(analyzer, false,"arg_delimiter");
                            expect(analyzer, false, "numeric");
                            expect(analyzer, false,"arg_delimiter");
                            expect(analyzer, false, "numeric");
                            expect(analyzer, false,"right_paren");
                            expect(analyzer, false, "arg_delimiter", "right_paren");
                            break;
                        }

                        case "float4_keyword", "quat4_keyword" : {
                            expect(analyzer, false, "left_paren");
                            expect(analyzer, false, "numeric");
                            expect(analyzer, false,"arg_delimiter");
                            expect(analyzer, false, "numeric");
                            expect(analyzer, false,"arg_delimiter");
                            expect(analyzer, false, "numeric");
                            expect(analyzer, false,"arg_delimiter");
                            expect(analyzer, false, "numeric");
                            expect(analyzer, false,"right_paren");
                            expect(analyzer, false, "arg_delimiter", "right_paren");
                            break;
                        }

                    }

                }
                break;
            }






        }

    }

}
