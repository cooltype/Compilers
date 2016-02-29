/* MicroJava Symbol Table Scopes */
package MicroJava;

class Scope {

    Scope outer; // parent scope
    Obj locals;  // local variables of this scope
    int nVars;   // number of variables in this scope
    int level;

    Scope(Scope outer, int level) {
        this.outer = outer;
        this.level = level;
    }
}
