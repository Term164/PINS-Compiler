package pins.phase.seman;

import java.util.*;
import pins.data.ast.*;

public class SemAn implements AutoCloseable {

	/** Maps names to declarations. */
	public static final HashMap<AstName, AstDecl> declaredAt = new HashMap<AstName, AstDecl>();

	public SemAn() {		
	}
	
	public void close() {
	}

}
