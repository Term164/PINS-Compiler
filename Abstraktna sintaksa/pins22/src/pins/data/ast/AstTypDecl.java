package pins.data.ast;

import pins.common.report.*;

public class AstTypDecl extends AstDecl {

	public AstTypDecl(Location location, String name, AstType type) {
		super(location, name, type);
	}

	@Override
	public void log(String pfx) {
		System.out.println(pfx + "\033[1mAstTypDecl(" + name + ")\033[0m @(" + location + ")");
		type.log(pfx + "  ");
	}

}
