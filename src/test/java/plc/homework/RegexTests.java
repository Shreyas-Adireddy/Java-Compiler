package plc.homework;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.regex.Pattern;
import java.util.stream.Stream;
/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {
    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }
    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Mixed Case", "UserName@Example.com", true),
                Arguments.of("Underscore in Domain", "user_name@example.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                Arguments.of("Invalid Domain", "invalid@.com", false),
                Arguments.of("Invalid Characters", "user&name@example.com", false),
                Arguments.of("No Username", "@example.com", false),
                Arguments.of("No Domain", "username@", false),
                Arguments.of("Invalid TLD", "user@domain.invalidtld", false),
                Arguments.of("Domain with Hyphen", "user@my-domain.com", false)
        );
    }
    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }
    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
// what have eleven letters and starts with gas?
                Arguments.of("11 Characters", "automobiles", true),
                Arguments.of("13 Characters", "i<3pancakes13", true),
                Arguments.of("15 Characters", "i<3pancakes1312", true),
                Arguments.of("17 Characters", "i<3pancakes131234", true),
                Arguments.of("19 Characters", "i<3pancakes13123456", true),
                Arguments.of("5 Characters", "5five", false),
                Arguments.of("14 Characters", "i<3pancakes14!", false),
                Arguments.of("10 Characters", "abcdefghij", false),
                Arguments.of("20 Characters", "12345678901234567890", false),
                Arguments.of("9 Characters", "shortone", false),
                Arguments.of("Empty String", "", false),
                Arguments.of("Special Characters", "!@#$%^&*()", false),
                Arguments.of("Special Characters", "!@#$%^&*()\n\r\t\b\f", true),
                Arguments.of("Special Characters 2", "!@#$%^&*(", false),
                Arguments.of("Special Characters 2", "!@#$%^&*(\u000B\u000C", true)
        );
    }
    @ParameterizedTest
    @MethodSource
    public void testCharacterListRegex(String test, String input, boolean success)
    {
        test(input, Regex.CHARACTER_LIST, success);
    }
    public static Stream<Arguments> testCharacterListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "['a']", true),
                Arguments.of("Multiple Elements", "['a','b','c']", true),
                Arguments.of("Missing Brackets", "'a','b','c'", false),
                Arguments.of("Missing Commas", "['a' 'b' 'c']", false),
                Arguments.of("Too Many Commas", "['a', 'b',, 'c']", false),
                Arguments.of("Empty List", "[]", true),
                Arguments.of("Numeric Elements", "[1,2,3]", false),
                Arguments.of("Special Characters", "['!', '@', '#']", true),
                Arguments.of("Mixed Spaces", "['a','b', 'c']", true),
                Arguments.of("Invalid Characters", "[a, b, c]", false),
                Arguments.of("Nested Brackets", "[['a', 'b'], ['c', 'd']]", false),
                Arguments.of("Incorrect Array Structure", "['a', 'b''c']", false),
                Arguments.of("Multiple Special Characters", "['!!', '@@', '##']", false),
                Arguments.of("Special Character 2", "[''']", true),
                Arguments.of("Special Character 2.5", "['\n', '\r', '\f', '\b']", true),
                Arguments.of("Special Character 3", "['', '']", false),
                Arguments.of("Special Special Characters", "[''', ''', '\\', '\\', '\u000B', '\u000C']", true)
        );
    }
    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        test(input, Regex.DECIMAL, success);
    }
    public static Stream<Arguments> testDecimalRegex() {
        return Stream.of(
                Arguments.of("Valid Decimal", "10100.001", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Trailing Zeros", "0.123000", true),
                Arguments.of("Integer with Trailing Zeros", "100.00", true),
                Arguments.of("Single Digit before Decimal", "0.1", true),
                Arguments.of("Negative with Two-digit Decimal", "-0.01", true),
                Arguments.of("Integer not Decimal", "1", false),
                Arguments.of("Missing Integer Part", ".5", false),
                Arguments.of("Leading Zeros", "00.123", false),
                Arguments.of("Invalid Decimal", "-0.", false),
                Arguments.of("Trailing Dot", "1.", false),
                Arguments.of("Multiple Decimal Points", "1.23.4", false)
        );
    }
    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }
    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("Empty String", "", false),
                Arguments.of("Empty Nested String", "\"\"", true),
                Arguments.of("Spaces", "\"   \"", true),
                Arguments.of("String with Special Characters", "\"Hello, World!\"", true),
                Arguments.of("String with Escape Sequence", "\"1\\t2\"", true),
                Arguments.of("Multiple Escape Characters", "\"Escape: \\\\\\\\\"", true),
                Arguments.of("String with Newline", "\"Line1\\nLine2\"", true),
                Arguments.of("Escaped Quote", "\"Quoted: \\\"\"", true),
                Arguments.of("Unterminated String", "\"unterminated", false),
                Arguments.of("Invalid Escape Sequence", "\"invalid\\escape\"", false),
                Arguments.of("No Quotes", "noquotes", false),
                Arguments.of("Extra Quote", "\"\"\"", false),
                Arguments.of("Extra Characters after String", "\"Hello, World!\" extra", false),
                Arguments.of("Unquoted Escape Sequence", "1\\t2", false),
                Arguments.of("Escape Sequences", "\"\\t\\n\\b\\f\\r\"", true),
                Arguments.of("Unicode Escape Sequences", "\"\u000B\u000C\"", true)
        );
    }
    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }
}
