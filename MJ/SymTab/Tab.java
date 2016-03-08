/* MicroJava Symbol Table  (HM 06-12-28)
   ======================
This class manages scopes and inserts and retrieves objects.
	"C:\Program Files\Java\jdk1.8.0_74\bin\javac" MJ/SymTab/Tab.java
*/
package MJ.SymTab;

import java.lang.*;
import MJ.*;

public class Tab {
	public static Scope curScope;	// current scope
	public static int   curLevel;	// nesting level of current scope

	public static Struct intType;	// predefined types
	public static Struct charType;
	public static Struct nullType;
	public static Struct noType;
	public static Obj chrObj;		// predefined objects
	public static Obj ordObj;
	public static Obj lenObj;
	public static Obj noObj;

	private static void error(String msg) {
		Parser.error(msg);
	}

	//------------------ scope management ---------------------

	public static void openScope() {
			System.out.println("***opening Scope");
		Scope s = new Scope();
		s.outer = curScope;
		curScope = s;
		curLevel++;
	}

	public static void closeScope() {
		System.out.println("***closing Scope");
		Tab.dumpScope(curScope.locals);
		curScope = curScope.outer;
		curLevel--;
	}

	//------------- Object insertion and retrieval --------------

	// Create a new object with the given kind, name and type
	// and insert it into the top scope.
	public static Obj insert(int kind, String name, Struct type) {
			System.out.println("--start of Tab insert: creating " + name);
				//--- create object node
		Obj obj = new Obj(kind, name, type);
		if (kind == Obj.Var) {
			obj.adr = curScope.nVars;
			curScope.nVars++;
			obj.level = curLevel;
			//System.out.println("---current Scope: " + curScope.outer+" "+curScope.locals+" "+curScope.nVars);
		}
		//--- append object node - creates obj p & last
		Obj p = curScope.locals, last = null;
		while (p != null) {
			//System.out.println("----checking in obj : " + p.name +"  for "+ name);
			if (p.name.equals(name))
				error(name + " declared twice");
			last = p; p = p.next;
		}

		if (last == null)
		{
				System.out.println("-----first item in obj chain");
				curScope.locals = obj;
		}  else last.next = obj;
		dumpObj(obj);
	//	System.out.println("---created obj: " + obj.kind+" "+obj.name+" "+obj.val+" "+obj.adr+" "+obj.level+" "+obj.nPars);
		System.out.println("--end of Tab insert ");
		return obj;

	}

	// Retrieve the object with the given name from the top scope
	public static Obj find(String name) {
		for (Scope s = curScope; s != null; s = s.outer)
			for (Obj p = s.locals; p != null; p = p.next)
				if (p.name.equals(name))
					return p;
		error(name + " is undeclared");
		return noObj;
	}

	// Retrieve a class field with the given name from the fields of "type"
	public static Obj findField(String name, Struct type) {
		//TODO  // fill in the code
		return noObj;
	}

	//---------------- methods for dumping the symbol table --------------

	public static void dumpStruct(Struct type) {
		String kind;
		switch (type.kind) {
			case Struct.Int:  kind = "Int  "; break;
			case Struct.Char: kind = "Char "; break;
			case Struct.Arr:  kind = "Arr  "; break;
			case Struct.Class:kind = "Class"; break;
			default: kind = "None";
		}
		System.out.print(kind+" ");
		if (type.kind == Struct.Arr) {
			System.out.print(type.nFields + " (");
			dumpStruct(type.elemType);
			System.out.print(")");
		}
		if (type.kind == Struct.Class) {
			System.out.println(type.nFields + "<<");
			for (Obj o = type.fields; o != null; o = o.next) dumpObj(o);
			System.out.print(">>");
		}
	}

	public static void dumpObj(Obj o) {
		String kind;
		switch (o.kind) {
			case Obj.Con:  kind = "Con "; break;
			case Obj.Var:  kind = "Var "; break;
			case Obj.Type: kind = "Type"; break;
			case Obj.Meth: kind = "Meth"; break;
			default: kind = "None";
		}
		System.out.print(kind+" "+o.name+" "+o.val+" "+o.adr+" "+o.level+" "+o.nPars+" (");
		dumpStruct(o.type);
		System.out.println(")");
	}

	public static void dumpScope(Obj head) {
		System.out.println("--------------");
		for (Obj o = head; o != null; o = o.next)
			dumpObj(o);
		for (Obj o = head; o != null; o = o.next)
				if (o.kind == Obj.Meth || o.kind == Obj.Prog)
					dumpScope(o.locals);
	}

	//-------------- initialization of the symbol table ------------

	public static void init() {  // build the universe
		System.out.println("start of Tab init");
		Obj o;
		curScope = new Scope();
		curScope.outer = null;
		curLevel = -1;

		// create predeclared types
		intType = new Struct(Struct.Int);
		charType = new Struct(Struct.Char);
		nullType = new Struct(Struct.Class);
		noType = new Struct(Struct.None);

		noObj = new Obj(Obj.Var, "???", noType);

		// create predeclared objects
		insert(Obj.Type, "int", intType);
		insert(Obj.Type, "char", charType);
		insert(Obj.Con, "null", nullType);

		chrObj = insert(Obj.Meth, "chr", charType);
		chrObj.locals = new Obj(Obj.Var, "i", intType);
			System.out.println("---created local chrobj: " + chrObj.locals.kind+" "+chrObj.locals.name);
		chrObj.nPars = 1;

		ordObj = insert(Obj.Meth, "ord", intType);
		ordObj.locals = new Obj(Obj.Var, "ch", charType);
			System.out.println("---created local ordobj: " + ordObj.locals.kind+" "+ordObj.locals.name);
		ordObj.nPars = 1;

		lenObj = insert(Obj.Meth, "len", intType);
		lenObj.locals = new Obj(Obj.Var, "a", new Struct(Struct.Arr, noType));
			System.out.println("---created local lenobj: " + lenObj.locals.kind+" "+lenObj.locals.name);
		lenObj.nPars = 1;
		System.out.println("end of Tab init");
	}
}
