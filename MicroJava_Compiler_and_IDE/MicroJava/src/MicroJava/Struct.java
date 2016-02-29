/* MicroJava Type Structures
 *
 * A type structure stores the type attributes of a declared object.
 */
package MicroJava;


import java.util.*;

class Struct {

    public static final int // structure kinds
            NONE  = 0,
            INT   = 1,
            CHAR  = 2,
            ARRAY = 3,
            CLASS = 4;
    public int kind;    // NONE, INT, CHAR, ARRAY, CLASS
    public Struct elemType; // ARRAY: element type
    public int nFields;  // CLASS: number of fields
    public Obj fields;   // CLASS: fields
    private final static Map hash;

    public Struct(int kind) {
        this.kind = kind;
    }

    public Struct(int kind, Struct elemType) {
        this.kind = kind;
        this.elemType = elemType;
    }

    // Checks if this is a reference type
    public boolean isRefType() {
        return kind == CLASS || kind == ARRAY;
    }

    // Checks if two types are equal
    public boolean equals(Struct other) {
        if (kind == ARRAY) {
            return other.kind == ARRAY && other.elemType == elemType;
        } else {
            return other == this;
        }
    }

    // Checks if two types are compatible (e.g. in a comparison)
    public boolean compatibleWith(Struct other) {
        return this.equals(other)
                || (this == Tab.nullType && other.isRefType())
                || (other == Tab.nullType && this.isRefType());
    }

    // Checks if an object with type "this" can be assigned to an object with type "dest"
    public boolean assignableTo(Struct dest) {
        return this.equals(dest)
                || (this == Tab.nullType && dest.isRefType())
                || (this.kind == ARRAY && dest.kind == ARRAY && dest.elemType == Tab.noType);
    }

    static {
        hash = new HashMap();
        hash.put(NONE, "none");
        hash.put(INT, "int");
        hash.put(CHAR, "char");
        hash.put(ARRAY, "array");
        hash.put(CLASS, "class");
    }

    public static String getKindName(int kind) {
        if (hash.containsKey(kind)) {
            return (String) hash.get(kind);
        }
        return "Not defined";
    }

    public static String getTypeName(Struct struct) {
        if (struct.kind == ARRAY) {
            return Struct.getKindName(struct.elemType.kind) + "[]";
        }
        return Struct.getKindName(struct.kind);
    }
}
