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
			else error(name[expected] + " expected, got: " + sym);
		}

		public static void error(String msg) { // syntactic error at token la
			if (errDist >= 3) {
				System.out.println("** line " + la.line + " col " + la.col + ": " + msg);
				errors++;
			}
			errDist = 0;
		}

		//-------------- parsing methods (in alphabetical order) -----------------
		// {} = zero or more - 0,1, or many
		// [] = option- 0 or 1
		// () has to have 1?
		private static int Addop()
		{
			int OperatorID = 0;
			//Addop = "+" | "-".
		/*
		plus      = 4,
		minus     = 5,
		*/
			if(sym==plus||sym==minus)
			{
				OperatorID = sym;
				scan();
			} else {
				error("Addop  ");
			}
			return OperatorID;
		}

		private static int ActPairs()
		{
			int result = 0;
			// called by statement or factor, calls expr
			//ActPars = "(" [ Expr {"," Expr} ] ")".
				//The numbers of actual and formal parameters must match.
				//The type of every actual parameter must be assignment compatible with the type of
				//every formal parameter at corresponding positions.
			//	System.out.println("start ActPairs");
			check(lpar);
			while(exprStart.get(sym))
			{ // s.set(ident); s.set(number); s.set(charCon); s.set(new_); s.set(lpar); s.set(minus);
				result += Expr(); // returns 0 if not a number (what abut charcon?)
				// parameter checked in designator (eventually!)
			//	Obj TestObj = Tab.find(t.string);
				// how do you know what there should be to match actual with formal
				if(sym==comma)
				{
					scan();
				}
			}
			check(rpar);
			//	System.out.println("end ActPairs");
			return result;
		}

		private static void Block()
		{
			// called by statement, calls statement
			//	System.out.println("start block");
		//Block = "{" {Statement} "}".
			check(lbrace);
			while(!statSeqFollow.get(sym))
			{
				Statement();
			}
			check(rbrace);
			//	System.out.println("end block");
		}

		private static void ClassDecl()
		{
			//	System.out.println("start classdecl");
		//ClassDecl = "class" ident "{" {VarDecl} "}".
		  String Classname = "";
			check(class_);
			//System.out.println("**openscope from class");
			Struct ClassStruct = new Struct(Struct.Class);
			check(ident);
			//System.out.println("**name used in obj insert: " + t.string);
		//	Obj curClass = Tab.noObj;
			Obj TestObj = Tab.find(t.string);
			if(TestObj!=Tab.noObj)
			{// its a duplicate, error
						error("classDecl duplicate class name " + t.string);
			} else { // add it to symbol table, and make a local copy
					Classname = t.string;
			}
			Tab.openScope();
			check(lbrace);
			while(sym==ident)
			{  // ident is caught in vardecl
				VarDecl();
			}
			// you now have a bunch of variables attached to new scope in the usual way
			ClassStruct.nFields = Tab.curScope.nVars;
			ClassStruct.fields = Tab.curScope.locals;

		  	check(rbrace);
			//	System.out.println("**closescope from class");
				//curClass.locals = Tab.curScope.locals;
				Tab.closeScope();
				Tab.insert(Obj.Type,Classname,ClassStruct);
			//		System.out.println("end classdecl");
		}

		private static boolean Condition()
		{
			boolean result = false;
			int sum1 = 0, sum2 = 0;
			int OperatorID;
			//	System.out.println("start condition");
		//Condition = Expr Relop Expr.
		/*
			eql       = 9,
			neq       = 10,
			lss       = 11,
			leq       = 12,
			gtr       = 13,
			geq       = 14,
		*/
		// E.G.   A<B - WILL HAVE TO BE EVALUATED
		// where will the value be, as expr doent retun anything? have to return boolean eventually?
			sum1 = Expr();
			OperatorID = Relop();
			sum2 = Expr();
			switch(OperatorID)
			{
				case eql:
					if(sum1==sum2)
					{
						result = true;
					}
					break;
				case neq:
					if(sum1==sum2)
					{
						result = true;
					}
					break;
				case lss:
					if(sum1<sum2)
					{
						result = true;
					}
					break;
				case leq:
					if(sum1<=sum2)
					{
						result = true;
					}
					break;
				case gtr:
					if(sum1>sum2)
					{
						result = true;
					}
					break;
				case geq:
					if(sum1>=sum2)
					{
						result = true;
					}
					break;
				default:

			}
			//	System.out.println("end condition");
			return result;
		}

		private static void ConstDecl()
		{
			//	System.out.println("start constDecl");
		//ConstDecl = "final" Type ident "=" (number | charConst) ";".
		//  A DECLARATION - ERROR CHECK AND SEMANTIC PROC
			check(final_);
			Struct IdType = Type();
			Obj LocalObj = Tab.noObj;
			// get 3rd decl type from here?
			check(ident);
				// ident needs to go in the symbol table - change type later
				//	System.out.println("**name used in obj insert: " + t.string);
				Obj TestObj = Tab.find(t.string);
				if(TestObj!=Tab.noObj)
				{// its a duplicate, error
							error("ConstDecl duplicate Const: "  + t.string);
				} else { // add it to symbol table
						LocalObj = Tab.insert(Obj.Con,t.string,IdType);
				}
			// set constant value
			check(assign);
			if(sym==number ||sym==charCon)
			{
				LocalObj.val = t.val;
				scan();
			}  else {
					error("ConstDecl  ");
			}
			check(semicolon);
			//	System.out.println("end constdecl");
		}

		private static void Designator()
		{
			int result = 0;
			//	System.out.println("start designator");
		//Designator = ident {"." ident | "[" Expr "]"}.
			check(ident);
				// ident needs to go in the symbol table
				Obj TestObj = Tab.find(t.string);
				if(TestObj==Tab.noObj)
				{// its a duplicate, error
							error("Designator ident not found: " + t.string);
				} // its on the right hand side and should already be declared

			while(sym==period || sym==lbrack)
			{
				if(sym==period)
				{
					scan();
					check(ident);
					// check its a valid field of Obj found above
					if(Tab.findField(t.string,TestObj.type))
					{
							// its on the right hand side and should already be declared
					} else {
							error("Designator ident.field not found: " + TestObj.name + " " + t.string);
					}
				} else {
					scan();
					// should a number here dimension an array?
					result = Expr();
					check(rbrack);
				}
			}
			//	System.out.println("end designator");
		}

		private static int Expr()
		{
			//	System.out.println("start expr");
		//Expr = ["-"] Term {Addop Term}.
		//  A DECLARATION - ERROR CHECK AND SEMANTIC PROC
		// need to be checked with sym table (these are the parameters going into funcs etc)
			int result = 0;
			if(sym==minus)
			{ // DEAL WITH THE NEGATIVES
				scan();
				result = (Term() * (-1)); // make negative
			} else {
				result = Term();
			}

			while(sym==plus||sym==minus)
			{
				// the maths must be processed here
				if(sym==plus)
				{
					scan();
					result += Term();
				} else {
					scan();
				  result -= Term();
				}
			}
			//	System.out.println("end expr");
			return result;
		}

		private static int Factor()
		{
			//	System.out.println("start factor");
		// exprstart has all the starting functions
			//Factor = Designator [ActPars] | number | charConst | "new" ident ["[" Expr "]"] | "(" Expr ")".
			// PART OF A DECLARATION?
      int result = 0;
			if(sym == ident)
			{
				Designator();
				if(sym==lpar)
				{
					ActPairs();
				}
				// do you match up parameters with those expected here?
			}
			else if (sym == number) {
				// enter into variable?
				result = t.val;
				scan();
			}
			else if (sym == charCon) {
					// enter into variable?
				result = t.val;
				scan();
			}
			else if (sym == new_) {
				scan();
				check(ident);
				Obj TestObj = Tab.find(t.string);
				if(TestObj==Tab.noObj)
				{// its a duplicate, error
							error("factor ident not found: " + t.string);
				} // its on the right hand side and should already be declared
				if (sym == lbrack) {
					scan();
					result = Expr();
					check(rbrack);
				}
			}
			else if (sym == lpar) {
				scan();
				result = Expr();
				check(rpar);
			}
			else
			{
				error("invalid Factor  ");
			}
			//	System.out.println("end factor");
			return result;
		}

		private static void FormPars()
		{
			//	System.out.println("start formpars");
		//FormPars = Type ident {"," Type ident}.
		// PART OF A DECLARATION?
			Struct IdType = Type();
			check(ident);
			Obj TestObj = Tab.find(t.string);
			if(TestObj!=Tab.noObj)
			{// its a duplicate, error
						error("formpars duplicate var name  ");
			} else { // add it to symbol table
						Tab.insert(Obj.Var,t.string,IdType);
			}
				// ident needs to go in the symbol table
			while(sym==comma)
			{
				check(comma);
				IdType = Type();
				check(ident);
				TestObj = Tab.find(t.string);
				if(TestObj!=Tab.noObj)
				{// its a duplicate, error
							error("formpars duplicate var name  ");
				} else { // add it to symbol table
							Tab.insert(Obj.Var,t.string,IdType);
				}
			}
			//	System.out.println("end formpars");
		}

		private static void MethodDecl()
		{
			//	System.out.println("start methoddecl");
		//MethodDecl = (Type | "void") ident "(" [FormPars] ")" {VarDecl} Block.
		//  A DECLARATION - ERROR CHECK AND SEMANTIC PROC
			Struct IdType = Tab.noType;
			if(sym==number||sym==charCon)
			{
				IdType = Type();
			} else if (sym==void_)
			{
				check(void_);
				IdType = Tab.nullType;
			}
		  check(ident);
		//	System.out.println("**name used in obj insert: " + t.string);
		Obj curMethod = Tab.noObj;
		Obj TestObj = Tab.find(t.string);
		if(TestObj!=Tab.noObj)
		{// its a duplicate, error
					error("classDecl duplicate class name  ");
		} else { // add it to symbol table
					curMethod = Tab.insert(Obj.Meth,t.string,IdType);
		}

		//	System.out.println("**openscope from method");
			Tab.openScope();
				// ident needs to go in the symbol table
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
			// last node of previous scope obj string is this method
			curMethod.locals = Tab.curScope.locals;
		//	System.out.println("**closescope from method");
			Tab.closeScope();
		//	System.out.println("end methoddecl");
		}

		private static int Mulop()
		{
			int OperatorID = 0;
			//	System.out.println("start mulop");
		//Mulop = "*" | "/" | "%".
		// is any processing done here?
			/*
			times     = 6,
			slash     = 7,
			rem       = 8,
			*/
			if(sym==times||sym==slash ||sym==rem)
			{
				OperatorID = sym;
				scan();
			} else {
				error("Mulop  ");
			}
			//	System.out.println("end mulop");
			return OperatorID;
		}

		private static int Relop()
		{
			int OperatorID = 0;
			//	System.out.println("start relop");
		//Relop = "==" | "!=" | ">" | ">=" | "<" | "<=".
			//use the dcit thingy
				// is any processing done here?
				/*
				eql       = 9,
				neq       = 10,
				lss       = 11,
				leq       = 12,
				gtr       = 13,
				geq       = 14,
				*/
			if(relopStart.get(sym))
			{
				OperatorID = sym;
				scan();
			} else {
				error("Relop  ");
			}
			//	System.out.println("end relop");
			return OperatorID;
		}

		private static void Statement()
		{
			//	System.out.println("start statement");
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
					// ident checked in designator
					//	ident -> Designator ("=" Expr | ActPars) ";" |
					// does this need t be assessed?
					Designator();
					if(sym==assign)
					{
						scan();
						Expr();
					} else {
						// the erros come here
						if(sym!=lpar){
						//statStart = s.set(ident); s.set(if_); s.set(while_); s.set(read_);	s.set(return_); s.set(print_); s.set(lbrace); s.set(semicolon);
							error("invalid Assignment");
						//	while(!statSync.get(sym)) scan();
							do scan(); while(sym!=semicolon);
						} else {
							ActPairs();
						}
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
			//	System.out.println("end statement");
		}

		private static int Term()
		{
			//	System.out.println("start term");
		//Term = Factor {Mulop Factor}.
		//	 exprstart has all the starting functions in Factor
		// thses need to be fed into actual variables and processed
		/*
			times     = 6,
			slash     = 7,
			rem       = 8,
		*/
			int result=0;
			int sum1, sum2, OperatorID;
			sum1 = Factor();
			while(sym==times||sym==slash ||sym==rem)
			{
				OperatorID = Mulop();
				sum2 = Factor();
				switch(OperatorID)
				{
					case times:
						result = sum1*sum2;
						break;
					case slash:
						result = sum1/sum2;
						break;
					case rem:
						result = sum1%sum2;
						break;
					default:
				}
			}
			//	System.out.println("end term");
			return result;
		}

		private static Struct Type()
		{
		//Type = ident ["[" "]"].  -ident must denote a type.
	   //	System.out.println("start type");
		 // int=intType, char=charType, class=nullType, int[], char[], class[]
		 /* to paste everyewhere type is
		 		Struct IdType;
		 */
		 		Struct IdType;
				check(ident);
			// possible types are: int, char, arr - class gets filtered out in the scanner
			// ident needs to go in the symbol table
				Obj TestObj = Tab.find(t.string);
			//	String ArrStructName = t.string + "Arr";
			// what if its a class?
			if(TestObj==Tab.noObj)
				{ // for null objects
						IdType = Tab.noType;
				} else {
						// for evertyhing else, including classes
						IdType = TestObj.type;
				}

				if(sym==lbrack)
				{
					// if it has a bracket, then the struct type needs to be arr, linked to struct above
					scan();
					check(rbrack);
					// need to indicate that the type is an array
					Struct ArrStruct = new Struct(Struct.Arr);
					ArrStruct.elemType = IdType;
					IdType = ArrStruct;
				}
				return IdType;
				//	System.out.println("end type");
		}

		private static void VarDecl()
		{
			//	System.out.println("start vardecl");
		//VarDecl = Type ident {"," ident } ";".
		//  A DECLARATION - ERROR CHECK AND SEMANTIC PROC
			Struct IdType = Type();
			check(ident);
			Obj TestObj = Tab.find(t.string);

			if(TestObj!=Tab.noObj)
			{// its a duplicate, error
						error("classDecl duplicate class name  ");
			} else { // add it to symbol table
					Tab.insert(Obj.Var,t.string,IdType);
			}
			//	System.out.println("**name used in obj insert: " + t.string);
			// ident needs to go in the symbol table

			while(sym==comma )
			{
				scan();
				check(ident);
				//	System.out.println("**name used in obj insert: " + t.string);
				TestObj = Tab.find(t.string);
				if(TestObj!=Tab.noObj)
				{// its a duplicate, error
							error("classDecl duplicate class name  ");
				} else { // add it to symbol table
							Tab.insert(Obj.Var,t.string,IdType);
				}
			}
				// ident needs to go in the symbol table			}
				check(semicolon);
				//	System.out.println("end vardecl");

		}

		private static void Program() {
			//	System.out.println("start program");
		// Program = "program" ident {ConstDecl | ClassDecl | VarDecl} '{' {MethodDecl} '}'.
		//System.out.println("**openscope from program");
			Tab.openScope();
			check(program_);
			// might have 0, 1 or more {ConstDecl | ClassDecl | VarDecl}
			check(ident);
			//	System.out.println("**name used in obj insert: " + t.string);
			Obj TestObj = Tab.find(t.string);
			if(TestObj!=Tab.noObj)
			{// its a duplicate, error
						error("classDecl duplicate class name  ");
			} else { // add it to symbol table
						Tab.insert(Obj.Prog,t.string,Tab.nullType);
			}

			// the name of the program - does this go in the table?
			while(declStart.get(sym))
			{ // start while
				// ConstDecl | ClassDecl | VarDecl
				if(sym==final_ )
				{ // if 1
				//	System.out.println("**calling constdecl");
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
		//	System.out.println("**closescope from program");
			Tab.closeScope();
			//	System.out.println("end program");
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
			Tab.init();
			scan();
			Program();
			Tab.closeScope();
			if (sym != eof) error("end of file found before end of program");
		}
}
