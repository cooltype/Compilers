/* MicroJava Parser Semantic Tester
 *
 * Test only semantics. The grammar in all tests are correct.
 */
package MicroJava;

import java.io.*;

public class TestParserSemantic {

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
        System.out.println(header);
        Parser parser = new Parser(reader);
        parser.parse();
        System.out.println(parser.errors + " errors detected\n");
    }

    private static Parser createParser(String input) {
        Parser parser = new Parser(new StringReader(input));
        parser.showSemanticError = true;
        parser.code.showError = false;
        parser.scan(); // get first token
        return parser;
    }

    private static void executeTests() {
        testClassDecl();
        testCondition();
        testConstDecl();
        testDesignator();
        testExpr();
        testFactor();
        testFormPars();
        testMethodDecl();
        testProgram();
        testStatement();
        testTerm();
        testType();
        testVarDecl();
    }

    private static void testClassDecl() {
        System.out.println("Test: ClassDecl");
        createParser("class A {}").ClassDecl();
        createParser("class B { int b; }").ClassDecl();
        createParser("class C { int c1; char[] c2; }").ClassDecl();
        createParser("class D {} class E { D d; }").ClassDecl().ClassDecl();

        System.out.println("Test: ClassDecl errors");
        createParser("class int {}").ClassDecl();
        createParser("class char {}").ClassDecl();
        createParser("class F { F[] f; }").ClassDecl();
        createParser("class H { int H; }").ClassDecl();
        createParser("class J { int j; char j; }").ClassDecl();
        // invalid type (X not declared)
        createParser("class K { X k; }").ClassDecl();
        // atribute name is the same of the type of variable
        createParser("class L {} class M { L L; }").ClassDecl().ClassDecl();
        createParser("class N { N N; }").ClassDecl();
        System.out.println();
    }

    private static void testCondition() {
        System.out.println("Test: Condition");
        createParser("2 == 3").Condition();
        createParser("int[] a, b; a == b").VarDecl().Condition();
        createParser("char[] c, d; c != d").VarDecl().Condition();
        createParser("class E{} E e, f; e == f   e != f")
                .ClassDecl().VarDecl().Condition().Condition();

        System.out.println("Test: Condition errors");
        createParser("2 + 3 == 'a'").Condition();
        createParser("int[] ia, ib; ia > ib").VarDecl().Condition();
        createParser("class X{} X x, y; x <= y").ClassDecl().VarDecl().Condition();
        createParser("class W{ int[] a; } W w; int[] b; w.a <= b")
                .ClassDecl().VarDecl().VarDecl().Condition();
        createParser("class Z{} Z z; int[] r; r == z")
                .ClassDecl().VarDecl().VarDecl().Condition();
        System.out.println();
    }

    private static void testConstDecl() {
        System.out.println("Test: ConstDecl");
        createParser("final int b = 2;").ConstDecl();
        createParser("final char c = 'c';").ConstDecl();

        System.out.println("Test: ConstDecl errors");
        createParser("final int d = 'd';").ConstDecl();
        createParser("final char e = 1;").ConstDecl();
        // array is not accept
        createParser("final int[] f = 1;").ConstDecl();
        createParser("final char[] g = 'g';").ConstDecl();
        System.out.println();
    }

    private static void testDesignator() {
        System.out.println("Test: Designator");
        createParser("class A {} A a; a = new A; a")
                .ClassDecl().VarDecl().Statement().Designator();
        createParser("class B { int bb; } B b; b = new B; b.bb")
                .ClassDecl().VarDecl().Statement().Designator();
        createParser("class C { char c1; int c2; } C c; c = new C; c.c1 c.c2")
                .ClassDecl().VarDecl().Statement().Designator().Designator();
        createParser("int[] d; d[2]").VarDecl().Designator();

        System.out.println("Test: Designator errors");
        createParser("int x; x.y").VarDecl().Designator();
        createParser("class M { int m; } M.m").ClassDecl().Designator();
        System.out.println();
    }

    private static void testExpr() {
        System.out.println("Test: Expr");
        createParser("2 + 3").Expr();
        createParser("-4").Expr();
        createParser("-5 * 6").Expr();
        createParser("int a; a * 6").VarDecl().Expr();
        createParser("int b, c, d; b * 6 + c * d").VarDecl().Expr();

        System.out.println("Test: Expr errors");
        createParser("-'a'").Expr();
        createParser("-2 * 'b'").Expr();
        createParser("-3 * 'c' * 4").Expr();
        createParser("-5 * 'd' * 6 + 7").Expr();
        createParser("char e;  e - 'f' % 'g'").VarDecl().Expr();
        createParser("void m(){}  m + 8").MethodDecl().Expr();
        System.out.println();
    }

    private static void testFactor() {
        System.out.println("Test: Factor");
        // new
        createParser("new int[2]").Factor();
        createParser("int size; new char[size]").VarDecl().Factor();
        createParser("class A {}  new A").ClassDecl().Factor();
        createParser("class B {}  new B[2]").ClassDecl().Factor();
        // designator: variable
        createParser("int i; i").VarDecl().Factor();
        // designator: method */
        createParser("int  m1(int i, int j) {}    m1(2, 3)").MethodDecl().Factor();

        System.out.println("Test: Factor errors");
        // new
        createParser("new int").Factor();
        createParser("new char").Factor();
        createParser("new X").Factor();
        createParser("new Y[2]").Factor();
        createParser("class G {}  new G['b']").ClassDecl().Factor();
        // designator: variable
        createParser("int w; w()").VarDecl().Factor();
        // designator: method
        createParser("int  me1() {}    me1(1)").MethodDecl().Factor();
        createParser("int  me2(int i, int j) {}    me2(2)").MethodDecl().Factor();
        createParser("char me3(char c) {}    me3(3)").MethodDecl().Factor();
        createParser("char me4(int i, int j, char k) {}    me4(4, 'c', 6)").MethodDecl().Factor();
        // void methods (procedures) cannot be used in a expression context.
        // Factor is always called in an expression context (Expr -> Term -> Factor)
        createParser("void m5() {}    m5()").MethodDecl().Factor();
        createParser("void m6(char c) {}    m6('c')").MethodDecl().Factor();
        System.out.println();
    }

    private static void testFormPars() {
        System.out.println("Test: FormPars");
        createParser("int i, char c, int[] ia, char[] ca").FormPars();
        createParser("class A {}  A a, A[] aa").ClassDecl().FormPars();

        System.out.println("Test: FormPars errors");
        createParser("int char, int char").FormPars();
        createParser("B b, C[] c").FormPars();
        System.out.println();
    }

    private static void testMethodDecl() {
        System.out.println("Test: MethodDecl");
        createParser("int[] a(int[] ia, char[] ca) int i; char c; {}").MethodDecl();
        createParser("void b() int i; char c; {}").MethodDecl();
        createParser("class C {} \n C c() {}").ClassDecl().MethodDecl();
        createParser("class D {} \n D[] d(D dp) D dv; {}").ClassDecl().MethodDecl();

        System.out.println("Test: MethodDecl errors");
        createParser("int char() {}").MethodDecl();
        createParser("void m(G g) {}").MethodDecl();
        System.out.println();
    }

    private static void testProgram() {
        System.out.println("Test: Program");
        createParser("program P { void main() {} }").Program();
        createParser("program ABC {"
                + "     void f() {}"
                + "     void main() { return ; }"
                + "     int g(int x) { return x*2; }"
                + "   }").Program();

        System.out.println("Test: Program errors");
        createParser("program E1 { }").Program();
        createParser("program E2 { int main() { } }").Program();
        createParser("program E3 { void main(int i) { } }").Program();
        System.out.println();
    }

    private static void testStatement() {
        System.out.println("Test: Statement");
        // Designator "=" Expr ";"
        createParser("int b; b = 2;").VarDecl().Statement();
        createParser("char c, d; c = d;").VarDecl().Statement();
        createParser("int[] e; e[3] = 3;").VarDecl().Statement();
        createParser("class A { int i; } A a; a.i = 4;").ClassDecl().VarDecl().Statement();
        createParser("int[] ia; ia = new int[5];").VarDecl().Statement();
        createParser("char[] ca; ca = new char[6];").VarDecl().Statement();
        // Designator ActPars ";"
        createParser("void f() {} f();").MethodDecl().Statement();
        //  "print" "(" Expr ["," number] ")" ";"
        createParser("print(2); print('c');").Statement().Statement();
        createParser("print(3, 4);").Statement();
        // "read" "(" Designator ")" ";"
        createParser("int i; read(i);").VarDecl().Statement();
        createParser("char c; read(c);").VarDecl().Statement();
        createParser("char[] ca; read(ca[2]);").VarDecl().Statement();
        createParser("class C {int i;} C o; read(o.i);").ClassDecl().VarDecl().Statement();
        //  "return" [Expr] ";"
        createParser("void f() { return ;}").MethodDecl();
        createParser("int g() { return 2;}").MethodDecl();
        createParser("char h() { return 'c';}").MethodDecl();

        System.out.println("Test: Statement errors");
        // Designator "=" Expr
        createParser("int b; b = 'c';").VarDecl().Statement();
        createParser("char[] c; c[4] = 4;").VarDecl().Statement();
        createParser("class A { int i; } A a; a.i = '4';").ClassDecl().VarDecl().Statement();
        createParser("int[] ia; ia = new char[5];").VarDecl().Statement();
        createParser("char[] ca; ca = new int[6];").VarDecl().Statement();
        // Designator ActPars
        createParser("int g; g();").VarDecl().Statement();
        //  "print" "(" Expr ["," number] ")" ";"
        createParser("class C {}  C c;  print(c);").ClassDecl().VarDecl().Statement();
        createParser("print(5, 'd');").Statement();
        // "read" "(" Designator ")" ";"
        createParser("class C {}  C c; read(c);").ClassDecl().VarDecl().Statement();
        //  "return" [Expr] ";"
        createParser("void p() { return 1; }").MethodDecl();
        createParser("int q() { return 'c';}").MethodDecl();
        createParser("char r() { return 2;}").MethodDecl();
        createParser("int s() { return ;}").MethodDecl();
        System.out.println();
    }

    private static void testTerm() {
        System.out.println("Test: Term");
        createParser("2 * 3").Term();
        createParser("4").Term();
        createParser("5 * (-6)").Term();
        createParser("'a'").Term();
        createParser("int a, b; a * (-7) * b").VarDecl().Term();
        createParser("char a; a").VarDecl().Term();

        System.out.println("Test: Term errors");
        createParser("2 * 'b'").Term();
        createParser("'c' * 3").Term();
        createParser("char d; d * 'e'").VarDecl().Term();
        System.out.println();
    }

    private static void testType() {
        System.out.println("Test: Type");
        createParser("int").Type();
        createParser("int[]").Type();
        createParser("char").Type();
        createParser("char[]").Type();
        createParser("class A {}  A").ClassDecl().Type();

        System.out.println("Test: Type errors");
        createParser("class").Type();
        createParser("MyClass").Type();
        System.out.println();
    }

    private static void testVarDecl() {
        System.out.println("Test: VarDecl");
        createParser("int x;").VarDecl();
        createParser("int x, y;").VarDecl();
        createParser("int x, X;").VarDecl(); // case-sensitive variables
        createParser("int x, y, z, X, Y, Z;").VarDecl();

        System.out.println("Test: VarDecl errors");
        createParser("int int;").VarDecl();
        createParser("int char;").VarDecl();
        createParser("char int;").VarDecl();
        createParser("char char;").VarDecl();
        createParser("int s, s;").VarDecl();
        createParser("int t, char, t, int;").VarDecl();
        createParser("int x, w, p, q, r, w;").VarDecl();
        System.out.println();
    }

}
