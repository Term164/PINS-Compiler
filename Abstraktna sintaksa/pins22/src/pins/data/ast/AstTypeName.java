package pins.data.ast;

import pins.common.report.*;

/**
 * A type name.
 */
public class AstTypeName extends AstType {

	public final String name;

	public AstTypeName(Location location, String name) {
		super(location);
		this.name = name;
	}

	@Override
	public void log(String pfx) {
		System.out.println(pfx + "\033[1mAstTypeName(" + name + ")\033[0m @(" + location + ")");
	}

}
