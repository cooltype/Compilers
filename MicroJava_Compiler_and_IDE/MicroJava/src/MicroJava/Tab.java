/* MicroJava Symbol Table
 *
 * This class manages scopes and inserts and retrieves objects.
 */
package MicroJava;

class Tab {

    Scope curScope; // current scope
    // predefined scope
    static final Scope universe;
    // predefined types
    static final Struct intType, charType, nullType, noType;
    // predefined objects
    static final Obj intObj, charObj, nullObj,
            chrObj, ordObj, lenObj, noObj;

    private Parser parser; // used to emit errors.
                           // The parser knows where the error is (line, column)
                           // and this class knows the semantic error message.

    static {
        // create predeclared types
        intType = new Struct(Struct.INT);
        charType = new Struct(Struct.CHAR);
        nullType = new Struct(Struct.CLASS);
        noType = new Struct(Struct.NONE);

        universe = new Scope(null, -1);
        // insert predeclared objects
        intObj = insertObject(universe, Obj.TYPE, "int", intType, null);
        charObj = insertObject(universe, Obj.TYPE, "char", charType, null);
        nullObj = insertObject(universe, Obj.CONSTANT, "null", nullType, null);

        //chr method
        chrObj = insertObject(universe, Obj.METHOD, "chr", charType, null);
        chrObj.locals = new Obj(Obj.VARIABLE, "i", intType);
        chrObj.nParams = 1;

        //ord method
        ordObj = insertObject(universe, Obj.METHOD, "ord", intType, null);
        ordObj.locals = new Obj(Obj.VARIABLE, "ch", charType);
        ordObj.nParams = 1;

        //len method
        lenObj = insertObject(universe, Obj.METHOD, "len", intType, null);
        lenObj.locals = new Obj(Obj.VARIABLE, "a", new Struct(Struct.ARRAY, nullType));
        lenObj.nParams = 1;

        noObj = new Obj(Obj.VARIABLE, "noObj", noType);
    }

    Tab(Parser parser) {
        // create global scope (must be different of universe)
        curScope = new Scope(universe, universe.level + 1);
        this.parser = parser;
    }

    void setNumberMethodParams(Obj method, int n) {
        method.nParams = n;
    }

    private Struct typeVar(String identifier, String varType, boolean isArray) {
        Struct tvar = noType;
        if (isArray) {
            tvar = new Struct(Struct.ARRAY);
        }
        if (varType.equals("int")) {
            if (isArray) {
                tvar.elemType = intType;
            } else {
                tvar = intType;
            }
        } else if (varType.equals("char")) {
            if (isArray) {
                tvar.elemType = charType;
            } else {
                tvar = charType;
            }
        } else if (varType.equals("void")) { // for method return type
            return noType;
        } else { // user defined class
            Obj object = find(varType);
            if (object == noObj) {
                if (varType.isEmpty()) {
                    error("Invalid type for '" + identifier + "'.");
                } else {
                    error("Type '" + varType +"' is not declared.");
                }
                return noType;
            }
            if (isArray) {
                tvar.elemType = object.type; // and this array is a found user definied class
            } else {
                tvar = object.type;
            }
        }
        return tvar;
    }

    /** Returns field with name in type if it exists. Else, returns noObj. */
    Obj findField(Struct type, String field, String err) {
        for (Obj p = type.fields; p != null; p = p.next) {
            if (p.name.equals(field)) {
                return p;
            }
        }
        error(field + ": " + err);
        return Tab.noObj;
    }

    void error(String errorMessage) {
        error(errorMessage, parser);
    }

    private static void error(String errorMessage, Parser parser) {
        parser.semanticError(errorMessage);
    }

    public boolean isStructKind(Obj object, int kind, String err) {
        if (object.type.kind == kind) {
            return true;
        }
        error(err);
        return false;
    }

    void openScope() {
        curScope = new Scope(curScope, curScope.level + 1);
    }

    void closeScope() {
        curScope = curScope.outer;
    }

    Obj insertConstVar(String identifier, String varType, boolean isArray) {
        return insert(Obj.CONSTANT, identifier, typeVar(identifier, varType, isArray));
    }

    Obj insertVariable(String identifier, String varType, boolean isArray) {
        return insert(Obj.VARIABLE, identifier, typeVar(identifier, varType, isArray));
    }

    Obj insertMethod(String identifier, String varType, boolean isArray) {
        return insert(Obj.METHOD, identifier, typeVar(identifier, varType, isArray));
    }

    Obj insertClass(String identifier) {
        return insert(Obj.TYPE, identifier, new Struct(Struct.CLASS));
    }

    Obj insertObject(String identifier) {
        return insert(Obj.VARIABLE, identifier, new Struct(Struct.CLASS));
    }

    Obj insert(int kind, String name, Struct type) {
        return insertObject(curScope, kind, name, type, parser);
    }

    /**
     * Insert a new object with kind, name and type in scope
     * only if the object was not already declared.
     * Returns the new object if it was inserted, else returns noObj.
     */
    private static Obj insertObject(Scope scope, int kind, String name,
                                    Struct type, Parser parser) {
        // create object node
        Obj object = new Obj(kind, name, type);
        if (kind == Obj.VARIABLE) {
            object.adr = scope.nVars;
            scope.nVars++;
            object.level = scope.level;
        }
        // append object node
        Obj last = null;
        for (Obj o = scope.locals; o != null; o = o.next) {
            if (o.name.equals(name)) {
                error("'" + name + "' was already declared.", parser);
                return noObj;
            }
            last = o;
        }

        if (last == null) {
            scope.locals = object;
        } else {
            last.next = object;
        }
        return object;
    }

    /**
     * Returns object with name from the current scope.
     * If no object is found, returns noObj.
     */
    Obj find(String name) {
        for (Scope s = curScope; s != null; s = s.outer) {
            for (Obj p = s.locals; p != null; p = p.next) {
                if (p.name.equals(name)) {
                    return p;
                }
            }
        }
//        error(name + " is undeclared");
        return noObj;
    }

    /** Returns true if object kind matches any kind arguments.*/
    boolean isObjType(Obj object, int... kind) {
        for (int k : kind) {
            if (object.kind == k) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns object with name and matches any objkind.
     * If the object does not exist, returns noObj.
     */
    Obj isObjDeclaredAndType(String name, int... objkind) {
        for (Scope s = curScope; s != null; s = s.outer) {
            Obj o = objDeclaredAndType(s.locals, name, objkind);
            if (o != noObj) {
                return o;
            }
        }
        return noObj;
    }

    private Obj objDeclaredAndType(Obj initial, String name, int... objkind) {
        for (Obj object = initial; object != null; object = object.next) {
            if (object != noObj) {
                for (int k : objkind) {
                    if (isObjType(object, k) && name.equals(object.name)) {
                        return object;
                    }
                }
            }
            // search in class fields
            if (object.type.kind == Struct.CLASS) {
                for (Obj field = object.type.fields; field != null; field = field.next) {
                    Obj o = objDeclaredAndType(field, name, objkind);
                    if (o != noObj) {
                        return o;
                    }
                }
            }
        }
        return noObj;
    }

    static void dumpStruct(Struct type) {
        String kind;
        switch (type.kind) {
            case Struct.INT:
                kind = "INT  ";
                break;
            case Struct.CHAR:
                kind = "CHAR ";
                break;
            case Struct.ARRAY:
                kind = "ARRAY  ";
                break;
            case Struct.CLASS:
                kind = "CLASS";
                break;
            default:
                kind = "NONE";
        }
        System.out.print(kind + " ");
        if (type.kind == Struct.ARRAY) {
            System.out.print(type.nFields + " (");
            dumpStruct(type.elemType);
            System.out.print(")");
        }
        if (type.kind == Struct.CLASS) {
            System.out.println(type.nFields + "<<");
            for (Obj o = type.fields; o != null; o = o.next) {
                dumpObj(o);
            }
            System.out.print(">>");
        }
    }

    static void dumpObj(Obj o) {
        String kind;
        switch (o.kind) {
            case Obj.CONSTANT:
                kind = "CONSTANT ";
                break;
            case Obj.VARIABLE:
                kind = "VARIABLE ";
                break;
            case Obj.TYPE:
                kind = "TYPE";
                break;
            case Obj.METHOD:
                kind = "METHOD";
                break;
            default:
                kind = "NONE";
        }
        System.out.print(kind + " " + o.name + " " + o.val + " " + o.adr
                + " " + o.level + " " + o.nParams + " (");
        dumpStruct(o.type);
        System.out.println(")");
    }

    static void dumpScope(Obj head) {
        System.out.println("--------------");
        for (Obj o = head; o != null; o = o.next) {
            dumpObj(o);
        }
        for (Obj o = head; o != null; o = o.next) {
            if (o.kind == Obj.METHOD || o.kind == Obj.PROGRAM) {
                dumpScope(o.locals);
            }
        }
    }
}
