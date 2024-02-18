package plc.project;

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
        List<Ast.Global> globals = new ArrayList<>();
        List<Ast.Function> functions = new ArrayList<>();

        while (tokens.has(0)) {
            if (peek("LIST") || peek("VAR") || peek("VAL")) {
                globals.add(parseGlobal());
            } else if (peek("FUN")) {
                functions.add(parseFunction());
            } else {
                throw new ParseException("Invalid Token", tokens.get(0).getIndex());
            }
        }

        return new Ast.Source(globals, functions);
    }

    /**
     * Parses the {@code global} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        if (peek("LIST")) {
            return parseList();
        } else if (peek("VAR")) {
            return parseMutable();
        } else if (peek("VAL")){
            return parseImmutable();
        }else {
            throw new ParseException("Expected LIST or VAR or VAL", tokens.get(0).getIndex());
        }
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        match("LIST");
        if (!tokens.has(0)){
            throw new ParseException("Invalid token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }if (!peek(Token.Type.IDENTIFIER)){
            throw new ParseException("Invalid token", tokens.get(0).getIndex());
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();
        if (!match("=") || !match("[")){
            throw new ParseException("Invalid token", tokens.get(0).getIndex() + tokens.get(0).getLiteral().length());
        }
        List<Ast.Expression> expressions = new ArrayList<>();
        expressions.add(parseExpression());
        while (match(",")) {
            expressions.add(parseExpression());
        }
        if (!match("]") || !match(";")){
            throw new ParseException("Invalid token", tokens.get(0).getIndex() + tokens.get(0).getLiteral().length());
        }
        return new Ast.Global(name, true, Optional.of(new Ast.Expression.PlcList(expressions)));
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        match("VAR");
        if (!tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        if (!peek(Token.Type.IDENTIFIER)){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();
        Optional<Ast.Expression> expression = Optional.empty();
        if (match("=")) {
            expression = Optional.of(parseExpression());
        }
        if (!match(";")){
            if (!tokens.has(0)){
                throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }else{
                throw new ParseException("Invalid Token", tokens.get(0).getIndex());
            }
        }
        return new Ast.Global(name, true, expression);
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        match("VAL");
        if (!tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        if (!peek(Token.Type.IDENTIFIER)){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();
        if (tokens.has(0) && !match("=")){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }else if (!tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        Ast.Expression expression = parseExpression();
        if (!tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        if (!match(";")){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        return new Ast.Global(name, false, Optional.of(expression));
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        match("FUN");
        if (!tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        if (!peek(Token.Type.IDENTIFIER)){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();
        if (!tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        if (!match("(")){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        List<String> parameters = new ArrayList<>();
        if (!peek(")") && peek(Token.Type.IDENTIFIER)) {
            parameters.add(tokens.get(0).getLiteral());
            tokens.advance();
            while (match(",")) {
                parameters.add(tokens.get(0).getLiteral());
                tokens.advance();
            }
        }
        if (!tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        if (!match(")")){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        if (!tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        if (!peek("DO")){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        List<Ast.Statement> body = parseBlock();
        if (!tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        if (!match("END")){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        return new Ast.Function(name, parameters, body);
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block of statements.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<>();
        if (!tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        if (!match("DO")){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        while (!peek("END")) {
            statements.add(parseStatement());
        }
        if (!tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        if (!peek("END")){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        return statements;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (!tokens.has(0))
            throw new ParseException("Dude there's no tokens!", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        if (peek("LET"))
            return parseDeclarationStatement();
        if (peek("SWITCH"))
            return parseSwitchStatement();   // TODO p2b
        if (peek("IF"))
            return parseIfStatement();       // TODO p2b
        if (peek("WHILE"))
            return parseWhileStatement();    // TODO p2b
        if (peek("RETURN"))
            return parseReturnStatement();   // TODO p2b

        Ast.Expression expr1 = parseExpression();



        if (match("=") && tokens.has(0)) {
            Ast.Expression expr2 = parseExpression();
            if (match(";"))
                return new Ast.Statement.Assignment(expr1, expr2);
            else
                throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        if (match(";"))
            return new Ast.Statement.Expression(expr1);

        if (tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        match("LET");
        if (!tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        if (!peek(Token.Type.IDENTIFIER)){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        String name = tokens.get(0).getLiteral();
        tokens.advance();
        Optional<Ast.Expression> value = Optional.empty();
        if (match("=")) {
            value = Optional.of(parseExpression());
        }
        if (match(";")){
            return new Ast.Statement.Declaration(name, value);
        }
        if (tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        match("IF");
        Ast.Expression condition = parseExpression();
        if (!tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        if (!peek("DO")){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        List<Ast.Statement> thenBlock = parseBlock();
        List<Ast.Statement> elseBlock = new ArrayList<>();
        if (match("ELSE")) {
            elseBlock = parseBlock();
        }
        if (match("END"))
            return new Ast.Statement.If(condition, thenBlock, elseBlock);
        if (tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        match("SWITCH");
        Ast.Expression condition = parseExpression();
        List<Ast.Statement.Case> cases = new ArrayList<>();
        while (peek("CASE")) {
            cases.add(parseCaseStatement());
        }
        if (match("DEFAULT")) {
            cases.add(new Ast.Statement.Case(Optional.empty(), parseBlock()));
            if (!match("END")){
                if (tokens.has(0)){
                    throw new ParseException("Invalid Token", tokens.get(0).getIndex());
                }
                throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            return new Ast.Statement.Switch(condition, cases);
        } else if (tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule. 
     * This method should only be called if the next tokens start the case or 
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        match("CASE");
        Ast.Expression expression = parseExpression();
        if (!match(":")){
            if (tokens.has(0)) {
                throw new ParseException("Invalid Token", tokens.get(0).getIndex());
            }throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        List<Ast.Statement> block = parseBlock();
        return new Ast.Statement.Case(Optional.of(expression), block);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        match("WHILE");
        Ast.Expression expression = parseExpression();
        if (!peek("DO")){
            if (tokens.has(0)){
                throw new ParseException("Invalid Token", tokens.get(0).getIndex());
            }throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
        List<Ast.Statement> block = parseBlock();
        if (match("END")){
            return new Ast.Statement.While(expression, block);
        }
        if (tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        match("RETURN");
        Ast.Expression expression = parseExpression();
        if (match(";")){
            return new Ast.Statement.Return(expression);
        }if (tokens.has(0)){
            throw new ParseException("Invalid Token", tokens.get(0).getIndex());
        }
        throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        if (tokens.has(0))
            return parseLogicalExpression();
        throw new ParseException("Dude there's no tokens!", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        if (!tokens.has(0))
            throw new ParseException("Dude there's no tokens!", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());

        Ast.Expression[] expr = {parseComparisonExpression(), null};

        if (!tokens.has(0))
            return expr[0];

        int ind = 1;

        while (peek("&&") || peek("||"))
        {
            String op = tokens.get(0).getLiteral();
            tokens.advance();
            if (!tokens.has(0)) throw new ParseException("Dude there's no tokens!", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            expr[ind % 2] = new Ast.Expression.Binary(op, expr[++ind % 2], parseComparisonExpression());
        }

        return expr[1] == null ? expr[0] : expr[++ind % 2];
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        if (!tokens.has(0))
            throw new ParseException("Dude there's no tokens!", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());

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
        if (!tokens.has(0)) throw new ParseException("Dude there's no tokens!", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());

        Ast.Expression[] expr = {parseMultiplicativeExpression(), null};

        int ind = 1;
        while (peek("+") || peek("-"))
        {
            String op = tokens.get(0).getLiteral();
            tokens.advance();
            if (!tokens.has(0)) throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            expr[ind % 2] = new Ast.Expression.Binary(op, expr[++ind % 2], parseMultiplicativeExpression());
        }

        return expr[1] == null ? expr[0] : expr[++ind % 2];
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        if (!tokens.has(0)) throw new ParseException("Dude there's no tokens!", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());

        Ast.Expression[] expr = {parsePrimaryExpression(), null};

        int ind = 1;

        while (peek("*") || peek("/") || peek("^"))
        {
            String op = tokens.get(0).getLiteral();
            tokens.advance();
            if (!tokens.has(0)) throw new ParseException("Dude there's no tokens!", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
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
        if (!tokens.has(0))
            throw new ParseException("Dude there's no tokens!", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        if (peek(Token.Type.INTEGER))
            return new Ast.Expression.Literal(new BigInteger(tokens.get(0).getLiteral()));
        if (peek(Token.Type.DECIMAL))
            return new Ast.Expression.Literal(new BigDecimal(tokens.get(0).getLiteral()));
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
                throw new ParseException("Invalid Token", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }

        if (!peek(Token.Type.IDENTIFIER))
            throw new ParseException("Invalid Token", tokens.get(0).getIndex()); //TODO

        String id = tokens.get(0).getLiteral();
        tokens.advance();

        if (match("(", ")"))
            return new Ast.Expression.Function(id, Arrays.asList());


        if (match("(") && tokens.has(0)) {
            List<Ast.Expression> exprArr = new ArrayList<>();
            exprArr.add(parseExpression());

            while (match(",") && tokens.has(0))
                exprArr.add(parseExpression());

            if (match(")"))
                return new Ast.Expression.Function(id, exprArr);
            else
                throw new ParseException("Invalid Token", tokens.index);
        }

        if (match("[") && tokens.has(0)) {
            Ast.Expression expr = parseExpression();
            if (match("]"))
                return new Ast.Expression.Access(Optional.of(expr), id);
            else
                throw new ParseException("Invalid Token", tokens.get(0).getIndex());
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

