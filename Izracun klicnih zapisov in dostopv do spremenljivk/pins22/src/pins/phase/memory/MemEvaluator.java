package pins.phase.memory;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.mem.*;
import pins.data.typ.*;
import pins.phase.seman.*;

/**
 * Computing memory layout: frames and accesses.
 */
public class MemEvaluator extends AstFullVisitor<Object, MemEvaluator.FunContext> {

	@Override
	public Object visit(AstFunDecl funDecl, FunContext funContext) {
		MemLabel label;
		FunContext newFunContext = new FunContext();

		if (funContext == null){
			funContext = newFunContext;
			label = new MemLabel(funDecl.name);
		}else {
			newFunContext.depth = funContext.depth;
			funContext = newFunContext;
			label = new MemLabel();
		}

		funContext.depth++;
		super.visit(funDecl, funContext);
		funContext.depth--;
		Memory.frames.put(funDecl,
				new MemFrame(
						label,
						funContext.depth,
						funContext.locsSize,
						funContext.argsSize
				));
		return null;
	}

	@Override
	public Object visit(AstVarDecl varDecl, FunContext funContext) {
		long size = SemAn.describesType.get(varDecl.type).actualType().size();
		if (funContext == null){
			Memory.varAccesses.put(varDecl, new MemAbsAccess(size, new MemLabel(varDecl.name)));
		}else {
			Memory.varAccesses.put(varDecl, new MemRelAccess(size, -funContext.locsSize-size, funContext.depth));
			funContext.locsSize += size;
		}
		return super.visit(varDecl, funContext);
	}

	@Override
	public Object visit(AstCallExpr callExpr, FunContext funContext) {
		funContext.argsSize = Math.max(funContext.argsSize, callExpr.args.asts().size()* 8L + 8);
		return super.visit(callExpr, funContext);
	}

	@Override
	public Object visit(AstParDecl parDecl, FunContext funContext) {
		long size = SemAn.describesType.get(parDecl.type).actualType().size();
		Memory.parAccesses.put(parDecl, new MemRelAccess(size, funContext.parsSize, funContext.depth));
		funContext.parsSize += size;
		return super.visit(parDecl, funContext);
	}

	/**
	 * Functional context, i.e., used when traversing function and building a new
	 * frame, parameter acceses and variable acceses.
	 */
	protected class FunContext {
		public int depth = 1;
		public long locsSize = 0;
		public long argsSize = 0;
		public long parsSize = new SemPtr(new SemVoid()).size();
	}

}
