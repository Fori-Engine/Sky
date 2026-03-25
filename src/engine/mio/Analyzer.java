package engine.mio;

/*
God help you if you're actually trying to understand this lexer
 */
public class Analyzer {
    private String source;
    private int index = 0;
    private int advance;
    public Analyzer(String source) {
        this.source = source;
    }
    public void unexpectedSymbol(char character) {
        throw new RuntimeException("Unexpected character for token (" + character + ")");
    }

    private int line = 1;
    public Token next() {

        boolean string = false, numeric = false;
        Token token = new Token(line);

        advance = index;

        while (index < source.length()) {
            char character = source.charAt(index);
            token.content.append(character);

            //Order indicates match priority!
            //Make sure to update index before emitting and quitting the loop!
            if (character == '\"') {
                if (!string) {
                    string = true;
                    token.type = Token.TokenType.StringLiteral;
                }
                else {
                    string = false;
                    index++;
                    break;
                }
            }
            if(!string) {

                if(character == '\n') line++;
                if(Character.isWhitespace(character)) {
                    index++;
                    token.type = Token.TokenType.Whitespace;
                    break;
                }
                if(character == '(') {
                    if (numeric) numeric = false;
                    token.type = Token.TokenType.LeftParen;
                    index++;
                    break;
                }
                if((Character.isDigit(character) || character == '-') && !Character.isDigit(source.charAt(index - 1)) && !Character.isLetter(source.charAt(index - 1))) {
                    if(!numeric) {
                        token.type = Token.TokenType.Numeric;
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
                    token.type = character == ',' ? Token.TokenType.ArgDelimiter : Token.TokenType.RightParen;
                    index++;
                    break;

                }

                //Match keywords
                {
                    boolean b = false;

                    for (Token.TokenType tokenType : Token.TokenType.values()) {
                        if (token.content.toString().strip().equals(tokenType.value)) {
                            token.type = tokenType;
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
        public TokenType type;
        public enum TokenType {
            LeftParen("("),
            RightParen(")"),
            StringLiteral(null),
            Numeric(null),
            ActorKeyword("actor"),
            EndKeyword("end"),
            DataKeyword("data"),
            PassKeyword("pass"),
            Float1Keyword("float1"),
            Float2Keyword("float2"),
            Float3Keyword("float3"),
            Float4Keyword("float4"),
            Euler3Keyword("euler3"),
            Quat4Keyword("quat4"),
            ArgDelimiter(","),
            LateKeyword("late"),
            TrueKeyword("true"),
            FalseKeyword("false"),
            Whitespace(null);

            public String value;

            TokenType(String value) {
                this.value = value;
            }
        }
        public StringBuilder content = new StringBuilder();
        public boolean foundDecimal = false;
        public int line;
        public Token(int line) {
            this.line = line;
        }
    }
}
