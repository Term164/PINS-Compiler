package pins.phase.imcgen;

import java.util.*;

import pins.common.report.Report;
import pins.data.ast.*;
import pins.data.ast.visitor.*;
import pins.data.imc.code.expr.*;
//import pins.data.imc.code.stmt.*;
import pins.data.imc.code.stmt.ImcSTMTS;
import pins.data.imc.code.stmt.ImcStmt;
import pins.data.mem.*;
import pins.data.typ.SemArr;
import pins.phase.memory.Memory;
import pins.phase.seman.SemAn;
//import pins.data.typ.*;
//import pins.phase.memory.*;
//import pins.phase.seman.*;

public class ExprGenerator implements AstVisitor<ImcExpr, Stack<MemFrame>> {

	@Override
	public ImcExpr visit(AstWhereExpr whereExpr, Stack<MemFrame> frames) {
		whereExpr.decls.accept(new CodeGenerator(), frames);
		ImcExpr code = whereExpr.subExpr.accept(this, frames);
		ImcGen.exprImc.put(whereExpr, code);
		return code;
	}

	@Override
	public ImcExpr visit(AstStmtExpr stmtExpr, Stack<MemFrame> memFrames) {

		ImcExpr expr;
		if (stmtExpr.stmts.asts().lastElement() instanceof AstExprStmt){
			// Remove the last expression statement and evaluate its expression;
			ImcExpr lastExpression = ((AstExprStmt) stmtExpr.stmts.asts().lastElement()).expr.accept(this, memFrames);
			ImcStmt evaluatedSTMTS = stmtExpr.stmts.accept(new StmtGenerator(), memFrames);
			((ImcSTMTS) evaluatedSTMTS).stmts.remove(((ImcSTMTS) evaluatedSTMTS).stmts.lastElement()); // Remove last statement

			expr = new ImcSEXPR(evaluatedSTMTS,lastExpression);
		}else {
			expr = new ImcSEXPR(
					stmtExpr.stmts.accept(new StmtGenerator(), memFrames),
					new ImcCONST(0)
			);
		}

		ImcGen.exprImc.put(stmtExpr, expr);
		return expr;
	}

	@Override
	public ImcExpr visit(AstPreExpr preExpr, Stack<MemFrame> memFrames) {
		ImcExpr expr;
		if (preExpr.oper == AstPreExpr.Oper.NEW || preExpr.oper == AstPreExpr.Oper.DEL){
			MemLabel label;
			if (preExpr.oper == AstPreExpr.Oper.NEW)
				label = new MemLabel("new");
			else
				label = new MemLabel("del");

			Vector<Long> offset = new Vector<>();
			offset.add(0L);
			offset.add(8L);
			Vector<ImcExpr> args = new Vector<>();
			args.add(new ImcTEMP(memFrames.peek().FP));
			args.add(preExpr.subExpr.accept(this, memFrames));

			expr = new ImcCALL(
					label,
					offset,
					args);
		}
		else if (preExpr.oper == AstPreExpr.Oper.PTR){
			ImcExpr subexpr = preExpr.subExpr.accept(this, memFrames);
			if (subexpr instanceof ImcMEM)
				expr = ((ImcMEM)subexpr).addr;
			else
				expr = subexpr;
		}else{
			ImcUNOP.Oper operand = preExpr.oper == AstPreExpr.Oper.NOT ? ImcUNOP.Oper.NOT : ImcUNOP.Oper.NEG;
			expr = new ImcUNOP(operand, preExpr.subExpr.accept(this, memFrames));
		}

		ImcGen.exprImc.put(preExpr, expr);
		return expr;
	}

	@Override
	public ImcExpr visit(AstPstExpr pstExpr, Stack<MemFrame> memFrames) {
		ImcExpr expr = new ImcMEM(pstExpr.subExpr.accept(this, memFrames));
		ImcGen.exprImc.put(pstExpr, expr);
		return expr;
	}

	@Override
	public ImcExpr visit(AstCallExpr callExpr, Stack<MemFrame> memFrames) {
		Vector<Long> offsets = new Vector<>();
		Vector<ImcExpr> args = new Vector<>();

		MemFrame calledFunctionFrame = Memory.frames.get(SemAn.declaredAt.get(callExpr));
		int currentDepth = memFrames.peek().depth;
		int calledFunctionDepth = calledFunctionFrame.depth;
		int depthDiff = currentDepth - calledFunctionDepth;

		// Create the correct SL connection
		ImcExpr SL = new ImcTEMP(memFrames.peek().FP);
		if (calledFunctionDepth != 1){
			for (int i = -1; i < depthDiff; i++)
				SL = new ImcMEM(SL);
		}

		offsets.add(0L);
		args.add(SL);
		int counter = 1;
		for (AstExpr expr : callExpr.args.asts()){
			args.add(expr.accept(this, memFrames));
			offsets.add(counter*8L);
			counter++;
		}

		ImcExpr expr = new ImcCALL(
				calledFunctionFrame.label,
				offsets,
				args
		);
		ImcGen.exprImc.put(callExpr, expr);
		return  expr;
	}

	@Override
	public ImcExpr visit(AstCastExpr castExpr, Stack<MemFrame> memFrames) {
		ImcExpr expr;
		ImcExpr subExpr = castExpr.subExpr.accept(this, memFrames);
		if (castExpr.type instanceof AstPtrType){
			expr = subExpr;
		}else {
			AstAtomType.Kind type = ((AstAtomType)castExpr.type).kind;
			if (type == AstAtomType.Kind.CHAR){
				if (subExpr instanceof ImcCONST)
					expr = new ImcCONST(((ImcCONST) subExpr).value%256);
				else
					expr = new ImcBINOP(ImcBINOP.Oper.MOD, subExpr, new ImcCONST(256));
			}else {
				expr = subExpr;
			}
		}

		ImcGen.exprImc.put(castExpr, expr);
		return expr;
	}

	@Override
	public ImcExpr visit(AstBinExpr binExpr, Stack<MemFrame> memFrames) {
		ImcExpr expr;
		// Array logic
		if (binExpr.oper == AstBinExpr.Oper.ARR){
			ImcExpr name = binExpr.fstSubExpr.accept(this, memFrames);
			name = ((ImcMEM) name).addr;
			long size = SemAn.exprOfType.get(binExpr).size();
			ImcExpr arrExpr = binExpr.sndSubExpr.accept(this, memFrames);
			expr = new ImcMEM(new ImcBINOP(
					ImcBINOP.Oper.ADD,
					name,
					new ImcBINOP(ImcBINOP.Oper.MUL, arrExpr, new ImcCONST(size))
			));
			ImcGen.exprImc.put(binExpr, expr);
		}
		else { // All other binary operation logic
			ImcExpr firstExpression = binExpr.fstSubExpr.accept(this, memFrames);
			ImcExpr secondExpression = binExpr.sndSubExpr.accept(this, memFrames);
			expr = new ImcBINOP(ASTOperToIMCOper(binExpr.oper), firstExpression, secondExpression);
			ImcGen.exprImc.put(binExpr, expr);
		}
		return expr;
	}

	@Override
	public ImcExpr visit(AstConstExpr constExpr, Stack<MemFrame> memFrames) {
		long value;
		if (constExpr.kind == AstConstExpr.Kind.CHAR){
			value = constExpr.name.charAt(1);
			if (value == '\\')
				value = constExpr.name.charAt(2);
		} else if (constExpr.kind == AstConstExpr.Kind.VOID) {
			value = 0;
		} else {
			if (Objects.equals(constExpr.name, "nil"))
				value = 0;
			else
				value = Long.valueOf(constExpr.name);
		}
		ImcCONST constant = new ImcCONST(value);
		ImcGen.exprImc.put(constExpr, constant);
		return constant;
	}

	@Override
	public ImcExpr visit(AstNameExpr nameExpr, Stack<MemFrame> memFrames) {
		// Check if the variable is a local variable or a parameter
		MemAccess access = Memory.varAccesses.get(SemAn.declaredAt.get(nameExpr));
		if (access == null) access = Memory.parAccesses.get(SemAn.declaredAt.get(nameExpr));

		ImcExpr name;
		if (access instanceof MemAbsAccess){
			name = new ImcMEM(new ImcNAME(((MemAbsAccess) access).label));
		}else {
			int currentDepth = memFrames.peek().depth;
			int variableDepth = ((MemRelAccess) access).depth;
			int depthDiff = currentDepth - variableDepth;

			ImcExpr temp = new ImcTEMP(memFrames.peek().FP);
			for (int i = 0; i < depthDiff; i++)
				temp = new ImcMEM(temp);

			ImcExpr binop = new ImcBINOP(
					ImcBINOP.Oper.ADD,
					temp,
					new ImcCONST(((MemRelAccess) access).offset));

			name = new ImcMEM(binop);
		}
		ImcGen.exprImc.put(nameExpr, name);
		return name;
	}

	// ======================= HELPER FUNCTION =========================
	private ImcBINOP.Oper ASTOperToIMCOper (AstBinExpr.Oper opernad){
		switch (opernad){
			case ADD -> {return ImcBINOP.Oper.ADD;}
			case SUB -> {return ImcBINOP.Oper.SUB;}
			case AND -> {return ImcBINOP.Oper.AND;}
			case OR -> {return ImcBINOP.Oper.OR;}
			case EQU -> {return ImcBINOP.Oper.EQU;}
			case NEQ -> {return ImcBINOP.Oper.NEQ;}
			case LTH -> {return ImcBINOP.Oper.LTH;}
			case GTH -> {return ImcBINOP.Oper.GTH;}
			case LEQ -> {return ImcBINOP.Oper.LEQ;}
			case GEQ -> {return ImcBINOP.Oper.GEQ;}
			case MUL -> {return ImcBINOP.Oper.MUL;}
			case DIV -> {return ImcBINOP.Oper.DIV;}
			case MOD -> {return ImcBINOP.Oper.MOD;}
			default -> {throw new Report.Error("Internal compiler error when translating binary operand enums.");}
		}
	}

}
