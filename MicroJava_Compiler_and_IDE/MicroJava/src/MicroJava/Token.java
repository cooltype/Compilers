/* MicroJava Token */
package MicroJava;

import java.util.*;

public class Token {

    public int line, column;
    public Kind kind;
    public String string, errorMessage;
    public int intValue;
    public char charValue;

    public Token(int line, int column) {
        this.line = line;
        this.column = column;
        this.kind = Token.Kind.UNKNOWN;
        this.string = this.errorMessage = "";
        this.intValue = 0;
        this.charValue = '\0';
    }

    public static enum Kind {

        UNKNOWN, EOF, IDENTIFIER, NUMBER, CHARACTER,
        // keywords
        CLASS, ELSE, FINAL, IF, NEW, PRINT, PROGRAM, READ,
        RETURN, VOID, WHILE,
        // operators
        PLUS, MINUS, TIMES, SLASH, REMAINDER, EQUAL, NOT_EQUAL, LESS,
        LESS_EQUAL, GREATER, GREATER_EQUAL, ASSIGN, SEMICOLON, COMMA,
        PERIOD, LEFT_PARENTHESIS, RIGHT_PARENTHESIS, LEFT_BRACKET,
        RIGHT_BRACKET, LEFT_BRACE, RIGHT_BRACE
    }
    private static final Map<Kind, String> Name;

    // initialize Name
    static {
        Name = new EnumMap<Kind, String>(Kind.class);
        Name.put(Kind.UNKNOWN, "UNKOWN");
        Name.put(Kind.EOF, "EOF");
        Name.put(Kind.IDENTIFIER, "identifier");
        Name.put(Kind.NUMBER, "number");
        Name.put(Kind.CHARACTER, "character");
        // keywords
        Name.put(Kind.CLASS, "class");
        Name.put(Kind.ELSE, "else");
        Name.put(Kind.FINAL, "final");
        Name.put(Kind.IF, "if");
        Name.put(Kind.NEW, "new");
        Name.put(Kind.PRINT, "print");
        Name.put(Kind.PROGRAM, "program");
        Name.put(Kind.READ, "read");
        Name.put(Kind.RETURN, "return");
        Name.put(Kind.VOID, "void");
        Name.put(Kind.WHILE, "while");
        // operators
        Name.put(Kind.PLUS, "+");
        Name.put(Kind.MINUS, "-");
        Name.put(Kind.TIMES, "*");
        Name.put(Kind.SLASH, "/");
        Name.put(Kind.REMAINDER, "%");
        Name.put(Kind.EQUAL, "==");
        Name.put(Kind.NOT_EQUAL, "!=");
        Name.put(Kind.LESS, "<");
        Name.put(Kind.LESS_EQUAL, "<=");
        Name.put(Kind.GREATER, ">");
        Name.put(Kind.GREATER_EQUAL, ">=");
        Name.put(Kind.ASSIGN, "=");
        Name.put(Kind.SEMICOLON, ";");
        Name.put(Kind.COMMA, ",");
        Name.put(Kind.PERIOD, ".");
        Name.put(Kind.LEFT_PARENTHESIS, "(");
        Name.put(Kind.RIGHT_PARENTHESIS, ")");
        Name.put(Kind.LEFT_BRACKET, "[");
        Name.put(Kind.RIGHT_BRACKET, "]");
        Name.put(Kind.LEFT_BRACE, "{");
        Name.put(Kind.RIGHT_BRACE, "}");
    }
    private static final Kind keywordKind[] = {
        Kind.CLASS, Kind.ELSE, Kind.FINAL, Kind.IF,
        Kind.NEW, Kind.PRINT, Kind.PROGRAM, Kind.READ,
        Kind.RETURN, Kind.VOID, Kind.WHILE
    };
    private static final Kind operatorKind[] = {
        Kind.PLUS, Kind.MINUS, Kind.TIMES, Kind.SLASH, Kind.REMAINDER,
        Kind.EQUAL, Kind.NOT_EQUAL, Kind.LESS, Kind.LESS_EQUAL, Kind.GREATER,
        Kind.GREATER_EQUAL, Kind.ASSIGN, Kind.SEMICOLON, Kind.COMMA,
        Kind.PERIOD, Kind.LEFT_PARENTHESIS, Kind.RIGHT_PARENTHESIS,
        Kind.LEFT_BRACKET, Kind.RIGHT_BRACKET, Kind.LEFT_BRACE,
        Kind.RIGHT_BRACE
    };

    /**
     * If lexeme is a keyword, returns its kind.
     * Else returns Token.Kind.IDENTIFIER
     */
    public static Kind getKeywordKind(String lexeme) {
        for (int i = 0; i < keywordKind.length; ++i) {
            Kind kind = keywordKind[i];
            if (lexeme.equals(Name.get(kind))) {
                return kind;
            }
        }
        return Kind.IDENTIFIER;
    }

    /**
     * If lexeme is an operator, returns its kind.
     * Else returns Token.Kind.UNKNOWN
     */
    public static Kind getOperatorKind(String lexeme) {
        for (int i = 0; i < operatorKind.length; ++i) {
            Kind kind = operatorKind[i];
            if (lexeme.equals(Name.get(kind))) {
                return kind;
            }
        }
        return Kind.UNKNOWN;
    }

    /** Returns name of token with kind. */
    public static String getName(Kind kind) {
        return Name.get(kind);
    }
}
