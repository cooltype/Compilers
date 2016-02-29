/* MicroJava Parser */
package MicroJava;

// Methods without modifiers (private, protected, public)
// have package access to make test easier.
import java.util.*;
import java.io.Reader;

public class Parser {

    private Token token;       // current token (recently recognized)
    private Token lookahead;   // lookahead token
    private Token.Kind symbol; // always contains kind fo lookahead token
    public int errors;         // error counter
    private boolean showError;  // turn on/off all errors
    boolean showSemanticError; // turn on/off semantic error
    private Scanner scanner;
    Tab table;
    Code code;

    private static final Set // first
            firstMulop, firstActPars, firstAddop, firstExpr, firstType,
            firstFormPars, firstVarDecl, firstMethodDecl, firstConstDecl,
            firstClassDecl, firstBlock, firstDesignator, firstStatement,
            firstFactor, firstTerm;

    private Struct last_type; // type of the last expression

    //(its used in specific cases like methods that calll Designator and view type of objects read internal)
    private Obj last_object; //To methods that require view last obj read
    private Obj last_method_obj;
    private Obj mainMethod;

    private String last_typeident, currentClass;
    private boolean last_array;
    private int last_nfields, nParams;

    private int last_instruction;
    private Operand last_operand;
    private static final Set keywords;

    public Parser(Reader reader) {
        table = new Tab(this);
        code = new Code();
        token = lookahead = new Token(0, 0);
        symbol = Token.Kind.UNKNOWN;
        errors = 0;
        showError = true;
        showSemanticError = true;
        scanner = new Scanner(reader);
        last_method_obj = last_object = Tab.noObj;
        mainMethod = Tab.noObj;
        last_typeident = new String();
        currentClass = "";
        last_array = false;
        nParams = last_nfields = 0;
        last_type = Tab.noType;
        last_instruction = Code.error;
        last_operand = null;
    }

    // initialize symbol sets
    static {
        firstMulop = new HashSet();
        firstMulop.add(Token.Kind.TIMES);
        firstMulop.add(Token.Kind.SLASH);
        firstMulop.add(Token.Kind.REMAINDER);

        firstActPars = new HashSet();
        firstActPars.add(Token.Kind.LEFT_PARENTHESIS);

        firstAddop = new HashSet();
        firstAddop.add(Token.Kind.PLUS);
        firstAddop.add(Token.Kind.MINUS);

        firstDesignator = new HashSet();
        firstDesignator.add(Token.Kind.IDENTIFIER);

        firstFactor = new HashSet(firstDesignator);
        firstFactor.add(Token.Kind.NUMBER);
        firstFactor.add(Token.Kind.CHARACTER);
        firstFactor.add(Token.Kind.NEW);
        firstFactor.add(Token.Kind.LEFT_PARENTHESIS);

        firstTerm = new HashSet(firstFactor);

        firstExpr = new HashSet(firstTerm);
        firstExpr.add(Token.Kind.MINUS);

        firstType = new HashSet();
        firstType.add(Token.Kind.IDENTIFIER);

        firstFormPars = firstType;

        firstVarDecl = firstType;

        firstMethodDecl = new HashSet(firstType);
        firstMethodDecl.add(Token.Kind.VOID);

        firstConstDecl = new HashSet();
        firstConstDecl.add(Token.Kind.FINAL);

        firstClassDecl = new HashSet();
        firstClassDecl.add(Token.Kind.CLASS);

        firstBlock = new HashSet();
        firstBlock.add(Token.Kind.LEFT_BRACE);

        firstStatement = new HashSet(firstBlock);
        firstStatement.addAll(firstDesignator);
        firstStatement.add(Token.Kind.IF);
        firstStatement.add(Token.Kind.WHILE);
        firstStatement.add(Token.Kind.RETURN);
        firstStatement.add(Token.Kind.READ);
        firstStatement.add(Token.Kind.PRINT);
        firstStatement.add(Token.Kind.SEMICOLON);
    }

    static {
        keywords = new HashSet();
        keywords.add("int");
        keywords.add("char");
        keywords.add("class");
        keywords.add("else");
        keywords.add("final");
        keywords.add("if");
        keywords.add("new");
        keywords.add("print");
        keywords.add("program");
        keywords.add("read");
        keywords.add("return");
        keywords.add("void");
        keywords.add("while");
    }

    boolean isKeyword(String s) {
        return keywords.contains(s);
    }

    boolean isClassname(String name) {
        for (Scope s = table.curScope; s != null; s = s.outer) {
            for (Obj o = s.locals; o != null; o = o.next) {
                if (o.name.equals(name) && o.kind == Obj.TYPE
                        && o.type.kind == Struct.CLASS) {
                    return true;
                }
            }
        }
        return false;
    }

    void printObj(Obj obj) {
        System.out.println("-------");
        System.out.println("Name: " + obj.name);
        System.out.println("Obj.Kind:" + Obj.getKindName(obj.kind));
        System.out.println("Address: " + obj.adr);


        //if (obj.type != Tab.noType) {


            System.out.println("Struct.Kind:" + Struct.getKindName(obj.type.kind));


            System.out.println("Number of parameters: " + obj.type.nFields);
        //}
        System.out.println("\n\n");
    }

    Parser printTable() {
        for (Obj obj = table.curScope.locals; obj != null; obj = obj.next) {
            printObj(obj);
            if (obj.type.kind == Struct.CLASS) {
                for (Obj field = obj.type.fields; field != null; field = field.next) {
                    System.out.println("FIELD OF " + obj.name);
                    printObj(field);
                }
            }
        }
        return this;
    }

    private void addType(String type){
        last_typeident = type;
    }

    private void addArray(boolean array){
        last_array = array;
    }

    private void addField(){
        last_nfields++;
    }

    private void clearField(){
        last_nfields = 0;
    }

    public void parse() {
        scan();
        Program();
        if (symbol != Token.Kind.EOF) {
            error("end of file found before end of program");
        }
    }

    void scan() {
        showError = true;
        token = lookahead;
        lookahead = scanner.next();
        symbol = lookahead.kind;
        /*
        System.out.print("line " + lookahead.line + ", column "
                + lookahead.column + ": " + Token.getName(symbol));
        if (symbol == Token.Kind.IDENTIFIER) {
            System.out.print(" (" + lookahead.string + ")");
        }
        if (symbol == Token.Kind.NUMBER) {
            System.out.print(" (" + lookahead.intValue + ")");
        }
        if (symbol == Token.Kind.CHARACTER) {
            System.out.print(" (" + lookahead.charValue + ")");
        }
        System.out.println();
        // */
    }

    boolean tokenMatch(String errorMessage, Token.Kind... kind) {
        for (Token.Kind k : kind) {
            if (symbol == k) {
                scan();
                return true;
            }
        }
        error(errorMessage);
        return false;
    }

    /**
     * Returns true if next token is an identifier, not int, char or classname.
     * Else, give an error and returns false.
     */
    boolean identifierMatch(String errorMessage) {
        if (tokenMatch(errorMessage, Token.Kind.IDENTIFIER)) {
            if (!isKeyword(token.string)) {
                if (!isClassname(token.string)) {
                    return true;
                }
                semanticError("'" + token.string + "' is a classname. Expected non-classname identifier.");
            } else {
                semanticError("'" + token.string + "' is not a non-keyword identifier.");
            }
        }
        return false;
    }

    void error(String errorMessage) {
        if (showError) {
            System.out.printf("Error: line %3d, column %3d: %s\n",
                    lookahead.line, lookahead.column, errorMessage);
            errors++;
            showError = false;
        }
    }

    void semanticError(String errorMessage) {
        if (showSemanticError) {
            error("Semantic: " + errorMessage);
        }
    }

    /** Addop = "+" | "-". */
    Parser Addop() {
        if (tokenMatch("expected '-'or '+'", Token.Kind.PLUS, Token.Kind.MINUS)) {
            if (token.kind == Token.Kind.PLUS) {
                last_instruction = Code.add;
            } else if (token.kind == Token.Kind.MINUS) {
                last_instruction = Code.sub;
            }
        } else {
            last_instruction = Code.error;
        }
        return this;
    }

    /** Mulop = "*" | "/" | "%". */
    Parser Mulop() {
        if (tokenMatch("expected '*', '/' or '%'", Token.Kind.TIMES,
                Token.Kind.SLASH, Token.Kind.REMAINDER)) {
            if (token.kind == Token.Kind.TIMES) {
                last_instruction = Code.mul;
            } else if (token.kind == Token.Kind.SLASH) {
                last_instruction = Code.div;
            } else if (token.kind == Token.Kind.REMAINDER) {
                last_instruction = Code.rem;
            }
        } else {
            last_instruction = Code.error;
        }
        return this;
    }

    /** ClassDecl = "class" ident "{" {VarDecl} "}". */
    Parser ClassDecl() {
        Obj object = Tab.noObj;
        boolean classOK = tokenMatch("Class definition not found. Expected 'class'.", Token.Kind.CLASS);
        if (identifierMatch("Class identifier not valid.") && classOK) {
            currentClass = token.string;
            object = table.insertClass(token.string);
        }
        table.openScope();
        tokenMatch("Class left brace not found.", Token.Kind.LEFT_BRACE);
        clearField();
        while (symbol != Token.Kind.RIGHT_BRACE) {
            if (symbol == Token.Kind.EOF) {
                error("End of file not expected, close class declaration braces.");
                return this;
            }
            VarDecl();
        }
        scan(); // '}' found, get next token
        if (object != Tab.noObj) {
            object.type.fields = table.curScope.locals;
            object.type.nFields = last_nfields;
            clearField();
        }
        table.closeScope();
        currentClass = "";
        return this;
    }

    /**
     * Type() specific for ConstDecl().
     * Accepts only 'int' or 'char' without arrays.
     */
    Parser TypeConstDecl() {
        addType("");
        if (tokenMatch("Expected type identifier.", Token.Kind.IDENTIFIER)) {
            if (token.string.equals("int") || token.string.equals("char")) {
                addType(token.string);
            } else {
                semanticError("'" + token.string + "' is not a valid constant type."
                        + " Expected 'int' or 'char'.");
            }
        }
        addArray(false);
        return this;
    }

    /**
     * ConstDecl = "final" Type ident "=" (number | charConst) ";".
     * <br/>
     * The type of number or charConst must be the same as the type of Type.
     */
    Parser ConstDecl() {
        tokenMatch("Expected final keyword.", Token.Kind.FINAL);
        TypeConstDecl();
        Obj obj = Tab.noObj;
        if (identifierMatch("Expected identifier name.")) {
            obj = table.insertConstVar(token.string, last_typeident, last_array);
        }
        tokenMatch("Expected '='.", Token.Kind.ASSIGN);
        if (tokenMatch("Expected number or character.", Token.Kind.NUMBER,
                Token.Kind.CHARACTER) && !last_typeident.isEmpty()) {
            if (last_typeident.equals("int") && token.kind == Token.Kind.NUMBER) {
                obj.val = token.intValue;
            } else if (last_typeident.equals("char") && token.kind == Token.Kind.CHARACTER) {
                obj.val = token.charValue;
            } else  {
                semanticError("Expected " + last_typeident + " value.");
            }
        }
        tokenMatch("Expected ';' at end of declaration.", Token.Kind.SEMICOLON);
        addArray(false);
        return this;
    }

    /** Relop = "==" | "!=" | ">" | ">=" | "&lt;" | "&lt;=". */
    Parser Relop() {
        if (tokenMatch("Invalid condition operator. Expected any of these: == != > >= < <= ",
                Token.Kind.EQUAL, Token.Kind.NOT_EQUAL, Token.Kind.GREATER,
                Token.Kind.GREATER_EQUAL, Token.Kind.LESS, Token.Kind.LESS_EQUAL)) {
            if (token.kind == Token.Kind.EQUAL) {
                last_instruction = Code.eq;
            } else if (token.kind == Token.Kind.NOT_EQUAL) {
                last_instruction = Code.ne;
            } else if (token.kind == Token.Kind.GREATER) {
                last_instruction = Code.gt;
            } else if (token.kind == Token.Kind.GREATER_EQUAL) {
                last_instruction = Code.ge;
            } else if (token.kind == Token.Kind.LESS) {
                last_instruction = Code.lt;
            } else if (token.kind == Token.Kind.LESS_EQUAL) {
                last_instruction = Code.le;
            }
        } else {
            last_instruction = Code.error;
        }
        return this;
    }

    /** Type = ident ["[" "]"]. */
    Parser Type() {
        addType("");
        if (tokenMatch("Expected type identifier.", Token.Kind.IDENTIFIER)) {
            if (currentClass.equals(token.string)) {
                semanticError("Cannot declare field with the same type ("
                        + token.string + ") of the class.");
            } else {
                Obj o = table.isObjDeclaredAndType(token.string, Obj.TYPE);
                if (o != Tab.noObj) {
                    addType(token.string);
                } else {
                    semanticError("'" + token.string + "' is not a keyword or a classname.");
                }
            }
        }
        addArray(false);
        if (symbol == Token.Kind.LEFT_BRACKET) {
            scan();
            if (tokenMatch("Expected ']'.", Token.Kind.RIGHT_BRACKET)) {
                addArray(true);
            }
        }
        return this;
    }

    /** VarDecl = Type ident {"," ident } ";". */
    Parser VarDecl() {
        Type();
        if (identifierMatch("Expected an identifier at variable declaration.")) {
            table.insertVariable(token.string, last_typeident, last_array);
            addField();
        }
        while (true) {
            if (symbol == Token.Kind.SEMICOLON) {
                scan();
                return this;
            } else if (symbol == Token.Kind.COMMA) {
                scan();
                if (identifierMatch("Expected an identifier after ',' at variable declaration.")) {
                    table.insertVariable(token.string, last_typeident, last_array);
                    addField();
                }
            } else if (symbol == Token.Kind.IDENTIFIER
                    && token.kind == Token.Kind.IDENTIFIER) { // forgot comma?
                error("Expected ',' between '" + token.string
                        + "' and '" + lookahead.string + "'.");
                scan();
                showError = false; // scan() sets showError to true, we don't want that
            } else {
                error("Expected ';' at the end of variable declaration.");
                addArray(false);
                return this; // probably forgot a semicolon, so get out of loop
            }
        }
    }

    /** Term = Factor {Mulop Factor}. */
    Parser Term() {
        Factor();
        while (firstMulop.contains(symbol)) {
            expectedInt(last_type); // in case of first Factor is not an int
            code.load(last_operand); // Factor()
            Mulop();
            int op_mulop = last_instruction;
            Factor();
            expectedInt(last_type);
            code.load(last_operand); // Factor()
            code.put(op_mulop);
        }
        return this;
    }

    /** <pre>
     * Factor = Designator [ActPars]
     *          | number
     *          | charConst
     *          | "new" ident ["[" Expr "]"]
     *          | "(" Expr ")".
     * </pre>
     */
    Parser Factor() {
        if (symbol == Token.Kind.NUMBER) {
            scan();
            last_object = Tab.intObj;
            last_operand = new Operand(token.intValue);
            last_type = last_operand.type = Tab.intType;
        } else if (symbol == Token.Kind.CHARACTER) {
            scan();
            last_object = Tab.charObj;
            last_operand = new Operand(token.charValue);
            last_type = last_operand.type = Tab.charType;
        } else if (symbol == Token.Kind.LEFT_PARENTHESIS) { // "(" Expr ")"
            scan();
            Expr();
            tokenMatch("Expected ')'.", Token.Kind.RIGHT_PARENTHESIS);
        } else if (symbol == Token.Kind.NEW) { // "new" ident ["[" Expr "]"]
            scan();
            Struct newType = Tab.noType;
            if (tokenMatch("Expected identifier.", Token.Kind.IDENTIFIER)) {
                Obj object = table.find(token.string);
                if (object == Tab.noObj) {
                    semanticError("'" + token.string + "' not declared.");
                } else if (object.type.kind != Struct.INT
                        && object.type.kind != Struct.CHAR
                        && !isClassname(token.string)) {
                    semanticError("'" + token.string + "' is not a class.");
                } else {
                    newType = object.type;
                }
            }
            if (symbol == Token.Kind.LEFT_BRACKET) {
                scan();
                Expr();
                expectedInt(last_type);
                code.load(last_operand);
                code.put (Code.newarray);
                if (newType == Tab.charType) {
                    code.put(0);
                } else {
                    code.put(1);
                }
                newType = new Struct(Struct.ARRAY, newType);
                if (tokenMatch("Expected ']'.", Token.Kind.RIGHT_BRACKET)) {
                    last_object = new Obj(Obj.VARIABLE, "new", newType);
                }
            } else {
                if (newType.kind != Struct.CLASS) {
                    semanticError("Class type expected");
                }
                code.put(Code.new_);
                code.put2(newType.nFields);
            }
            last_operand = new Operand(Operand.Stack, newType);
            last_type = newType;

        } else { // Designator [ActPars]
            Designator();
            if (firstActPars.contains(symbol)) {
                if (last_object.kind != Obj.METHOD) {
                    semanticError("'" + token.string + "' is not method.");
                }
                Operand x = last_operand;
                ActPars();
                if (x != null) {
                    if (x.type == Tab.noType)
                        semanticError("procedure called as a function");
                    if (x.obj == Tab.chrObj || x.obj == Tab.ordObj) {
                        // do nothing
                    } else if (x.obj == Tab.lenObj) {
                        code.put(Code.arraylength);
                    } else {
                        code.put(Code.call);
                        code.put2(x.adr);
                    }
                    x.kind = Operand.Stack;
                    last_operand = x;
                }
            }
        }
        // do not reset array because Term calls Factor (and terms view this)
        code.load(last_operand);
        return this;
    }

    /** Designator = ident {"." ident | "[" Expr "]"}. */
    Parser Designator() {
        String identifier = "";
        if (identifierMatch("Identifier not found.")) {
            identifier = token.string;
        }
        //Identifier: variable (its include array), object or method
        Obj object = table.find(identifier);

        Operand x = new Operand(object);
        last_operand = x;
        last_type = last_operand.type;
        while (true) {
            if (symbol == Token.Kind.PERIOD) {
                scan();
                // Designator = Designator "." ident .
                // The type of Designator must be a class.
                // ident must be a field of Designator.
                if (x.type.kind != Struct.CLASS && object != Tab.noObj) {
                    semanticError("'" + object.name + "' is not an object.");
                }
                if (identifierMatch("Expected identifier.")) {
                    identifier = token.string;
                    if (x.type.kind == Struct.CLASS) {
                        code.load(x);
                        Obj field = table.findField(x.type, identifier,
                                "Attribute '" + identifier + "' not declared.");
                        x.kind = Operand.Fld;
                        x.adr = field.adr;
                        last_type = x.type = field.type;
                    }
                    object = table.isObjDeclaredAndType(identifier,
                            Obj.TYPE, Obj.VARIABLE, Obj.METHOD, Obj.CONSTANT);
                }
            } else if (symbol == Token.Kind.LEFT_BRACKET) {
                scan();
                // is previous token an array?
                table.isStructKind(object, Struct.ARRAY,
                        "'" + identifier + "' is not declared as array");
                code.load(x);
                Expr();
                if (x.type.kind == Struct.ARRAY) {
                    if (last_object.type.kind != Struct.INT) {
                        error("array index must be of type int");
                    }
                    code.load(last_operand);
                    x.kind = Operand.Elem;
                    last_type = x.type = x.type.elemType;
                }
                tokenMatch("Expected ']'.", Token.Kind.RIGHT_BRACKET);
            } else {
                last_object = object; // define the last object set in Designator
                last_operand = x;
                return this;
            }
        }
    }

    String objectType(Obj object) {
        if (object.type.kind == Struct.NONE) {
            return Obj.getKindName(object.kind);
        }
        return Struct.getTypeName(object.type);
    }

    boolean expectedInt(Struct type) {
        if (type == Tab.intType)
            return true;
        semanticError("Got " + token.string + ". Expected int type.");
        return false;
    }

    /** Expr = ["-"] Term {Addop Term}. */
    Parser Expr() {
        if (symbol == Token.Kind.MINUS) {
            scan();
            Term();
            expectedInt(last_type);
            if (last_operand.kind == Operand.Con) {
                last_operand.val = -last_operand.val;
            }  else {
                code.load(last_operand);
                code.put(Code.neg);
            }
        } else {
            Term();
        }
        while (firstAddop.contains(symbol)) {
            expectedInt(last_type); // in case of first Term is not an int
            Addop();
            int op_addop = last_instruction;
            Term();
            expectedInt(last_type);
            code.put(op_addop);
        }
        return this;
    }

    /** Condition = Expr Relop Expr. */
    Parser Condition() {
        Expr();
        code.load(last_operand);
        Struct type_expr1 = last_type;
        Obj object = last_object;
        Relop();
        Token operator = token;
        int instruction = last_instruction; // Expr() use last_instruction
        Expr();
        code.load(last_operand);
        Struct type_expr2 = last_type;
        last_instruction = instruction;
        // The types of both expressions must be compatible.
        Struct op1 = object.getType();
        Struct op2 = last_object.getType();
        if (!type_expr1.compatibleWith(type_expr2)) {
            semanticError("Incompatible types: " + object.name
                    + " " + last_object.name);
        } else if (type_expr1.isRefType() && type_expr2.isRefType()
                && operator.kind != Token.Kind.EQUAL
                && operator.kind != Token.Kind.NOT_EQUAL) {
            semanticError("Classes and arrays can only be checked for equality or inequality.");
        }
        return this;
    }

    /**
     * Check numbers of parameters and parameters types of a method.
     * The type of every actual parameter must be assignment compatible
     * with the type of every formal parameter at corresponding positions.
     * Returns true if everything is ok. Else, returns false.
     */
    boolean checkMethodParameters(Obj method, LinkedList<Obj> parameters) {
        // check number of parameters
        if (method.nParams != parameters.size()) {
            semanticError("Wrong number of parameters (" + parameters.size()
                    + ") for '" + method.name + "'."
                    + " Expected " + method.nParams + " parameters.");
            return false;
        }
        boolean ok = true;
        // check types
        Obj pMethod = method.locals;
        if (pMethod == null)
            return false;
        for (Obj p : parameters) {
            if (!p.type.assignableTo(pMethod.type)) {
                ok = false;
                showError = true; // needed to show more than one error
                semanticError("Incompatible parameter type. Got " + objectType(p)
                        + ". Expected " + objectType(pMethod) + ".");
            }
            pMethod = pMethod.next;
        }
        return ok;
    }

    /** ActPars = "(" [ Expr {"," Expr} ] ")". */
    Parser ActPars() {
        Obj object = last_object;
        tokenMatch("Expected '('.", Token.Kind.LEFT_PARENTHESIS);
        LinkedList parameters = new LinkedList();
        if (firstExpr.contains(symbol)) {
            Expr();
            parameters.addLast(last_object);
            code.load(last_operand);
            while (symbol == Token.Kind.COMMA) {
                scan();
                Expr();
                parameters.addLast(last_object);
                code.load(last_operand);
            }
        }
        if (object.kind == Obj.METHOD) {
            checkMethodParameters(object, parameters);
        }
        tokenMatch("Expected ')'.", Token.Kind.RIGHT_PARENTHESIS);
        return this;
    }

    /** Block = "{" {Statement} "}". */
    Parser Block() {
        tokenMatch("Expected: '{'.", Token.Kind.LEFT_BRACE);
        while (firstStatement.contains(symbol)) {
            Statement();
        }
        tokenMatch("Expected: '}'.", Token.Kind.RIGHT_BRACE);
        return this;
    }

    /** <pre>
     * Statement = Designator ("=" Expr | ActPars) ";"
     *               | "if" "(" Condition ")" Statement ["else" Statement]
     *               | "while" "(" Condition ")" Statement
     *               | "return" [Expr] ";"
     *               | "read" "(" Designator ")" ";"
     *               | "print" "(" Expr ["," number] ")" ";"
     *               | Block
     *               | ";".
     * </pre>
     */
    Parser Statement() {
        Obj object = Tab.noObj;
        int adr;
        //Statement = Designator ("=" Expr | ActPars) ";"
        if (firstDesignator.contains(symbol)) {
            Designator();
            Operand x = last_operand;
            if (symbol == Token.Kind.ASSIGN) {
                scan();
                // Designator must denote a variable or an array element.
                object = last_object;
                if (object.kind != Obj.VARIABLE && object.type.kind != Struct.ARRAY) {
                    semanticError("'" + object.name + "' is not a variable or an array.");
                }
                Expr();
                // type of Expr must be assignment compatible with the type of Designator.
                if (last_operand.type.assignableTo(x.type)) {
                    code.assign(x, last_operand);
                } else {
                    semanticError("Incompatible types: "
                            + object.name + " (" + objectType(object)
                            + ")    " + objectType(last_object));
                }
            } else {
                if (!table.isObjType(last_object, Obj.METHOD)) {
                    semanticError("'" + last_object.name + "' is not a method. ");
                }
                ActPars();
                if (x != null) {
                    code.put(Code.call);
                    code.put2(x.adr);
                    if (x.type != Tab.noType)
                        code.put(Code.pop);
                }
            }
            tokenMatch("Expected ';'", Token.Kind.SEMICOLON);

            //"if" "(" Condition ")" Statement ["else" Statement]
        } else if (symbol == Token.Kind.IF) {
            scan();
            tokenMatch("Expected '('.", Token.Kind.LEFT_PARENTHESIS);
            Condition();
            code.putFalseJump(last_instruction, 0);
            adr = code.pc - 2;
            tokenMatch("Expected ')'.", Token.Kind.RIGHT_PARENTHESIS);
            Statement();
            if (symbol == Token.Kind.ELSE) { // ["else" Statement]
                code.putJump(0);
                int adr2 = code.pc - 2;
                code.fixup(adr);
                scan();
                Statement();
                code.fixup(adr2);
            } else {
                code.fixup(adr);
            }

            //"while" "(" Condition ")" Statement
        } else if (symbol == Token.Kind.WHILE) {
            scan();
            tokenMatch("Expected '('.", Token.Kind.LEFT_PARENTHESIS);
            int top = code.pc;
            Condition();
            code.putFalseJump(last_instruction, 0);
            adr = code.pc - 2;
            tokenMatch("Expected ')'", Token.Kind.RIGHT_PARENTHESIS);
            Statement();
            code.putJump(top);
            code.fixup(adr);

            // "return" [Expr] ";"
        } else if (symbol == Token.Kind.RETURN) {
            scan();
            if (firstExpr.contains(symbol)) {
                Expr();
                code.load(last_operand);
                // type of Expr must be assignment compatible with type of current method
                if (last_method_obj.type == Tab.noType) {
                    semanticError("void method '" + last_method_obj.name + "' must not return a value");
                } else if (!last_type.assignableTo(last_method_obj.type)) {
                    if (last_method_obj.type.kind != Struct.NONE) {
                        semanticError("Got " + Struct.getKindName(last_object.type.kind)
                            + ". Expected " + Struct.getKindName(last_method_obj.type.kind) + ".");
                    }
                }
            } else {
                // current method must be declared as void
                if (last_method_obj.kind == Obj.METHOD
                        && last_method_obj.type.kind != Struct.NONE) {
                    semanticError("Method '" + last_method_obj.name + "' must return void.");
                }
            }
            code.put(Code.exit);
            code.put(Code.return_);
            tokenMatch("Expected ';'", Token.Kind.SEMICOLON);

            //"read" "(" Designator ")" ";"
        } else if (symbol == Token.Kind.READ) {
            scan();
            tokenMatch("Expected '('.", Token.Kind.LEFT_PARENTHESIS);
            Designator();
            if (object.kind != Obj.VARIABLE && object.type.kind != Struct.ARRAY) {
                semanticError("'" + last_object.name + "' is not a variable or an array.");
            }
            if (last_object.type.kind != Struct.ARRAY) {
                if(last_object.type.kind != Struct.INT && last_object.type.kind != Struct.CHAR) {
                    semanticError("Expected int or char.");
                }
                // array
            } else if (last_object.type.elemType.kind != Struct.INT
                    && last_object.type.elemType.kind != Struct.CHAR) {
                semanticError("Expected int or char.");
            }

            if (last_operand.type == Tab.intType)
                code.put(Code.read);
            else if (last_operand.type == Tab.charType)
                code.put(Code.bread);
            code.assign(last_operand);

            tokenMatch("Expected ')'", Token.Kind.RIGHT_PARENTHESIS);
            tokenMatch("Expected ';'", Token.Kind.SEMICOLON);
            //"print" "(" Expr ["," number] ")" ";"
        } else if (symbol == Token.Kind.PRINT) {
            scan();
            tokenMatch("Expected '('.", Token.Kind.LEFT_PARENTHESIS);
            Expr();
            if (last_type != Tab.intType && last_type != Tab.charType) {
                semanticError("Expected int or char.");
            } else {
                code.load(last_operand);
            }
            if (symbol == Token.Kind.COMMA) {
                scan();
                if (tokenMatch("Expected a number.", Token.Kind.NUMBER)) {
                    code.load(new Operand(token.intValue));
                }
            } else
                code.put(Code.const0 + 1);

            if (last_operand.type == Tab.intType) {
                code.put(Code.print);
            } else if (last_operand.type == Tab.charType) {
                code.put(Code.bprint);
            } else {
                error("can't print anything else int or char types");
            }
            tokenMatch("Expected ')'", Token.Kind.RIGHT_PARENTHESIS);
            tokenMatch("Expected ';'", Token.Kind.SEMICOLON);

            // Block
        } else if (firstBlock.contains(symbol)) {
            Block();
        } else { // ";"
            tokenMatch("Expected ';'", Token.Kind.SEMICOLON);
        }
        return this;
    }

    /** FormPars = Type ident {"," Type ident}. */
    Parser FormPars() {
        Type();
        if (identifierMatch("Expected identifier.")) {
            table.insertVariable(token.string, last_typeident, last_array);
            addArray(false);
        }
        nParams = 1;
        while (symbol == Token.Kind.COMMA) {
            scan();
            Type();
            if (identifierMatch("Expected identifier.")) {
                table.insertVariable(token.string, last_typeident, last_array);
            }
            ++nParams;
        }
        addArray(false);
        return this;
    }

    /** MethodDecl = (Type | "void") ident "(" [FormPars] ")" {VarDecl} Block. */
    Parser MethodDecl() {
        Obj object = Tab.noObj;
        last_method_obj = Tab.noObj;
        if (symbol == Token.Kind.VOID) {
            scan();
            addType("void");
        } else if (firstType.contains(symbol)) {
            Type();
        } else {
            error("Expected type identifier or 'void'");
        }
        if (identifierMatch("Method name missing.")) {
            object = table.insertMethod(token.string, last_typeident, last_array);
            last_method_obj = object;
            if (token.string.equals("main")) {
                mainMethod = object;
            }
        }
        table.openScope();
        tokenMatch("Expected '('.", Token.Kind.LEFT_PARENTHESIS);
        if (firstFormPars.contains(symbol)) {
            FormPars();
            if (object.kind == Obj.METHOD) {
                object = table.find(object.name);
                object.nParams = nParams;
            }
        }
        if (mainMethod == object)
            code.mainPc = code.pc;
        tokenMatch("Expected ')'.", Token.Kind.RIGHT_PARENTHESIS);
        while (firstVarDecl.contains(symbol)) {
            VarDecl();
        }
        object.adr = code.pc;
        code.put(Code.enter);
        code.put(object.nParams);
        code.put(table.curScope.nVars);
        Block();
        if (object.type == Tab.noType) {
            code.put(Code.exit);
            code.put(Code.return_);
        } else { // end of function reached without a return statement
            code.put(Code.trap);
            code.put(1);
        }
        addArray(false);
        object.locals = table.curScope.locals;
        table.closeScope();
        return this;
    }

    /**
     * Program = "program" ident {ConstDecl | ClassDecl | VarDecl} "{" {MethodDecl} "}".
     *
     * A program must contain a method named main.
     * It must be declared as a void method and must not have parameters.
     */
    Parser Program() {
        tokenMatch("Expected 'program'.", Token.Kind.PROGRAM);
        tokenMatch("Program name not declared.", Token.Kind.IDENTIFIER);
        // table.openScope();
        table.insert(Obj.PROGRAM, token.string, Tab.noType);
        while (true) {
            if (firstConstDecl.contains(symbol)) {
                ConstDecl();
            } else if (firstClassDecl.contains(symbol)) {
                ClassDecl();
            } else if (firstVarDecl.contains(symbol)) {
                VarDecl();
            } else {
                break;
            }
        }
        tokenMatch("Expected '{'.", Token.Kind.LEFT_BRACE);
        while (firstMethodDecl.contains(symbol)) {
            MethodDecl();
        }
        if (mainMethod == Tab.noObj) {
            semanticError("Program must contain a 'main' method.");
        } else if (mainMethod.nParams != 0 || mainMethod.type != Tab.noType) {
            semanticError("'main' method must be void and not have parameters.");
        }
        tokenMatch("Expected '}'.", Token.Kind.RIGHT_BRACE);
        code.dataSize = table.curScope.nVars;
//        Tab.dumpScope(table.curScope.locals);
        return this;
    }

    Parser eof() {
        if (symbol != Token.Kind.EOF) {
            error("Expected EOF.");
        }
        return this;
    }

    Parser printCode() {
        try {
            code.write(System.out);
            System.out.println("\n");
        } catch (Exception ex) {
            System.err.println("ERRO printCode()");
        }
        return this;
    }
}
