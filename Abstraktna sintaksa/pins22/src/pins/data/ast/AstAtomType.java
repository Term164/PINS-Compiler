package pins.data.ast;

import pins.common.report.*;

/**
 * An atom type.
 */
public class AstAtomType extends AstType {

	public enum Kind {
		VOID, CHAR, INT
	};

	public final Kind kind;

	public AstAtomType(Location location, AstAtomType.Kind kind) {
		super(location);
		this.kind = kind;
	}

	@Override
	public void log(String pfx) {
		System.out.println(pfx + "\033[1mAstAtomType(" + kind + ")\033[0m @(" + location + ")");
	}

}
