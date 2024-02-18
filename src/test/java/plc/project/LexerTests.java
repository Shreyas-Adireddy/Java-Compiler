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
                Arguments.of("Final 1", "abc", true),
                Arguments.of("Final 2", "abc123", true),
                Arguments.of("Final 3", "a_b_c", true),
                Arguments.of("Final 4", "a-b-c", true),
                Arguments.of("Final 5", "@abc", true),
                Arguments.of("Final 6", "_abc", false),
                Arguments.of("Final 7", "1abc", false),
                Arguments.of("Final 8", "ABC", true),
                Arguments.of("Final 9", "a", true),
                Arguments.of("Final 10", "abcdefghijklmnopqrstuvwxyz012346789_-", true),
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
                Arguments.of("Shreyas 1", "a", true),
                Arguments.of("Shreyas 2", "a-b-c", true),
                Arguments.of("Shreyas 3", "___", false),
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
                Arguments.of("Final 1", "1", true),
                Arguments.of("Final 1", "123", true),
                Arguments.of("Final 1", "123456789123456789123456789", true),
                Arguments.of("Final 1", "-1", true),
                Arguments.of("Final 1", "0", true),
                Arguments.of("Tony 2", "0", true),
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Trailing Zeros", "1234500", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Tony 3", "-1", true),
                Arguments.of("Tony 4", "-0", false),
                Arguments.of("Tony 5", "1234567890", true),
                Arguments.of("Comma", "1,234,567,890", false),
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
                Arguments.of("Final 1", "1.0", true),
                Arguments.of("Final 1", "123.456", true),
                Arguments.of("Final 1", "9007199254740993.0", true),
                Arguments.of("Final 1", "-1.0", true),
                Arguments.of("Final 1", "111.000", true),
                Arguments.of("No Leading Zero 0", "-01.5", false),
                Arguments.of("Negative Decimal", "-0.1", true),
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Decimal", "1.1", true),
                Arguments.of("Decimal V2", "0.12345", true),
                Arguments.of("No Leading Zero 1", "1.05", true),
                Arguments.of("No Leading Zero 2", "0.5.", false),
                Arguments.of("No Leading Zero 3", "00.5", false)
        );
    }
    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }
    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Final 1", "''", false),
                Arguments.of("Final 2", "'c'", true),
                Arguments.of("Final 3", "'abc'", false),
                Arguments.of("Final 4", "'1'", true),
                Arguments.of("Final 5", "'ρ'", true),
                Arguments.of("Final 6", "' '", true),
                Arguments.of("Final 7", "'\''", true),
                Arguments.of("Final 8", "'\\'", true),
                Arguments.of("Final 9", "'c\n'", false),
                Arguments.of("Final 10", "'", false),
                Arguments.of("LF", "'\n'", false),
                Arguments.of("CR", "'\r'", false),
                Arguments.of("BB", "'\\'", false),
                Arguments.of("Apostrophe", "'''", false),
                Arguments.of("Unterminated Char", "'", false),
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Escape", "\'\\b\'", true),
                Arguments.of("Escape 1", "\'\\n\'", true),
                Arguments.of("Escape 2", "\'\\r\'", true),
                Arguments.of("Escape 3", "\'\\t\'", true),
                Arguments.of("Escape 4", "\'\\f\'", true),
                Arguments.of("Unicode", "\'\u000b'", true),
                Arguments.of("Unicode", "\'\u000c'", true),
                Arguments.of("Special", "'@'", true),
                Arguments.of("Special 1", "'.'", true),
                Arguments.of("Special 2", "'!'", true),
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
                Arguments.of("Final 1", "\"\"", true),
                Arguments.of("Final 2", "\"c\"", true),
                Arguments.of("Final 3", "\"abc\"", true),
                Arguments.of("Final 4", "\"123\"", true),
                Arguments.of("Final 5", "\"!@#$%^&*\"", true),
                Arguments.of("Final 6", "\"ρ★⚡\"", true),
                Arguments.of("Final 7", "\"\b\t\"", true),
                Arguments.of("Final 8", "\"Hello, \\nWorld!\"", true),
                Arguments.of("Final 9", "\"a\\bcdefghijklm\\nopq\\rs\\tuvwxyz\"\n", false),
                Arguments.of("Final 10", "\"sq\\'dq\\\"bs\\\\\"", false),
                Arguments.of("Final 11", "\"abc\\0123\"", false),
                Arguments.of("Final 12", "\"a\\u0000b\u12ABc\"", true),
                Arguments.of("Final 13", "\"unterminated", false),
                Arguments.of("Final 14", "\"unterminated\n\"", false),
                Arguments.of("Final 15", "\"", false),
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("NL", "\"\n\"", false),
                Arguments.of("CR", "\"\r\"", false),
                Arguments.of("BB", "\"\\\"", false),
                Arguments.of("Special Characters", "\"!@#$%^&*()\"", true),
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
                Arguments.of("Unterminated", "\"unt  ", false),
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
                Arguments.of("Final 1", "%", true),
                Arguments.of("Final 2", "ρ", true),
                Arguments.of("Final 3", "==", true),
                Arguments.of("Final 4", ">", true),
                Arguments.of("Final 5", "!=", true),
                Arguments.of("Final 6", "+", true),
                Arguments.of("Final 7", "-", true),
                Arguments.of("Final 8", " ", true),
                Arguments.of("Final 9", "\t", false),
                Arguments.of("Final 10", "\f", false),
                Arguments.of("Comparison", "||", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Comparison", "&&", true),
                Arguments.of("Comparison", "==", true),
                Arguments.of("Comparison", "=", true),
                Arguments.of("Character", "(", true),
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
                Arguments.of("PENIS", "VAR i = -1 : Integer;\nVAL inc = 2 : Integer;\nFUN foo() DO\n    WHILE i != 1 DO\n        IF i > 0 DO\n            print(\"bar\");\n        END\n        i = i + inc;\n    END\nEND",
                        Arrays.asList(
                                //VAR i = -1 : Integer;
                                new Token(Token.Type.IDENTIFIER, "VAR", 0),
                                new Token(Token.Type.IDENTIFIER, "i", 4),
                                new Token(Token.Type.OPERATOR, "=", 6),
                                new Token(Token.Type.INTEGER, "-1", 8),
                                new Token(Token.Type.OPERATOR, ":", 11),
                                new Token(Token.Type.IDENTIFIER, "Integer", 13),
                                new Token(Token.Type.OPERATOR, ";", 20),
                                //VAL inc = 2 : Integer;
                                new Token(Token.Type.IDENTIFIER, "VAL", 22),
                                new Token(Token.Type.IDENTIFIER, "inc", 26),
                                new Token(Token.Type.OPERATOR, "=", 30),
                                new Token(Token.Type.INTEGER, "2", 32),
                                new Token(Token.Type.OPERATOR, ":", 34),
                                new Token(Token.Type.IDENTIFIER, "Integer", 36),
                                new Token(Token.Type.OPERATOR, ";", 43),
                                //DEF foo() DO
                                new Token(Token.Type.IDENTIFIER, "FUN", 45),
                                new Token(Token.Type.IDENTIFIER, "foo", 49),
                                new Token(Token.Type.OPERATOR, "(", 52),
                                new Token(Token.Type.OPERATOR, ")", 53),
                                new Token(Token.Type.IDENTIFIER, "DO", 55),
                                // WHILE i != 1 DO
                                new Token(Token.Type.IDENTIFIER, "WHILE", 62),
                                new Token(Token.Type.IDENTIFIER, "i", 68),
                                new Token(Token.Type.OPERATOR, "!=", 70),
                                new Token(Token.Type.INTEGER, "1", 73),
                                new Token(Token.Type.IDENTIFIER, "DO", 75),
                                // IF i > 0 DO
                                new Token(Token.Type.IDENTIFIER, "IF", 86),
                                new Token(Token.Type.IDENTIFIER, "i", 89),
                                new Token(Token.Type.OPERATOR, ">", 91),
                                new Token(Token.Type.INTEGER, "0", 93),
                                new Token(Token.Type.IDENTIFIER, "DO", 95),
                                // print(\"bar\");
                                new Token(Token.Type.IDENTIFIER, "print", 110),
                                new Token(Token.Type.OPERATOR, "(", 115),
                                new Token(Token.Type.STRING, "\"bar\"", 116),
                                new Token(Token.Type.OPERATOR, ")", 121),
                                new Token(Token.Type.OPERATOR, ";", 122),
                                // END
                                new Token(Token.Type.IDENTIFIER, "END", 132),
                                // i = i + inc;
                                new Token(Token.Type.IDENTIFIER, "i",144),
                                new Token(Token.Type.OPERATOR, "=", 146),
                                new Token(Token.Type.IDENTIFIER, "i", 148),
                                new Token(Token.Type.OPERATOR, "+", 150),
                                new Token(Token.Type.IDENTIFIER, "inc", 152),
                                new Token(Token.Type.OPERATOR, ";", 155),
                                // END
                                new Token(Token.Type.IDENTIFIER, "END", 161),
                                //END
                                new Token(Token.Type.IDENTIFIER, "END", 165)
                    )),
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
                    )),
                Arguments.of("Example 3", "VAR i = -1 : Integer;",
                        Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "VAR", 0),
                                new Token(Token.Type.IDENTIFIER, "i", 4),
                                new Token(Token.Type.OPERATOR, "=", 6),
                                new Token(Token.Type.INTEGER, "-1", 8),
                                new Token(Token.Type.OPERATOR, ":", 11),
                                new Token(Token.Type.IDENTIFIER, "Integer", 13),
                                new Token(Token.Type.OPERATOR, ";", 20)
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
    @ParameterizedTest
    @MethodSource
    void testWhitespace(String test, String input, List<Token> expected, boolean success) {
        test(input, expected, success);
    }

    private static Stream<Arguments> testWhitespace() {
        return Stream.of(
                Arguments.of("Final 1", "one two", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 4)
                ), true),
                Arguments.of("Final 2", "one\ttwo", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 4)
                ), true),
                Arguments.of("Final 3", "one\btwo", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 4)
                ), true),
                Arguments.of("Final 4", "one\r\ntwo", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 5)
                ), true),
                Arguments.of("Final 5", "one \b\n\r\ttwo", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 8)
                ), true),
                Arguments.of("Final 6", "one    two", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 7)
                ), true),
                Arguments.of("Final 7", "      token", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "token", 6)
                ), true),
                Arguments.of("Final 8", "token     ", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "token", 0)
                ), true),
                Arguments.of("Final 9", "          ", Arrays.asList(), true),
                Arguments.of("Final 10", "verticaltab\u000Bformfeed\n", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "verticaltab", 0),
                        new Token(Token.Type.IDENTIFIER, "formfeed", 12)
                ), true),
                Arguments.of("Multiple Spaces", "one    two", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 7)
                ), true),

                Arguments.of("Trailing Newline", "token\n", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "token", 0)
                ), true),

                Arguments.of("Not Whitespace", "one\ttwo", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "one", 0),
                        new Token(Token.Type.IDENTIFIER, "two", 4)
                ), true)
        );
    }

    // Mixed Token tests
    @ParameterizedTest
    @MethodSource
    void testMixedToken(String test, String input, List<Token> expected, boolean success) {
        test(input, expected, success);
    }

    private static Stream<Arguments> testMixedToken() {
        return Stream.of(
                Arguments.of("Multiple Decimals", "1.2.3", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "1.2", 0),
                        new Token(Token.Type.DECIMAL, ".3", 3)
                ), false),

                Arguments.of("Equals Combinations", "!=====", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "!=", 0),
                        new Token(Token.Type.OPERATOR, "==", 2),
                        new Token(Token.Type.OPERATOR, "==", 4)
                ), true),

                Arguments.of("Weird Quotes", "\'\"\'string\"'\" ", Arrays.asList(
                        new Token(Token.Type.CHARACTER, "\'\"\'", 0),
                        new Token(Token.Type.IDENTIFIER, "string", 3),
                        new Token(Token.Type.STRING, "\"'\"", 9)
                ), true)
        );
    }
}

