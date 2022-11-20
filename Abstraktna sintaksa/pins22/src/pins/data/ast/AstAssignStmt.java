package pins.data.ast;

import pins.common.report.*;

/**
 * An assignment statement.
 */
public class AstAssignStmt extends AstStmt {
	
	public final AstExpr fstSubExpr;
	
	public final AstExpr sndSubExpr;
	
	public AstAssignStmt(Location location, AstExpr fstSubExpr, AstExpr sndSubExpr) {
		super(location);
		this.fstSubExpr = fstSubExpr;
		this.sndSubExpr = sndSubExpr;
	}

	@Override
	public void log(String pfx) {
		System.out.println(pfx + "\033[1mAstAssignStmt\033[0m @(" + location + ")");
		fstSubExpr.log(pfx + "  ");
		sndSubExpr.log(pfx + "  ");
	}

}
