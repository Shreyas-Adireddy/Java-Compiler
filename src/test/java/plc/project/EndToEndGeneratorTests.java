package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class EndToEndGeneratorTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSource(String test, String input, String expected) {
        test(input, expected, Parser::parseSource);
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Hello, World!",
                        // FUN main(): Integer DO
                        //     print("Hello, World!");
                        //     RETURN 0;
                        // END
                        "FUN main(): Integer DO\n    print(\"Hello, World!\");\n    RETURN 0;\nEND",
                        String.join(System.lineSeparator(),
                                "public class Main {",
                                "",
                                "    public static void main(String[] args) {",
                                "        System.exit(new Main().main());",
                                "    }",
                                "",
                                "    int main() {",
                                "        System.out.println(\"Hello, World!\");",
                                "        return 0;",
                                "    }",
                                "",
                                "}"
                        )
                )
        );
    }

    @Test
    void testList() {
        // LIST list: Decimal = [1.0, 1.5, 2.0];
        String input = new String("LIST list: Decimal = [1.0, 1.5, 2.0];");
        String expected = new String("double[] list = {1.0, 1.5, 2.0};");
        test(input, expected, Parser::parseGlobal);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testDeclarationStatement(String test, String input, String expected) {
        test(input, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration",
                        // LET name: Integer;
                        "LET name: Integer;",
                        "int name;"
                ),
                Arguments.of("Initialization",
                        // LET name = 1.0;
                        "LET name = 1.0;",
                        "double name = 1.0;"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testIfStatement(String test, String input, String expected) {
        test(input, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("If",
                        // IF TRUE DO
                        //     print(1);
                        // END
                        "IF TRUE DO\n    print(1);\nEND",
                        String.join(System.lineSeparator(),
                                "if (true) {",
                                "    System.out.println(1);",
                                "}"
                        )
                ),
                Arguments.of("Else",
                        // IF FALSE DO
                        //     print(1);
                        // ELSE
                        //     print(0);
                        // END
                        "IF FALSE DO\n    print(1);\nELSE\n    print(0);\nEND",
                        String.join(System.lineSeparator(),
                                "if (false) {",
                                "    System.out.println(1);",
                                "} else {",
                                "    System.out.println(0);",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testSwitchStatement(String test, String input, String expected) {
        test(input, expected, Parser::parseStatement);
    }

    private static Stream<Arguments> testSwitchStatement() {
        return Stream.of(
                Arguments.of("Switch",
                        // SWITCH 'y'
                        //     CASE 'y':
                        //         print("yes");
                        //     DEFAULT
                        //         print("no");
                        // END
                        "SWITCH 'y'\n    CASE 'y':\n        print(\"yes\");\n    DEFAULT\n        print(\"no\");\nEND",
                        String.join(System.lineSeparator(),
                                "switch ('y') {",
                                "    case 'y':",
                                "        System.out.println(\"yes\");",
                                "        break;",
                                "    default:",
                                "        System.out.println(\"no\");",
                                "}"
                        )
                ),
                Arguments.of("Switch Multiple",
                        // SWITCH 1
                        // CASE 1:
                        //     print("num is 1.");
                        // CASE 2:
                        //     print("num is 2.");
                        // DEFAULT
                        //     print("num is greater than 2.");
                        // END
                        "SWITCH 1\nCASE 1:\n    print(\"num is 1.\");\nCASE 2:\n    print(\"num is 2.\");\nDEFAULT\n    print(\"num is greater than 2.\");\nEND",
                        String.join(System.lineSeparator(),
                                "switch (1) {",
                                "    case 1:",
                                "        System.out.println(\"num is 1.\");",
                                "        break;",
                                "    case 2:",
                                "        System.out.println(\"num is 2.\");",
                                "        break;",
                                "    default:",
                                "        System.out.println(\"num is greater than 2.\");",
                                "}"
                        )
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testBinaryExpression(String test, String input, String expected) {
        test(input, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("And",
                        // TRUE && FALSE
                        "TRUE && FALSE",
                        "true && false"
                ),
                Arguments.of("Concatenation",
                        // "Ben" + 10
                        "\"Ben\" + 10",
                        "\"Ben\" + 10"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunctionExpression(String test, String input, String expected) {
        test(input, expected, Parser::parseExpression);
    }

    private static Stream<Arguments> testFunctionExpression() {
        return Stream.of(
                Arguments.of("Print",
                        // print("Hello, World!")
                        "print(\"Hello, World!\")",
                        "System.out.println(\"Hello, World!\")"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void testFunctionDeclaration(String test, String input, String expected) {
        test(input, expected, Parser::parseFunction);
    }

    private static Stream<Arguments> testFunctionDeclaration() {
        return Stream.of(
                Arguments.of("Square",
                        // FUN square(num: Decimal): Decimal DO
                        //    RETURN num * num;
                        // END
                        "FUN square(num: Decimal): Decimal DO\n    RETURN num * num;\nEND",
                        String.join(System.lineSeparator(),
                                "double square(double num) {",
                                "    return num * num;",
                                "}"
                        )
                ),
                Arguments.of("Multiple Statements",
                        // FUN func(x: Integer, y: Decimal, z: String) DO
                        //     print(x);
                        //     print(y);
                        //     print(z);
                        // END
                        "FUN func(x: Integer, y: Decimal, z: String) DO\n    print(x);\n    print(y);\n    print(z);\nEND",
                        String.join(System.lineSeparator(),
                                "Void func(int x, double y, String z) {",
                                "    System.out.println(x);",
                                "    System.out.println(y);",
                                "    System.out.println(z);",
                                "}"
                        )
                )
        );
    }

    /**
     * Helper function for tests, using a StringWriter as the output stream.
     */
    private static <T extends Ast> void test(String input, String expected, Function<Parser, T> function) {
        StringWriter writer = new StringWriter();
        Lexer lexer = new Lexer(input);
        Parser parser = new Parser(lexer.lex());

        Ast ast = function.apply(parser);

        Analyzer analyzer = new Analyzer(new Scope(null));
        analyzer.visit(ast);
        new Generator(new PrintWriter(writer)).visit(ast);
        Assertions.assertEquals(expected, writer.toString());
    }

    /**
     * Runs a callback on the given value, used for inline initialization.
     */
    private static <T> T init(T value, Consumer<T> initializer) {
        initializer.accept(value);
        return value;
    }

}
