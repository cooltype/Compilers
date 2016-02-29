/* MicroJava Scanner (HM 06-12-28)
   =================
	 "C:\Program Files\Java\jdk1.8.0_74\bin\javac" MJ/Token.java MJ/Scanner.java MJ/TestScanner.java
*/
// to be fixed - leading zeros in numbers; illegal numbers (is it too long?)
package MJ;
import java.io.*;
import java.util.Arrays;

public class Scanner { // open class
	private static final char eofCh = '\u0080';
	private static final char eol = '\n';
	private static final int  // token codes
		none      = 0,
		ident     = 1,
		number    = 2,
		charCon   = 3,
		plus      = 4,
		minus     = 5,
		times     = 6,
		slash     = 7,
		rem       = 8,
		eql       = 9,
		neq       = 10,
		lss       = 11,
		leq       = 12,
		gtr       = 13,
		geq       = 14,
		assign    = 15,
		semicolon = 16,
		comma     = 17,
		period    = 18,
		lpar      = 19,
		rpar      = 20,
		lbrack    = 21,
		rbrack    = 22,
		lbrace    = 23,
		rbrace    = 24,
		class_    = 25,
		else_     = 26,
		final_    = 27,
		if_       = 28,
		new_      = 29,
		print_    = 30,
		program_  = 31,
		read_     = 32,
		return_   = 33,
		void_     = 34,
		while_    = 35,
		eof       = 36;

	private static final String key[] = { // sorted list of keywords
		"class", "else", "final", "if", "new", "print",
		"program", "read", "return", "void", "while"
	}; //o & c

	private static final int keyVal[] = {
		class_, else_, final_, if_, new_, print_,
		program_, read_, return_, void_, while_
	}; // o & c

	private static char ch;			// lookahead character
	public  static int col;			// current column
	public  static int line;		// current line
	private static int pos;			// current position from start of source file
	private static Reader in;  	// source file reader
	private static char[] lex;	// current lexeme (token string)

	//----- ch = next input character
	private static void nextCh() { // open nextch
		try {
			ch = (char)in.read(); col++; pos++;
			if (ch == eol) {line++; col = 0;}
			else if (ch == '\uffff') ch = eofCh;
		} catch (IOException e) {
			ch = eofCh;
		}
	} // close nextch

	//--------- Initialize scanner
	public static void init(Reader r) { // open init
		in = new BufferedReader(r);
		lex = new char[64];
		line = 1; col = 0;
		nextCh();
	} //close init

	private static void readName(Token t)
	{ // open readname
		int index = 0;
		while(Character.isLetterOrDigit(ch)	||ch=='-')
		{
			lex[index] = ch;
			nextCh();
			index++;
		}
		t.string = new String(lex,0,index);

		// do a binary search here
		int keynd = Arrays.binarySearch(key, t.string);
		t.kind = keynd >= 0 ? keyVal[keynd] : ident;
		} // close readme

	private static void readNumber(Token t)
	{ // open readnumber
		int index = 0;
		char num[] = new char[64];
		while(Character.isDigit(ch) )
		{
			num[index] = ch;
			nextCh();
			index++;
		}

		try{
			t.val = Integer.parseInt(new String(num,0,index));
			t.kind = number;
		} catch (Exception e)
		{
				System.out.println("Error: number overflow " + e);
		}
	} // close readnumber

	private static void readCharCon(Token t)
	{ // open readcharcon
			int index = 0;
			t.kind = charCon;
			nextCh();
		while(Character.isLetterOrDigit(ch)	|| ch=='-' || ch=='\\')
		{
			lex[index] = ch;
			nextCh();
			index++;
		}

		if (ch=='\'')
		{
			nextCh();
			String teststr = new String(lex,0,index);
			switch(teststr)
				{
				case "x":
					t.val = 120;
					break;
				case "\\r":
					t.val = 13;
					break;
				case "\\n":
					t.val = 10;
					break;
				case "\\t":
				 t.val = 9;
					break;
				default:
					System.out.println("Error: invalid CharCon " + teststr);
			}
				//t.val = numeric char value
			nextCh();
		}
	} // close ReadCharCon

	//---------- Return next input token
	public static Token next() { // open token
		while(ch<=' ') nextCh();
		Token t = new Token();
		t.line = line;
		t.col = col;
		switch(ch)
		{ // open switch
			case '\'' :
				readCharCon(t);
				break;
			case 'a': case 'A':
			case 'b': case 'B':
			case 'c': case 'C':
			case 'd': case 'D':
		  case 'e': case 'E':
			case 'f': case 'F':
			case 'g': case 'G':
			case 'h': case 'H':
			case 'i': case 'I':
			case 'j': case 'J':
			case 'k': case 'K':
			case 'l': case 'L':
			case 'm': case 'M':
			case 'n': case 'N':
			case 'o': case 'O':
			case 'p': case 'P':
			case 'q': case 'Q':
			case 'r': case 'R':
		  case 's': case 'S':
			case 't': case 'T':
			case 'u': case 'U':
			case 'v': case 'V':
			case 'w': case 'W':
			case 'x': case 'X':
			case 'y': case 'Y':
			case 'z': case 'Z':
				readName(t);
				break;
			case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
				readNumber(t);
				break;
			case '+':
				nextCh();
				t.kind = plus;
				break;
			case '-':
				nextCh();
				t.kind = minus;
				break;
			case eofCh:
				t.kind = eof;
				break;
			case '*':
				nextCh();
				t.kind = times;
				break;
			case '/':
				nextCh();
				if(ch=='/')
				{
					do nextCh();
					while (ch != '\n' && ch != eofCh);
					t=next();
				} else t.kind = slash;
				break;
			case '%':
				nextCh();
				t.kind = rem;
				break;
			case '=':
				nextCh();
				if(ch=='=')
				{
					nextCh();
					t.kind = eql;
				} else {
					t.kind = assign;
				}
				break;
			case '!':
				nextCh();
				if(ch=='=')
				{
					nextCh();
					t.kind = neq;
				} else {
					t.kind = none;
				}
				break;
			case '<':
				nextCh();
				if(ch=='=')
				{
					nextCh();
					t.kind = leq;
				} else {
					t.kind = lss;
				}
				break;
			case '>':
				nextCh();
				if(ch=='=')
				{
					nextCh();
					t.kind = geq;
				} else {
					t.kind = gtr;
				}
				break;
			case ';':
				nextCh();
				t.kind = semicolon;
				break;
			case ',':
				nextCh();
				t.kind = comma;
				break;
			case '.':
				nextCh();
				t.kind = period;
				break;
			case '(':
				nextCh();
				t.kind = lpar;
				break;
			case ')':
				nextCh();
				t.kind = rpar;
				break;
			case '[':
				nextCh();
				t.kind = lbrack;
				break;
			case ']':
				nextCh();
				t.kind = rbrack;
				break;
			case '{':
				nextCh();
				t.kind = lbrace;
				break;
			case '}':
				nextCh();
				t.kind = rbrace;
				break;
			default:
				nextCh();
				t.kind = none;
				break;
		} // close switch
		return t;
	} // close token
}// close class
