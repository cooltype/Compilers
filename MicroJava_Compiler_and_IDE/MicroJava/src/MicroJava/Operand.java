/* MicroJava Code Operands
 *
 * An Operand stores the attributes of a value during code generation.
 */
package MicroJava;

public class Operand {

    public static final int // item kinds
            Con    = 0,
            Local  = 1,
            Static = 2,
            Stack  = 3,
            Fld    = 4,
            Elem   = 5,
            Meth   = 6;
    public int kind; // Con, Local, Static, Stack, Fld, Elem, Meth
    public Struct type; // item type
    public Obj obj;  // Meth
    public int val;  // Con: value
    public int adr;  // Local, Static, Fld, Meth: address

    public Operand(Obj o) {
        type = o.type;
        val = o.val;
        adr = o.adr;
        kind = Stack; // default
        switch (o.kind) {
            case Obj.CONSTANT:
                kind = Con;
                break;
            case Obj.VARIABLE:
                if (o.level == 0) {
                    kind = Static;
                } else {
                    kind = Local;
                }
                break;
            case Obj.METHOD:
                kind = Meth;
                obj = o;
                break;
            case Obj.TYPE:
                System.out.println("type identifier not allowed here");
                break;
            default:
                System.out.println("wrong kind of identifier");
                break;
        }
    }

    public Operand(int val) {
        this.kind = Con;
        this.val = val;
        this.type = Tab.intType;
    }

    public Operand(int kind, Struct type) {
        this.kind = kind;
        this.val = -1;
        this.type = type;
    }

    public Operand(int kind, int val, Struct type) {
        this.kind = kind;
        this.val = val;
        this.type = type;
    }
}
