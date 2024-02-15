package plc.project;

import javax.swing.text.html.Option;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (!tokens.has(0)) throw new ParseException("Dude there's no tokens!", 0);
        if (peek("LET")) throw new ParseException("Invalid Token", 0);      // TODO p2b
        if (peek("SWITCH")) throw new ParseException("Invalid Token", 0);   // TODO p2b
        if (peek("IF")) throw new ParseException("Invalid Token", 0);       // TODO p2b
        if (peek("WHILE")) throw new ParseException("Invalid Token", 0);    // TODO p2b
        if (peek("RETURN")) throw new ParseException("Invalid Token", 0);   // TODO p2b

        Ast.Expression expr1 = parseExpression();



        if (match("=") && tokens.has(0)) {
            Ast.Expression expr2 = parseExpression();
            if (match(";"))
                return new Ast.Statement.Assignment(expr1, expr2);
            else
                throw new ParseException("Invalid Token", tokens.index);
        }
        if (match(";"))
            return new Ast.Statement.Expression(expr1);

        throw new ParseException("Invalid Token", tokens.index);
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        if (tokens.has(0))
            return parseLogicalExpression();
        throw new ParseException("Dude there's no tokens!", 0);
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        if (!tokens.has(0))
            throw new ParseException("Dude there's no tokens!", 0);

        Ast.Expression[] expr = {parseComparisonExpression(), null};

        if (!tokens.has(0))
            return expr[0];

        int ind = 1;

        while (peek("&&") || peek("||"))
        {
            String op = tokens.get(0).getLiteral();
            tokens.advance();
            if (!tokens.has(0)) throw new ParseException("Dude there's no tokens!", 0);
            expr[ind % 2] = new Ast.Expression.Binary(op, expr[++ind % 2], parseComparisonExpression());
        }

        return expr[1] == null ? expr[0] : expr[++ind % 2];
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        if (!tokens.has(0))
            throw new ParseException("Dude there's no tokens!", tokens.index + tokens.get(-1).getLiteral().length() - 1);

        Ast.Expression[] expr = {parseAdditiveExpression(), null};

        int ind = 1;
        while (peek("<") || peek(">") || peek("==") || peek("!="))
        {
            String op = tokens.get(0).getLiteral();
            tokens.advance();
            if (!tokens.has(0)) throw new ParseException("Dude there's no tokens!", 0);
            expr[ind % 2] = new Ast.Expression.Binary(op, expr[++ind % 2], parseAdditiveExpression());
        }

        return expr[1] == null ? expr[0] : expr[++ind % 2];
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        if (!tokens.has(0)) throw new ParseException("Dude there's no tokens!", 0);

        Ast.Expression[] expr = {parseMultiplicativeExpression(), null};

        int ind = 1;
        while (peek("+") || peek("-"))
        {
            String op = tokens.get(0).getLiteral();
            tokens.advance();
            if (!tokens.has(0)) throw new ParseException("Invalid Token", 0);
            expr[ind % 2] = new Ast.Expression.Binary(op, expr[++ind % 2], parseMultiplicativeExpression());
        }

        return expr[1] == null ? expr[0] : expr[++ind % 2];
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        if (!tokens.has(0)) throw new ParseException("Dude there's no tokens!", 0);

        Ast.Expression[] expr = {parsePrimaryExpression(), null};

        int ind = 1;

        while (peek("*") || peek("/") || peek("^"))
        {
            String op = tokens.get(0).getLiteral();
            tokens.advance();
            if (!tokens.has(0)) throw new ParseException("Dude there's no tokens!", 0);
            expr[ind % 2] = new Ast.Expression.Binary(op, expr[++ind % 2], parsePrimaryExpression());
        }

        return expr[1] == null ? expr[0] : expr[++ind % 2];
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (!tokens.has(0))       throw new ParseException("Dude there's no tokens!", 0);
        if (peek(Token.Type.INTEGER))   return new Ast.Expression.Literal(new BigInteger(tokens.get(0).getLiteral()));
        if (peek(Token.Type.DECIMAL))   return new Ast.Expression.Literal(new BigDecimal(tokens.get(0).getLiteral()));
        if (peek(Token.Type.CHARACTER)) {
            String con = tokens.get(0).getLiteral();
            con = con.substring(1, con.length()-1).replace("\\n","\n");
            return new Ast.Expression.Literal(con.charAt(0));
        }
        if (peek(Token.Type.STRING))    {
            String con = tokens.get(0).getLiteral();
            con = con.substring(1, con.length()-1).replace("\\n","\n");
            return new Ast.Expression.Literal(con);
        }
        if (peek("NIL"))
            return new Ast.Expression.Literal(null);
        if (peek("TRUE"))
            return new Ast.Expression.Literal(true);
        if (peek("FALSE"))
            return new Ast.Expression.Literal(false);
        if (match("(") && tokens.has(0)) {
            Ast.Expression expr = parseExpression();
            if (match(")"))
                return new Ast.Expression.Group(expr);
            else {
                int len = tokens.get(-1).getLiteral().length();
                throw new ParseException("Invalid Token", tokens.index + len - 1);
            }
        }

        if (!peek(Token.Type.IDENTIFIER)) throw new ParseException("Invalid Token", tokens.index); //TODO

        String id = tokens.get(0).getLiteral();
        tokens.advance();

        if (match("(", ")"))   return new Ast.Expression.Function(id, Arrays.asList());


        if (match("(") && tokens.has(0)) {
            List<Ast.Expression> exprArr = new ArrayList<>();
            exprArr.add(parseExpression());

            while (match(",") && tokens.has(0)) exprArr.add(parseExpression());

            if (match(")"))    return new Ast.Expression.Function(id, exprArr);
            else throw new ParseException("Invalid Token", tokens.index);
        }

        if (match("[") && tokens.has(0)) {
            Ast.Expression expr = parseExpression();
            if (match("]"))    return new Ast.Expression.Access(Optional.of(expr), id);
            else throw new ParseException("Invalid Token", tokens.index);
        }

        return new Ast.Expression.Access(Optional.empty(), id);
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) { // DONE (in lecture)
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) { // DONE (in lecture)
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }

        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}

