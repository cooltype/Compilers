/* MicroJava Scanner Tester */
package MicroJava;

import java.io.*;

public class TestScanner {

    public static void main(String args[]) {
        if (args.length == 0) {
            executeTests();
        } else {
            for (int i = 0; i < args.length; ++i) {
                reportFile(args[i]);
            }
        }
    }

    private static void reportFile(String filename) {
        try {
            report("File: " + filename,
                    new InputStreamReader(new FileInputStream(filename)));
        } catch (IOException e) {
            System.out.println("-- cannot open file " + filename);
        }
    }

    private static void report(String header, Reader reader) {
        Scanner scanner = new Scanner(reader);
        Token token;
        System.out.println(header);
        while (true) {
            token = scanner.next();
            if (token.kind == Token.Kind.EOF) {
                break;
            }
            System.out.printf("%3d, %3d: %s ", token.line, token.column,
                    Token.getName(token.kind));
            switch (token.kind) {
                case IDENTIFIER:
                    System.out.println(token.string);
                    break;
                case NUMBER:
                    System.out.println(token.string + " = " + token.intValue);
                    break;
                case CHARACTER:
                    System.out.println(token.string + " = " + token.charValue);
                    break;
                case UNKNOWN:
                    System.out.println("- " + token.errorMessage
                            + ": " + token.string);
                    break;
                default:
                    System.out.println();
                    break;
            }
        }
        System.out.println();
    }

    private static void executeTests() {
        testNumbers();
        testCharacters();
        testIdentifiers();
        testKeywords();
        testOperators();
    }

    private static void testNumbers() {
        report("Test: numbers", new StringReader("1 255 001"));
    }

    private static void testCharacters() {
        report("Test: characters",
                new StringReader("'c' '1' '+' '@' ' ' '\\t' '\\r' '\\n'"));
        report("Test: characters errors",
                new StringReader("''\n'\\\\'\n'12'"));
    }

    private static void testIdentifiers() {
        report("Test: identifiers", new StringReader("int char a xyz world"));
    }

    private static void testKeywords() {
        report("Test: keywords",
                new StringReader("class else final if new print"
                + " program read return void while"));
    }

    private static void testOperators() {
        report("Test: operators",
                new StringReader("+ - * / % == != < <= > >= ="
                + " ; , .  ( ) [ ] { }"));
        report("Test: operators errors",
                new StringReader(" !# \n !!"));
    }
}
