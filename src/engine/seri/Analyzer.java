package engine.seri;

/*
God help you if you're actually trying to understand this lexer
 */
public class Analyzer {
    private String source;
    private int index = 0;
    public static String[] keywords = {"actor", "end", "component", "quat4", "float4", "float3", "float2", "float1", "late", "true", "false", "euler3", "pass"};
    private int advance;
    public Analyzer(String source) {
        this.source = source;
    }
    public void unexpectedSymbol(char character) {
        throw new RuntimeException("Unexpected character for token (" + character + ")");
    }

    public Token next() {

        boolean string = false, numeric = false;
        Token token = new Token();

        advance = index;

        while (index < source.length()) {
            char character = source.charAt(index);
            token.content.append(character);

            //Order indicates match priority!
            //Make sure to update index before emitting and quitting the loop!
            if (character == '\"') {
                if (!string) {
                    string = true;
                    token.type = "string_literal";
                }
                else {
                    string = false;
                    index++;
                    break;
                }
            }
            if(!string) {

                if(Character.isWhitespace(character)) {
                    index++;
                    token.type = "whitespace";
                    break;
                }
                if(character == '(') {
                    if (numeric) numeric = false;
                    token.type = "left_paren";
                    index++;
                    break;
                }
                if((Character.isDigit(character) || character == '-') && !Character.isDigit(source.charAt(index - 1)) && !Character.isLetter(source.charAt(index - 1))) {
                    if(!numeric) {
                        token.type = "numeric";
                        numeric = true;
                    }
                }
                if(character == '.') {
                    if(numeric) {
                        if (!token.foundDecimal) token.foundDecimal = true;
                        else unexpectedSymbol(character);
                    }


                }
                if(character == ',' || character == ')') {
                    if(numeric) {
                        token.content.deleteCharAt(token.content.length() - 1);
                        break;
                    }
                    token.type = character == ',' ? "arg_delimiter" : "right_paren";
                    index++;
                    break;

                }

                //Match keywords
                {
                    boolean b = false;
                    for (String keyword : keywords) {
                        if (token.content.toString().strip().equals(keyword)) {
                            token.type = keyword + "_keyword";
                            b = true;
                            break;
                        }
                    }
                    if (b) {
                        index++;
                        break;
                    }
                }
            }





            index++;
        }

        advance = advance - index;

        return token.type == null ? null : token;
    }

    public void rewind() {
        index -= advance;
    }

    public class Token {
        public String type;
        public StringBuilder content = new StringBuilder();
        public boolean foundDecimal = false;

        public Token() { }
    }
}
