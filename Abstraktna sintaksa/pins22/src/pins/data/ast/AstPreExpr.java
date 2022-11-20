package pins.data.ast;

import pins.common.report.*;

/**
 * A prefix expression.
 */
public class AstPreExpr extends AstExpr {

	public enum Oper {
		NEW, DEL, NOT, ADD, SUB, PTR
	};
	
	public final AstPreExpr.Oper oper;
	
	public final AstExpr subExpr;
	
	public AstPreExpr(Location location, AstPreExpr.Oper oper, AstExpr subExpr) {
		super(location);
		this.oper = oper;
		this.subExpr = subExpr;
	}

	@Override
	public void log(String pfx) {
		System.out.println(pfx + "\033[1mAstPreExpr(" + oper + ")\033[0m @(" + location + ")");
		subExpr.log(pfx + "  ");
	}

}
