/* MicroJava Compiler */
package MicroJava;

import java.io.*;

public class Compiler {

    private static String objFileName(String s) {
        int i = s.lastIndexOf('.');
        if (i < 0) {
            return s + ".obj";
        } else {
            return s.substring(0, i) + ".obj";
        }
    }

    public static void main(String args[]) {
        if (args.length > 0) {
            compile(args[0], objFileName(args[0]));
        } else {
            System.out.println("use: java MicroJava.Compiler <inputfileName>");
        }
    }

    public static void compile(String source, String output) {
        try {
            Parser parser = new Parser(new InputStreamReader(new FileInputStream(source)));
            parser.parse();
            if (parser.errors == 0) {
                try {
                    System.out.println("Compilation successful (0 errors).");
                    parser.code.write(new FileOutputStream(output));
                } catch (IOException e) {
                    System.out.println("-- cannot open output file " + output);
                }
            }
        } catch (IOException e) {
            System.out.println("-- cannot open input file " + source);
        }
    }
}
