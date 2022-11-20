package pins.data.ast;

import pins.common.report.*;

/**
 * A loop statement.
 */
public class AstWhileStmt extends AstStmt {
	
	public final AstExpr condExpr;
	
	public final AstStmt bodyStmt;
	
	public AstWhileStmt(Location location, AstExpr condExpr, AstStmt bodyStmt) {
		super(location);
		this.condExpr = condExpr;
		this.bodyStmt = bodyStmt;
	}

	@Override
	public void log(String pfx) {
		System.out.println(pfx + "\033[1mAstWhileStmt\033[0m @(" + location + ")");
		condExpr.log(pfx + "  ");
		bodyStmt.log(pfx + "  ");
	}

}
