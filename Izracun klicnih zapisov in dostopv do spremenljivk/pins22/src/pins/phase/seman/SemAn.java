package pins.phase.seman;

import java.util.*;
import pins.data.ast.*;
import pins.data.typ.SemName;
import pins.data.typ.SemType;

public class SemAn implements AutoCloseable {

	/** Maps names to declarations. */
	public static final HashMap<AstName, AstDecl> declaredAt = new HashMap<AstName, AstDecl>();

	// Loop over all type declarations
	public static final HashMap<AstTypDecl, SemName> declaresType = new HashMap<AstTypDecl, SemName>();
	// Loop over all variable declarations
	public static final HashMap<AstType, SemType> describesType = new HashMap<AstType, SemType>();
	// Set all expr types
	public static final HashMap<AstExpr, SemType> exprOfType = new HashMap<AstExpr, SemType>();
	//
	public static final HashMap<AstStmt, SemType> stmtOfType = new HashMap<AstStmt, SemType>();

	public SemAn() {		
	}
	
	public void close() {
	}

}
