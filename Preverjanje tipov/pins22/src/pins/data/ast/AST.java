package pins.data.ast;

import pins.common.logger.*;
import pins.common.report.*;
import pins.data.ast.visitor.*;
import pins.data.typ.*;
import pins.phase.seman.*;

/**
 * An abstract syntax tree.
 */
public abstract class AST implements Loggable {

	private static int count = 0;

	public final int id;

	public final Location location;

	public AST(Location location) {
		this.location = location;
		id = count++;
	}

	protected void logAttributes(String pfx) {
		if (this instanceof AstName) {
			AstDecl decl = SemAn.declaredAt.get((AstName) this);
			if (decl != null)
				System.out.println(pfx + "  declaredAt: " + decl.location);
		}
		if (this instanceof AstTypDecl) {
			SemName name = SemAn.declaresType.get((AstTypDecl) this);
			if (name != null) {
				System.out.println(pfx + "  declaresType:");
				name.log(pfx + "    ");
				name.type().log(pfx + "    ");
			}
		}
		if (this instanceof AstType) {
			SemType type = SemAn.describesType.get((AstType) this);
			if (type != null) {
				System.out.println(pfx + "  describesType:");
				type.log(pfx + "    ");
			}
		}
		if (this instanceof AstExpr) {
			SemType type = SemAn.exprOfType.get((AstExpr) this);
			if (type != null) {
				System.out.println(pfx + "  exprOfType:");
				type.log(pfx + "    ");
			}
		}
		if (this instanceof AstStmt) {
			SemType type = SemAn.stmtOfType.get((AstStmt) this);
			if (type != null) {
				System.out.println(pfx + "  stmtOfType:");
				type.log(pfx + "    ");
			}
		}
	}

	public abstract <Result, Arg> Result accept(AstVisitor<Result, Arg> visitor, Arg arg);

}
