package pins.data.ast;

import pins.common.report.*;

/**
 * An expression containing a list of statements.
 */
public class AstStmtExpr extends AstExpr {

	public final ASTs<AstStmt> stmts;

	public AstStmtExpr(Location location, ASTs<AstStmt> stmts) {
		super(location);
		this.stmts = stmts;
	}

	@Override
	public void log(String pfx) {
		System.out.println(pfx + "\033[1mAstStmtExpr\033[0m @(" + location + ")");
		System.out.println(pfx + "  {Stmts}");
		stmts.log(pfx + "    ");
	}

}
