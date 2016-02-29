/*  MicroJava Parser (HM 06-12-28)
    ================
		"C:\Program Files\Java\jdk1.8.0_74\bin\javac" MJ/Parser.java
*/
package MJ;

import java.util.*;
import MJ.SymTab.*;
	//import MJ.CodeGen.*;

	public class Parser {
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
		private static final String[] name = { // token names for error messages
			"none", "identifier", "number", "char constant", "+", "-", "*", "/", "%",
			"==", "!=", "<", "<=", ">", ">=", "=", ";", ",", ".", "(", ")",
			"[", "]", "{", "}", "class", "else", "final", "if", "new", "print",
			"program", "read", "return", "void", "while", "eof"
			};

		private static Token t;			// current token (recently recognized)
		private static Token la;		// lookahead token
		private static int sym;			// always contains la.kind
		public  static int errors;  // error counter
		private static int errDist;	// no. of correctly recognized tokens since last error

		private static BitSet exprStart, statStart, statSync, statSeqFollow, declStart, declFollow, relopStart;

		//------------------- auxiliary methods ----------------------
		private static void scan() {
			t = la;
			la = Scanner.next();
			sym = la.kind;
			//System.out.println(" line " + la.line + ", col " + la.col + ": " + name[sym] + " value " + la.string);
			errDist++;
			/*
			System.out.print("line " + la.line + ", col " + la.col + ": " + name[sym]);
			if (sym == ident) System.out.print(" (" + la.string + ")");
			if (sym == number || sym == charCon) System.out.print(" (" + la.val + ")");
			System.out.println();*/
		}

		private static void check(int expected) {
			if (sym == expected) scan();
			else error(name[expected] + " expected got: " + sym);
		}

		public static void error(String msg) { // syntactic error at token la
			if (errDist >= 3) {
				System.out.println("-- line " + la.line + " col " + la.col + ": " + msg);
				errors++;
			}
			errDist = 0;
		}

		//-------------- parsing methods (in alphabetical order) -----------------
		// {} = zero or more - 0,1, or many
		// [] = option- 0 or 1
		// () has to have 1?
		private static void Addop()
		{
		//Addop = "+" | "-".
			if(sym==plus||sym==minus)
			{
				scan();
			} else {
				error("Addop  ");
			}
		}

		private static void ActPairs()
		{
			//ActPars = "(" [ Expr {"," Expr} ] ")".
				//The numbers of actual and formal parameters must match.
				//The type of every actual parameter must be assignment compatible with the type of
				//every formal parameter at corresponding positions.
			check(lpar);
			while(exprStart.get(sym))
			{
				Expr();
				if(sym==comma)
				{
					scan();
				}
			}
			check(rpar);
		}

		private static void Block()
		{
		//Block = "{" {Statement} "}".
			check(lbrace);
			while(!statSeqFollow.get(sym))
			{
				Statement();
			}
			check(rbrace);
		}

		private static void ClassDecl()
		{
		//ClassDecl = "class" ident "{" {VarDecl} "}".
		//  A DECLARATION - ERROR CHECK AND SEMANTIC PROC
			check(class_);
			check(ident);
			check(lbrace);
			//may or may not be there
			while(sym==ident)
			{
				VarDecl();
			}
		  	check(rbrace);
		}

		private static void Condition()
		{
		//Condition = Expr Relop Expr.
		// E.G.   A<B - WILL HAVE TO BE EVALUATED
			Expr();
			Relop();
			Expr();
		}

		private static void ConstDecl()
		{
		//ConstDecl = "final" Type ident "=" (number | charConst) ";".
		//  A DECLARATION - ERROR CHECK AND SEMANTIC PROC
			check(final_);
			Type();
			check(ident);
			check(assign);
			if(sym==number ||sym==charCon)
			{
				scan();
			}  else {
					error("ConstDecl  ");
			}
			check(semicolon);
		}

		private static void Designator()
		{
		//Designator = ident {"." ident | "[" Expr "]"}.
			check(ident);
			while(sym==period || sym==lbrack)
			{
				if(sym==period)
				{
					scan();
					check(ident);
				} else {
					scan();
					Expr();
					check(rbrack);
				}
			}
		}

		private static void Expr()
		{
		//Expr = ["-"] Term {Addop Term}.
		//  A DECLARATION - ERROR CHECK AND SEMANTIC PROC
			if(sym==minus)
			{
				scan();
			}
			Term();
			while(sym==plus||sym==minus)
			{
				scan();
				Term();
			}
		}

		private static void Factor()
		{
		// exprstart has all the starting functions
			//Factor = Designator [ActPars] | number | charConst | "new" ident ["[" Expr "]"] | "(" Expr ")".
			// PART OF A DECLARATION?
			if(sym == ident)
			{
				Designator();
				if(sym==lpar)
				{
					ActPairs();
				}
			}
			else if (sym == number) {
				scan();
			}
			else if (sym == charCon) {
				scan();
			}
			else if (sym == new_) {
				scan();
				check(ident);
				if (sym == lbrack) {
					scan();
					Expr();
					check(rbrack);
				}
			}
			else if (sym == lpar) {
				scan();
				Expr();
				check(rpar);
			}
			else
			{
				error("invalid Factor  ");
			}
		}

		private static void FormPars()
		{
		//FormPars = Type ident {"," Type ident}.
		// PART OF A DECLARATION?
			Type();
			check(ident);
			while(sym==comma)
			{
				check(comma);
				Type();
				check(ident);
			}
		}

		private static void MethodDecl()
		{
		//MethodDecl = (Type | "void") ident "(" [FormPars] ")" {VarDecl} Block.
		//  A DECLARATION - ERROR CHECK AND SEMANTIC PROC
			if(sym==number||sym==charCon)
			{
				Type();
			} else if (sym==void_)
			{
				check(void_);
			}
			check(ident);
			check(lpar);

			if(sym==number||sym==charCon)
			{
				FormPars();
			}
			check(rpar);
			while(sym==ident)
			{
				VarDecl();
			}
			Block();
		}

		private static void Mulop()
		{
		//Mulop = "*" | "/" | "%".
			if(sym==times||sym==slash ||sym==rem)
			{
				scan();
			} else {
				error("Mulop  ");
			}
		}

		private static void Relop()
		{
		//Relop = "==" | "!=" | ">" | ">=" | "<" | "<=".
			//use the dcit thingy
			if(relopStart.get(sym))
			{
				scan();
			} else {
				error("Relop  ");
			}
		}

		private static void Statement()
		{
//		Statement =
			//	ident -> Designator ("=" Expr | ActPars) ";" |
			//	"if" "(" Condition ")" Statement ["else" Statement] |
			//	"while" "(" Condition ")" Statement |
			//  "return" [Expr] ";" |
			//	"read" "(" Designator ")" ";" |
			//	"print" "(" Expr ["," number] ")" ";" |
			//	 { -> Block |
			//	";".
			// WILL NEED TO BE EVALUATED
			if(!statStart.get(sym)){
			//statStart = s.set(ident); s.set(if_); s.set(while_); s.set(read_);	s.set(return_); s.set(print_); s.set(lbrace); s.set(semicolon);
				error("invalid start of statment");
		 	//	while(!statSync.get(sym)) scan();
			do scan(); while(statSync.get(sym) || sym == rbrace);
			if(sym==semicolon) scan();
				errDist = 0;
			}
			switch(sym){
				case ident:
					//	ident -> Designator ("=" Expr | ActPars) ";" |
					Designator();
					if(sym==assign)
					{
						scan();
						Expr();
					} else {
						ActPairs();
					}
					check(semicolon);
					break;
				case if_:
					//	"if" "(" Condition ")" Statement ["else" Statement] |
					check(if_);
					check(lpar);
					Condition();
					check(rpar);
					Statement();
					if (sym == else_) {
						scan();
						Statement();
					}
					break;
				case while_:
					// "while" "(" Condition ")" Statement |
					check(while_);
					check(lpar);
					Condition();
					check(rpar);
					Statement();
					break;
				case return_:
					//  "return" [Expr] ";" |
					check(return_);
					if(exprStart.get(sym))
					{
						Expr();
					}
					check(semicolon);
					break;
				case read_:
				  //	"read" "(" Designator ")" ";" |
					check(read_);
					check(lpar);
					Designator();
					check(rpar);
					check(semicolon);
					break;
				case print_:
					//	"print" "(" Expr ["," number] ")" ";" |
					check(print_);
					check(lpar);
					Expr();
					if(sym ==comma)
					{
						scan();
						check(number);
					}
					check(rpar);
					check(semicolon);
					break;
				case lbrace:
						//	 { -> Block |
						Block();
					break;
				case semicolon:
					scan();
					break;
				default:
					error("invalid start of a Statement  ");
			}
		}

		private static void Term()
		{
		//Term = Factor {Mulop Factor}.
		//	 exprstart has all the starting functions in Factor
			Factor();
			while(sym==times||sym==slash ||sym==rem)
			{
				Mulop();
				Factor();
			}
		}

		private static void Type()
		{
		//Type = ident ["[" "]"].  -ident must denote a type.

			check(ident);
				if(sym==lbrack)
				{
					scan();
					check(rbrack);
				}
		}

		private static void VarDecl()
		{
		//VarDecl = Type ident {"," ident } ";".
		//  A DECLARATION - ERROR CHECK AND SEMANTIC PROC
			Type();
			check(ident);
			while(sym==comma )
			{
				scan();
				check(ident);
			}
			check(semicolon);
		}

		private static void Program() {
		// Program = "program" ident {ConstDecl | ClassDecl | VarDecl} '{' {MethodDecl} '}'.
			check(program_);
			// might have 0, 1 or more {ConstDecl | ClassDecl | VarDecl}
			check(ident);
			while(declStart.get(sym))
			{ // start while
				// ConstDecl | ClassDecl | VarDecl
				if(sym==final_ )
				{ // if 1
					ConstDecl();
				} else if (sym==class_)
					{
						ClassDecl();
					} else {
						VarDecl();
					}

			} // end while
			check(lbrace);
			MethodDecl();
			check(rbrace);
		}

		public static void parse() {
			// initialize symbol sets
			BitSet s;
			s = new BitSet(64); exprStart = s;
			s.set(ident); s.set(number); s.set(charCon); s.set(new_); s.set(lpar); s.set(minus);

			s = new BitSet(64); statStart = s;
			s.set(ident); s.set(if_); s.set(while_); s.set(read_);
			s.set(return_); s.set(print_); s.set(lbrace); s.set(semicolon);

			s = new BitSet(64); statSync = s;
			s.set(eof); s.set(if_); s.set(while_); s.set(read_);
			s.set(return_); s.set(print_); s.set(lbrace); s.set(semicolon);

			s = new BitSet(64); statSeqFollow = s;
			s.set(rbrace); s.set(eof);

			s = new BitSet(64); declStart = s;
			s.set(final_); s.set(ident); s.set(class_);

			s = new BitSet(64); declFollow = s;
			s.set(lbrace); s.set(void_); s.set(eof);

			s = new BitSet(64); relopStart = s;
			s.set(neq); s.set(number); s.set(gtr); s.set(geq); s.set(lss); s.set(leq);
			//"==" | "!=" | ">" | ">=" | "<" | "<=".

			// start parsing
			errors = 0; errDist = 3;
			scan();
			Program();
			if (sym != eof) error("end of file found before end of program");
		}
}
