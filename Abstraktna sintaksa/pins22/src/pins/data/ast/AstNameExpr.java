package pins.data.ast;

import pins.common.report.*;

/**
 * An expression name (a variable or a parameter).
 */
public class AstNameExpr extends AstExpr {

	public final String name;

	public AstNameExpr(Location location, String name) {
		super(location);
		this.name = name;
	}

	@Override
	public void log(String pfx) {
		System.out.println(pfx + "\033[1mAstNameExpr(" + name + ")\033[0m @(" + location + ")");
	}

}
