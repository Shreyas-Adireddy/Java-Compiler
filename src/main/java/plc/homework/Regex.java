
package plc.homework;
import java.util.regex.Pattern;
/**
 * Contains {@link Pattern} constants, which are compiled regular expressions.
 * See the assignment page for resources on regexes as needed.
 */
public class Regex {
    public static final Pattern
            EMAIL = Pattern.compile("[A-Za-z0-9._]{2,}@[A-Za-z0-9~]+\\.([A-Za-z0-9-]+\\.)*[a-z]{3}"),
            ODD_STRINGS = Pattern.compile("^(((.|\n|\r|\f|\t|\b){11})|(.|\n|\r|\f|\t|\b){13}|(.|\n|\r|\f|\t|\b){15}|(.|\n|\r|\f|\t|\b){17}|(.|\n|\r|\f|\t|\b){19})$"), //TODO
            CHARACTER_LIST = Pattern.compile("^\\s*\\[('(.|\n|\r|\f|\t|\b)'(?:\\s*,\\s*'(.|\n|\r|\f|\t|\b)')*)?\\s*]\\s*$"), //TODO
            DECIMAL = Pattern.compile("^-?(0|[1-9]\\d*)\\.\\d+$"), //TODO
            STRING = Pattern.compile("^\"(?:[^\\\\\"]|\\\\[bnrtf'\"\\\\])*\""); //TODO
}
