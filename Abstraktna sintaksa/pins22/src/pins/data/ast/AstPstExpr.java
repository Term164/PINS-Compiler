package pins.data.ast;

import pins.common.report.*;

/**
 * A prefix expression.
 */
public class AstPstExpr extends AstExpr {

	public enum Oper {
		PTR
	};
	
	public final AstPstExpr.Oper oper;
	
	public final AstExpr subExpr;
	
	public AstPstExpr(Location location, AstPstExpr.Oper oper, AstExpr subExpr) {
		super(location);
		this.oper = oper;
		this.subExpr = subExpr;
	}

	@Override
	public void log(String pfx) {
		System.out.println(pfx + "\033[1mAstPstExpr(" + oper + ")\033[0m @(" + location + ")");
		subExpr.log(pfx + "  ");
	}

}
