// Tony Wong and Shreyas Adireddy

package plc.project;
import java.util.ArrayList;
import java.util.List;
/**
 * The lexer works through three main functions:
 *
 * - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 * - {@link #lexToken()}, which lexes the next token
 * - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are *
 helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {
    private final CharStream chars;
    public Lexer(String input) {
        chars = new CharStream(input);
    }
    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();
        while (chars.has(0)) {
            if (!peek("[ \b\n\r\t\f\u000B]")) {  // Skip whitespace
                tokens.add(lexToken());
            } else {
                chars.advance();
                chars.skip();
            }
        }
        return tokens; //TODO
    }
    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    String identifier = "('@'| [A-Za-z] ) [A-Za-z0-9_-]*";
    String integer = "('0' | '-'? [1-9]) [0-9]*";
    String decimal = "'-'? ('0' | [1-9] [0-9]*) '.' [0-9]+";
    String escape = "'\\' [bnrtf'\"\\]";
    String character = "['] ([^'\\n\\r\\\\] | '\\' [bnrt'\"\\\\]) [']";
    String string = "'\"' ([^\"\\n\\r\\\\] | '\\' [bnrt'\"\\\\])* '\"'";
    String operator = "[=!] '='? | '&&' | '||' | 'any character'";
    String whitespace = "[\b\n\r\t] ";
    public Token lexToken() {
        if (match("[@A-Za-z]")) {
            return lexIdentifier();
        } if (peek("0|[1-9]") || peek("0", "\\.") || peek("-", "0|[1-9]")) {
            return lexNumber();
        } else if (match("'")) {
            return lexCharacter();
        } else if (peek("\"")) {
            return lexString();
        } else {
            return lexOperator();
        }  //TODO
    }
    public Token lexIdentifier() {
        while (match("[A-Za-z0-9_-]")) continue;

        if (peek("@"))
            throw new ParseException("Multiple @s in Identifier", chars.index);
        return chars.emit(Token.Type.IDENTIFIER); //TODO
    }
    public Token lexNumber() {
        if (match("-")) {
            if (peek("[1-9]")){
                chars.advance();
            }
            else if (peek("0", "\\.")){
                chars.advance();
            }else{
                throw new ParseException("Invalid Number", chars.index);
            }
        }
        if (peek("0", "[0-9]")) {
            throw new ParseException("Invalid Number", chars.index);
        }
        while (peek("[0-9]")) {
            chars.advance();
        }
        if (peek("\\.")) {
            if (peek("\\.", "[^0-9]"))
                return chars.emit(Token.Type.INTEGER);
            chars.advance();
            if (!peek("[0-9]")){
                throw new ParseException("Invalid character", chars.index);
            }
            while (peek("[0-9]")) {
                chars.advance();
            }
            return chars.emit(Token.Type.DECIMAL);
        }
        return chars.emit(Token.Type.INTEGER);
    }
    public Token lexCharacter() {
        if (peek("[\n\r\b\t\0']")) {
            throw new ParseException("Invalid character", chars.index);
        }
        else if (match("\\\\")) {
            lexEscape();
        } else {
            chars.advance();
        }

        if (!match("'")){
            throw new ParseException("Invalid character", chars.index);
        }
        return chars.emit(Token.Type.CHARACTER); //TODO
    }
    public Token lexString() {
        match("\"");
        while (!peek("\"") && chars.has(0)) {
            if (peek("[\n\r]")) {
                throw new ParseException("Unterminated string", chars.index);
            }
            if (match("\\\\")) {
                lexEscape();
            } else {
                chars.advance();
            }
        }
        if (!match("\"")) {
            throw new ParseException("Unterminated string", chars.index);
        };
        return chars.emit(Token.Type.STRING); //TODO
    }
    public void lexEscape() {
        if(!match("[bnrtf'\"\\\\]")){
            throw new ParseException("Invalid Escape Character", chars.index);
        }; //TODO
    }
    public Token lexOperator() {
        if (peek("[^ \t\b\n\r]")) {
            if (match("!") || match("=")) {
                match("=");
                return chars.emit(Token.Type.OPERATOR);
            }
            if (match("&", "&") || match("\\|", "\\|")) {
                return chars.emit(Token.Type.OPERATOR);
            }
            chars.advance();
            return chars.emit(Token.Type.OPERATOR);
        } else {
            throw new ParseException("Invalid operator", chars.index);
        } //TODO
    }
    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i=0; i < patterns.length; i++) {
            if (!chars.has(i)){
                return false;
            }if (!String.valueOf(chars.get(i)).matches(patterns[i])){
                return false;
            }
        }
        return true; //TODO (in Lecture)
    }
    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);
        if (peek){
            for (int i = 0; i < patterns.length; i++){
                chars.advance();
            }
        }
        return peek; //TODO (in Lecture)
    }
    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {
        private final String input;
        private int index = 0;
        private int length = 0;
        public CharStream(String input) {
            this.input = input;
        }
        public boolean has(int offset) {
            return index + offset < input.length();
        }
        public char get(int offset) {
            return input.charAt(index + offset);
        }
        public void advance() {
            index++;
            length++;
        }
        public void skip() {
            length = 0;
        }
        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }
    }
}
