/* MicroJava Scanner */
package MicroJava;

import java.io.*;

public class Scanner {

    public int line, column; // current line, column
    private char c;          // lookahead character
    private int position;    // current position from start of input
    private Reader input;    // input reader
    private static final char EOL = '\n', EOF_CHAR = '\u0080';
    // Error messages
    public static String ERROR_INVALID_SYMBOL = "invalid symbol",
            ERROR_EMPTY_CHARACTER = "empty character",
            ERROR_INVALID_CHARACTER = "invalid character",
            ERROR_UNCLOSED_CHARACTER = "unclosed character",
            ERROR_INVALID_OPERATOR = "invalid operator";

    public Scanner(Reader reader) {
        input = new BufferedReader(reader);
        line = 1;
        position = column = 0;
        nextChar();
    }

    /** Read next character from input. */
    private void nextChar() {
        try {
            c = (char) input.read();
        } catch (IOException e) {
            c = EOF_CHAR;
            return;
        }
        ++column;
        ++position;
        if (c == EOL) {
            ++line;
            column = 0;
        } else if (c == '\uffff') {
            c = EOF_CHAR;
        }
    }

    /** Read next non-blank character. */
    private void skipBlankChars() {
        while (c <= ' ') {
            nextChar();
        }
    }

    /** Returns next input token. */
    public Token next() {
        skipBlankChars();
        Token token = new Token(line, column);
        if (c == EOF_CHAR) {
            token.kind = Token.Kind.EOF;
        } else if (Character.isDigit(c)) {
            readNumber(token);
        } else if (Character.isLetter(c)) {
            readName(token);
        } else if (c == '\'') {
            readCharacter(token);
        } else if (c == '/') { // This test is not include in readOperator
            // because of recursion (return next())
            nextChar();
            if (c == '/') {
                // skip commentary
                do {
                    nextChar();
                } while (c != EOL && c != EOF_CHAR);
                nextChar();
                return next(); // return next token (commentary is not a token)
            } else {
                token.kind = Token.Kind.SLASH;
                token.string = "/";
            }
        } else if (!readOperator(token)) {
            // An invalid operator that begins with ! but differ
            //  from != could been read in readOperator.
            // If so, token is already built, so token.string is NOT empty.
            if (token.string.isEmpty()) {
                token.kind = Token.Kind.UNKNOWN;
                token.errorMessage = ERROR_INVALID_SYMBOL;
                nextChar();
            }
        }
        return token;
    }

    /** Read number and build token. */
    private void readNumber(Token token) {
        StringBuilder lexeme = new StringBuilder();
        while (Character.isDigit(c)) {
            lexeme.append(c);
            nextChar();
        }
        token.string = lexeme.toString();
        token.kind = Token.Kind.NUMBER;
        token.intValue = Integer.parseInt(token.string);
    }

    /** Read name and build token. */
    private void readName(Token token) {
        StringBuilder lexeme = new StringBuilder();
        while (Character.isLetterOrDigit(c)) {
            lexeme.append(c);
            nextChar();
        }
        token.string = lexeme.toString();
        token.kind = Token.getKeywordKind(token.string);
    }

    /** Read character and build token. */
    private void readCharacter(Token token) {
        StringBuilder lexeme = new StringBuilder();
        lexeme.append(c);
        nextChar();
        lexeme.append(c);
        if (c == '\'') {
            errorEmptyCharacter(token);
        } else if (c == '\\') { //  '\n'  '\r'  '\t'
            readEscapedCharacter(token, lexeme);
        } else { // any valid character
            readCloseCharacter(token, lexeme);
        }
    }

    /** Build token based on empty character error. */
    private void errorEmptyCharacter(Token token) {
        token.errorMessage = ERROR_EMPTY_CHARACTER;
        token.string = "''";
        token.column = column;
        token.kind = Token.Kind.UNKNOWN;
        nextChar();
    }

    /** Read escaped character (like \r \n \t) and build token. */
    private void readEscapedCharacter(Token token, StringBuilder lexeme) {
        nextChar();
        lexeme.append(c);
        if (c == 'n' || c == 'r' || c == 't') {
            readCloseCharacter(token, lexeme);
        } else {
            token.errorMessage = ERROR_INVALID_CHARACTER;
            token.kind = Token.Kind.UNKNOWN;
            token.column = column;
            skipUntilCloseCharacter(lexeme);
            token.string = lexeme.toString();
            nextChar(); // skip single quote
        }
    }

    /** Read close character and build token. */
    private void readCloseCharacter(Token token, StringBuilder lexeme) {
        nextChar();
        lexeme.append(c);
        if (c == '\'') {
            token.string = lexeme.toString();
            token.kind = Token.Kind.CHARACTER;
            if (token.string.charAt(1) == '\\') { // \n \r \t
                char escapeChar = token.string.charAt(2);
                if (escapeChar == 'n') {
                    token.charValue = '\n';
                } else if (escapeChar == 'r') {
                    token.charValue = '\r';
                } else if (escapeChar == 't') {
                    token.charValue = '\t';
                }
            } else {
                token.charValue = token.string.charAt(1);
            }
            nextChar();
        } else {
            token.errorMessage = ERROR_UNCLOSED_CHARACTER;
            token.kind = Token.Kind.UNKNOWN;
            token.column = column;
            skipUntilCloseCharacter(lexeme);
            token.string = lexeme.toString();
            nextChar(); // skip single quote
        }
    }

    /**
     * Read until close character (to get next valid token). After the
     * method is executed, current char will be a single quote or EOF_CHAR.
     */
    private void skipUntilCloseCharacter(StringBuilder lexeme) {
        do {
            nextChar();
            lexeme.append(c);
        } while (c != '\'' && c != EOF_CHAR);
    }

    /**
     * Read an operator and build token.
     * Returns true if an operator was read. Else, returns false.
     */
    private boolean readOperator(Token token) {
        if (c == '=') {
            nextChar();
            if (c == '=') {
                token.kind = Token.Kind.EQUAL;
                nextChar();
            } else {
                token.kind = Token.Kind.ASSIGN;
            }
        } else if (c == '!') {
            nextChar();
            if (c == '=') {
                token.kind = Token.Kind.NOT_EQUAL;
                nextChar();
            } else {
                token.kind = Token.Kind.UNKNOWN;
                token.errorMessage = ERROR_INVALID_OPERATOR;
                token.string = "!" + c;
                nextChar();
                return false;
            }
        } else if (c == '<') {
            nextChar();
            if (c == '=') {
                token.kind = Token.Kind.LESS_EQUAL;
                nextChar();
            } else {
                token.kind = Token.Kind.LESS;
            }
        } else if (c == '>') {
            nextChar();
            if (c == '=') {
                token.kind = Token.Kind.GREATER_EQUAL;
                nextChar();
            } else {
                token.kind = Token.Kind.GREATER;
            }
        } else if ("+-*%;,.()[]{}".indexOf(c) >= 0) {
            token.kind = Token.getOperatorKind(Character.toString(c));
            nextChar();
        } else {
            return false;
        }
        token.string = Token.getName(token.kind);
        return true;
    }
}
