/* MicroJava Symbol Table Objects
 *
 * Every named object in a program is stored in an Obj node.
 * Every scope has a list of objects declared in this scope.
 */
package MicroJava;

import java.util.*;

class Obj {

    public static final int // object kinds
            CONSTANT  = 0,
            VARIABLE  = 1,
            TYPE      = 2,
            METHOD    = 3,
            PROGRAM   = 4;


    public int kind;    // CONSTANT, VARIABLE, TYPE, METHOD, PROGRAM
    public String name; // object name
    public Struct type; // object type
    public int val;     // CONSTANT: value
    public int adr;     // VARIABLE, Math: address
    public int level;   // VARIABLE: declaration level
    public int nParams; // METHOD: number of parameters
    public Obj locals;  // METHOD: parameters and local objects
    public Obj next;    // next local object in this scope
    private final static Map hash;

    public Obj(int kind, String name, Struct type) {
        this.kind = kind;
        this.name = name;
        this.type = type;
    }

     static{
        hash = new HashMap();
        hash.put(CONSTANT, "final");
        hash.put(VARIABLE, "variable");
        hash.put(TYPE, "type");
        hash.put(METHOD, "method");
        hash.put(PROGRAM, "program");
    }

    public static String getKindName(int kind){
        if(hash.containsKey(kind)){
            return (String) hash.get(kind);
        }

      return "Not defined";
    }

    Struct getType() {
        if (this.type.kind == Struct.ARRAY)
            return this.type.elemType;
        return this.type;
    }
}
