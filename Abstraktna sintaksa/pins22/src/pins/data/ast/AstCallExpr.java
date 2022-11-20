package pins.data.ast;

import pins.common.report.*;

/**
 * A call expression.
 */
public class AstCallExpr extends AstNameExpr {
	
	public final ASTs<AstExpr> args;

	public AstCallExpr(Location location, String name, ASTs<AstExpr> args) {
		super(location, name);
		this.args = args;
	}

	@Override
	public void log(String pfx) {
		System.out.println(pfx + "\033[1mAstCallExpr(" + name + ")\033[0m @(" + location + ")");
		System.out.println(pfx + "  {Args}");
		args.log(pfx + "  ");
	}

}
