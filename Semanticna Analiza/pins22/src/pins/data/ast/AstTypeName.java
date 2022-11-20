package pins.data.ast;

import pins.common.report.*;
import pins.data.ast.visitor.AstVisitor;
import pins.phase.seman.SemAn;

/**
 * A type name.
 */
public class AstTypeName extends AstType implements AstName {

	public final String name;

	public AstTypeName(Location location, String name) {
		super(location);
		this.name = name;
	}

	@Override
	public void log(String pfx) {
		System.out.println(pfx + "\033[1mAstTypeName(" + name + ")\033[0m @(" + location + ")");
		{
			AstDecl decl = SemAn.declaredAt.get(this);
			if (decl != null)
				System.out.println(pfx + "  declaredAt: " + decl.location);
		}
	}

	@Override
	public <Result, Arg> Result accept(AstVisitor<Result, Arg> visitor, Arg arg) {
		return visitor.visit(this, arg);
	}

}
