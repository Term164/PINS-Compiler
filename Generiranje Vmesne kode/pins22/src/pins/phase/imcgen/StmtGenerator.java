package pins.phase.imcgen;

import java.util.*;

import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.imc.code.expr.ImcExpr;
import pins.data.imc.code.stmt.*;
import pins.data.mem.*;

public class StmtGenerator implements AstVisitor<ImcStmt, Stack<MemFrame>> {

	static int lableCounter = 0;

	@Override
	public ImcStmt visit(ASTs<? extends AST> trees, Stack<MemFrame> memFrames) {
		Vector<ImcStmt> stmts = new Vector<>();
		for (AST s : trees.asts())
			stmts.add(s.accept(this, memFrames));
		ImcStmt stmt = new ImcSTMTS(stmts);
		return stmt;
	}

	@Override
	public ImcStmt visit(AstExprStmt exprStmt, Stack<MemFrame> memFrames) {
		ImcStmt stmt = new ImcESTMT(exprStmt.expr.accept(new ExprGenerator(), memFrames));
		ImcGen.stmtImc.put(exprStmt, stmt);
		return stmt;
	}

	@Override
	public ImcStmt visit(AstIfStmt ifStmt, Stack<MemFrame> memFrames) {
		// Set up the required lables
		MemLabel ifTrue = new MemLabel();
		MemLabel ifFalse = new MemLabel();
		MemLabel end = new MemLabel();

		ImcLABEL ifTrueLable = new ImcLABEL(ifTrue);
		ImcLABEL ifFalseLable = new ImcLABEL(ifFalse);
		ImcLABEL endLable = new ImcLABEL(end);

		ImcStmt cjump = new ImcCJUMP(
				ifStmt.condExpr.accept(new ExprGenerator(), memFrames),
				ifTrue,
				ifFalse);

		// Put the lables in the correct order
		Vector<ImcStmt> conditionalStatements = new Vector<>();
		conditionalStatements.add(cjump);
		conditionalStatements.add(ifTrueLable);
		conditionalStatements.add(ifStmt.thenBodyStmt.accept(this, memFrames));
		if (ifStmt.elseBodyStmt != null)
			conditionalStatements.add(new ImcJUMP(end));
		conditionalStatements.add(ifFalseLable);
		if (ifStmt.elseBodyStmt != null){
			conditionalStatements.add(ifStmt.elseBodyStmt.accept(this, memFrames));
			conditionalStatements.add(endLable);
		}
		return new ImcSTMTS(conditionalStatements);

	}

	@Override
	public ImcStmt visit(AstWhileStmt whileStmt, Stack<MemFrame> memFrames) {
		// Set up the required lables
		MemLabel start = new MemLabel();
		MemLabel end = new MemLabel();
		MemLabel isTrue = new MemLabel();

		ImcLABEL startLable = new ImcLABEL(start);
		ImcLABEL endLable = new ImcLABEL(end);
		ImcLABEL isTrueLable = new ImcLABEL(isTrue);

		ImcStmt cjump = new ImcCJUMP(
				whileStmt.condExpr.accept(new ExprGenerator(), memFrames),
				isTrue,
				end);

		Vector<ImcStmt> conditionalStatements = new Vector<>();

		conditionalStatements.add(startLable);
		conditionalStatements.add(cjump);
		conditionalStatements.add(isTrueLable);
		conditionalStatements.add(whileStmt.bodyStmt.accept(this, memFrames));
		conditionalStatements.add(new ImcJUMP(start));
		conditionalStatements.add(endLable);

		return new ImcSTMTS(conditionalStatements);
	}

	@Override
	public ImcStmt visit(AstAssignStmt assignStmt, Stack<MemFrame> frames) {
		ImcStmt code = new ImcMOVE(assignStmt.fstSubExpr.accept(new ExprGenerator(), frames),
				assignStmt.sndSubExpr.accept(new ExprGenerator(), frames));
		ImcGen.stmtImc.put(assignStmt, code);
		return code;
	}

}
