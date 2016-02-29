/* MicroJava Parser Tester
 *
 * Grammar tests without semantic.
 */
package MicroJava;

import java.io.*;

public class TestParser {

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
        parser.showSemanticError = false;
        parser.code.showError = false;
        parser.scan(); // get first token
        return parser;
    }

    private static void executeTests() {
        testActPars();
        testAddop();
        testBlock();
        testClassDecl();
        testCondition();
        testConstDecl();
        testDesignator();
        testExpr();
        testFactor();
        testFormPars();
        testMethodDecl();
        testMulop();
        testProgram();
        testRelop();
        testStatement();
        testTerm();
        testType();
        testVarDecl();
    }

    private static void testActPars() {
        System.out.println("Test: ActPars");
        createParser("()").ActPars();
        createParser("(2)").ActPars();
        createParser("(-2)").ActPars();
        createParser("(-2+3)").ActPars();
        createParser("(2*3)").ActPars();
        createParser("(-2*3)").ActPars();
        createParser("(2,3*4*5)").ActPars();
        createParser("(-2*3+4)").ActPars();
        createParser("(-2*3+4*5+6+7)").ActPars();
        createParser("(-2*3+4, 5+6+7)").ActPars();
        createParser("(-2*3+4, 5+6+7, 8*9+0)").ActPars();

        System.out.println("Test: ActPars errors");
        createParser("(").ActPars();
        createParser("(+2)").ActPars();
        createParser("(2,)").ActPars();
        createParser("(2,3+4,)").ActPars();
        System.out.println();
    }

    private static void testAddop() {
        System.out.println("Test: Addop");
        createParser("+").Addop();
        createParser("-").Addop();

        System.out.println("Test: Addop errors");
        createParser("1").Addop();
        createParser("=").Addop();
        System.out.println();
    }

    private static void testBlock() {
        System.out.println("Test: Block");
        createParser("{ }").Block();
        createParser("{ ; }").Block();
        createParser("{ return 2; }").Block();
        createParser("{ x = 3; return 2; }").Block();

        System.out.println("Test: Block errors");
        createParser(" ; }").Block();
        createParser("{ ;").Block();
        System.out.println();
    }

    private static void testClassDecl() {
        System.out.println("Test: ClassDecl");
        createParser("class A { }").ClassDecl();
        createParser("class B { int x; }").ClassDecl();

        System.out.println("Test: ClassDecl errors");
        createParser("C { }").ClassDecl();
        createParser("class { }").ClassDecl();
        createParser("class D }").ClassDecl();
        createParser("class E int y; }").ClassDecl();
        createParser("class F { ").ClassDecl();
        createParser("class G { int z; ").ClassDecl();
        System.out.println();
    }

    private static void testCondition() {
        System.out.println("Test: Condition");
        createParser("2 == 3").Condition();

        System.out.println("Test: Condition errors");
        createParser("  > 4").Condition();
        createParser("5   6").Condition();
        createParser("7 <  ").Condition();
        System.out.println();
    }

    private static void testConstDecl() {
        System.out.println("Test: ConstDecl");
        createParser("final int  x = 2;").ConstDecl();
        createParser("final int  y = 'Y';").ConstDecl();
        createParser("final char w = 3;").ConstDecl();
        createParser("final char z = 'Z';").ConstDecl();

        System.out.println("Test: ConstDecl errors");
        createParser("      int  c = 1;").ConstDecl();
        createParser("final      d = 2;").ConstDecl();
        createParser("final int    = 3;").ConstDecl();
        createParser("final char e   'E';").ConstDecl();
        createParser("final char f =  ;").ConstDecl();
        createParser("final char g = 5 ").ConstDecl();
        System.out.println();
    }

    private static void testDesignator() {
        System.out.println("Test: Designator");
        createParser("x").Designator();
        createParser("x.y").Designator();
        createParser("x.y.z.w").Designator();
        createParser("x[2]").Designator();
        createParser("x[3][4]").Designator();
        createParser("x[5].y[6].z[7][8]").Designator();

        System.out.println("Test: Designator errors");
        createParser("x. ").Designator();
        createParser(" .y").Designator();
        createParser("x. .z.w").Designator();
        createParser("x[]").Designator();
        createParser("x[3 [4]").Designator();
        System.out.println();
    }

    private static void testExpr() {
        System.out.println("Test: Expr");
        createParser("2").Expr();
        createParser("-2 + 3").Expr();
        createParser("2 + 3 * 4 + 5 / 6 * (7 + 8) % 9").Expr();
        createParser("-(x + ((6 + 3) / 3) + y) * z").Expr();
        createParser("-x + 'y' * z").Expr();
        createParser(" - 'y' ").Expr();
        createParser(" - '\t' + 3 * 'z' % '\n' ").Expr();
        createParser(" -'\n'[1].wtf ").Expr();

        System.out.println("Test: Expr errors");
        createParser("2+").Expr();
        createParser("x++").Expr();
        createParser("--i").Expr();
        createParser("(3 'z')").Expr();
        createParser("(a / (b + c) * e").Expr();
        System.out.println();
    }

    private static void testFactor() {
        System.out.println("Test: Factor");
        createParser("x").Factor();
        createParser("2").Factor();
        createParser(" 'c' ").Factor();
        createParser("new var").Factor();
        createParser("new char[size]").Factor();
        createParser("new a[2 * t + '&']").Factor();
        createParser("(abc + 2 * 3)").Factor();

        System.out.println("Test: Factor errors");
        createParser("new").Factor();
        createParser("new int[]").Factor();
        createParser("new char[size").Factor();
        createParser("new a[2 * t + ]").Factor();
        createParser("(abc (-2 + 3) * 4").Factor();
        System.out.println();
    }

    private static void testFormPars() {
        System.out.println("Test: FormPars");
        createParser("int x").FormPars();
        createParser("char c, char[] d").FormPars();
        createParser("int[] y, char[] e, int z, char f").FormPars();

        System.out.println("Test: FormPars errors");
        createParser("int x,").FormPars();
        createParser("char c, char[]").FormPars();
        System.out.println();
    }

    private static void testMethodDecl() {
        System.out.println("Test: MethodDecl");
        createParser("void f() { }").MethodDecl();
        createParser("int g() { if ('\n' == 2) return ; }").MethodDecl();
        createParser("char[] h(int n) { return new array[n]; }").MethodDecl();
        createParser("int F(int a, int b) int i, j; { return a*i + b*j; }").MethodDecl();

        System.out.println("Test: MethodDecl errors");
        createParser("int g() if ('\n' == 2) return ; }").MethodDecl();
        createParser("int gg() { ").MethodDecl();
        createParser("char[] int n) { }").MethodDecl();
        createParser("int F(int a, int b) int i, j { }").MethodDecl();
        System.out.println();
    }

    private static void testMulop() {
        System.out.println("Test: Mulop");
        createParser("*").Mulop();
        createParser("/").Mulop();
        createParser("%").Mulop();

        System.out.println("Test: Mulop errors");
        createParser("+").Mulop();
        createParser("$").Mulop();
        System.out.println();
    }

    private static void testProgram() {
        System.out.println("Test: Program");
        createParser("program P { }").Program();
        createParser("program Main { void main() {;} }").Program();
        createParser("program ABC {"
                + " void f() {;}"
                + " int g(int x) { return x*2; } }").Program();
        createParser("program R "
                + " final int x = 3;"
                + " class CL { }"
                + " CL obj;"
                + " { }").Program();

        System.out.println("Test: Program errors");
        createParser("P { }").Program();
        createParser("program { }").Program();
        createParser("program C }").Program();
        createParser("program D {").Program();
        createParser("program P1 { void main() { }").Program();
        createParser("program P2 int x { void main() { } }").Program();
        System.out.println();
    }

    private static void testRelop() {
        System.out.println("Test: Relop");
        createParser("==").Relop();
        createParser("!=").Relop();
        createParser("> ").Relop();
        createParser(">=").Relop();
        createParser("< ").Relop();
        createParser("<=").Relop();

        System.out.println("Test: Relop errors");
        createParser("!").Relop();
        createParser("=!").Relop();
        createParser("+").Relop();
        createParser("--").Relop();
        createParser("*").Relop();
        System.out.println();
    }

    private static void testStatement() {
        System.out.println("Test: Statement");
        createParser(";").Statement();
        createParser("{ ; }").Statement();
        createParser("{ { { ; } } }").Statement();
        createParser("x = 2;").Statement();
        createParser("f();").Statement();
        createParser("g(2, 'c', 3 * 4);").Statement();
        createParser("if (1 == 2) y = 1; else y = 0;").Statement();
        createParser("if (3 == 3) w = 1; else ;").Statement();
        createParser("while (0 > '1') ;").Statement();
        createParser("return ;").Statement();
        createParser("return 2 * '3' + 4;").Statement();
        createParser("read(x);").Statement();
        createParser("print(2);").Statement();
        createParser("print(3, 4);").Statement();

        System.out.println("Test: Statement errors");
        createParser("f(;").Statement();
        createParser("g(2, 'c';").Statement();
        createParser("if (1 == 2) y = 1  else y = 0;").Statement();
        createParser("if (3 == 3) w = 1; else ").Statement();
        createParser("return").Statement();
        createParser("return 2 * '3' + ;").Statement();
        createParser("read(1);").Statement();
        createParser("read(x, y);").Statement();
        createParser("print(3, 4, 5);").Statement();
        System.out.println();
    }

    private static void testTerm() {
        System.out.println("Test: Term");
        createParser("2").Term();
        createParser("3 * 4").Term();
        createParser("5 * (6 + 7) / 8 % 9").Term();

        System.out.println("Test: Term errors");
        createParser("2 %").Term();
        createParser("* 4").Term();
        createParser("5 * (6 / 7 8)").Term();
        createParser("11 / (12 % 2").Term();
        System.out.println();
    }

    private static void testType() {
        System.out.println("Test: Type");
        createParser("int").Type();
        createParser("int[]").Type();
        createParser("char").Type();
        createParser("char[]").Type();
        createParser("xyz").Type();

        System.out.println("Test: Type errors");
        createParser("int[").Type();
        createParser("int[2]").Type();
        createParser("void").Type();
        createParser("345").Type();
        System.out.println();
    }

    private static void testVarDecl() {
        System.out.println("Test: VarDecl");
        createParser("int x;").VarDecl();
        createParser("int x, y;").VarDecl();
        createParser("int x, x, x;").VarDecl();

        System.out.println("Test: VarDecl errors");
        createParser("int x").VarDecl();
        createParser("int x y;").VarDecl();
        createParser("int x, ,y;").VarDecl();
        System.out.println();
    }

}
