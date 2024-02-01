package plc.project;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
public class LexerTests {
    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }
    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphabetic 2", "GetName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("At symbol", "@thelegend27", true),
                Arguments.of("Underscore", "the_legend27", true),
                Arguments.of("Hyphen", "the-legend27", true),
                Arguments.of("Both", "the-legend_27", true),
                Arguments.of("Underscore at start", "_thelegend27", false),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Hyphen 2", "-123five", false),
                Arguments.of("Tony 1", "@@", false),
                Arguments.of("Tony 2", "@", true),
                Arguments.of("Tony 3", "abcdefghijklmnopqrstuvwxyz\n\u0008", false),
                Arguments.of("Tony 4", "@abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_--_-_-___--__-_", true),
                Arguments.of("Tony 5", "\b", false),
                Arguments.of("Tony 6", "\f", false),
                Arguments.of("Tony 7", "\r", false),
                Arguments.of("Tony 9", "\"", false),
                Arguments.of("Tony 10", "@_", true),
                Arguments.of("Tony 11", "@\n", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false)

        );
    }
    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }
    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Tony 2", "0", true),
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Trailing Zeros", "1234500", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Tony 3", "-1", true),
                Arguments.of("Tony 4", "-0", false),
                Arguments.of("Tony 5", "1234567890", true),
                Arguments.of("Tony 6", "69", true),
                Arguments.of("Tony 7", "34", true),
                Arguments.of("Tony 8", "--00", false),
                Arguments.of("Tony 9", "-1-", false),
                Arguments.of("Tony 10", "-1-", false),
                Arguments.of("Tony 11", "-144", true),
                Arguments.of("Tony 12", "-1-", false),
                Arguments.of("Leading Zero", "01", false),
                Arguments.of("Middle Minus", "0-1", false),
                Arguments.of("Middle Minus", ".1", false)

        );
    }
    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }
    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Decimal", "1.1", true),
                Arguments.of("Decimal V2", "0.12345", true),
                Arguments.of("Negative Decimal", "-0.1", true),
                Arguments.of("No Leading Zero", ".5", false)
        );
    }
    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }
    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Unicode", "\'\u0008'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false)
        );
    }
    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }
    private static Stream<Arguments> testString() {
        return Stream.of(
                // Matching Cases
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Empty String", "\"\"", true),
                Arguments.of("No Escape Characters", "\"Hello, World\"", true),
                Arguments.of("Working Escape Character 1", "\"1\\b2\"", true),
                Arguments.of("Working Escape Character 4", "\"1\\t2\"", true),
                Arguments.of("Working Escape Character 5", "\"1\\'2\"", true),
                Arguments.of("Working Escape Character 6", "\"1\\\"2\"", true),
                Arguments.of("Working Escape Character 7", "\"1\\\\2\"", true),
                Arguments.of("Working Escape Characters", "\"\\\\\\b\\t\\'\\\"\\\\WOAH\\\\\\b\\n\\r\\t\\'\\\"\\\\\"", true),
                // Non-matching Cases
                Arguments.of("Unterminated", "\"unt", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Not Closed Right", "\"unterminated", false),
                Arguments.of("Not Closed Left", "unterminated\"", false),
                Arguments.of("Not Closed", "unterminated", false),
                Arguments.of("Invalid Escape Character 1", "\"invalid\\ascape\"", false),
                Arguments.of("Invalid Escape Character 2", "\"invalid\\escape\"", false),
                Arguments.of("Invalid Escape Character 3", "\"invalid\\iscape\"", false),
                Arguments.of("Invalid Escape Character 4", "\"invalid\\oscape\"", false),
                Arguments.of("Invalid Escape Character 5", "\"invalid\\uscape\"", false)
        );
    }
    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
//this test requires our lex() method, since that's where whitespace is handled.
                test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)),
                        success);
    }
    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false)
        );
    }
    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }
    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "print", 0),
                                new Token(Token.Type.OPERATOR, "(", 5),
                                new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                                new Token(Token.Type.OPERATOR, ")", 21),
                                new Token(Token.Type.OPERATOR, ";", 22)
                        ))
        );
    }
    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }
    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new
                        Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new
                        Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }
    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }
}
