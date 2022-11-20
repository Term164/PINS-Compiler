package pins.phase.imcgen;

import java.util.*;

// import pins.data.ast.*;
import pins.common.report.Report;
import pins.data.ast.AST;
import pins.data.ast.ASTs;
import pins.data.ast.AstFunDecl;
import pins.data.ast.visitor.*;
import pins.data.imc.code.expr.ImcExpr;
import pins.data.imc.code.expr.ImcTEMP;
import pins.data.imc.code.stmt.ImcMOVE;
import pins.data.mem.*;
import pins.phase.memory.Memory;
// import pins.phase.memory.*;

public class CodeGenerator extends AstFullVisitor<Object, Stack<MemFrame>> {

	// TODO: scan the entire AST and trigger ExprGenerator where appropriate

    @Override
    public Object visit(ASTs<? extends AST> trees, Stack<MemFrame> memFrames) {
        if (memFrames == null)
            memFrames = new Stack<>();
        for (AST tree : trees.asts()){
            if (tree instanceof AstFunDecl)
                tree.accept(this, memFrames);
        }
        return null;
    }

    @Override
    public Object visit(AstFunDecl funDecl, Stack<MemFrame> memFrames) {
        memFrames.push(Memory.frames.get(funDecl));
        ImcExpr expr;
        if (funDecl.expr != null)
            expr = funDecl.expr.accept(new ExprGenerator(), memFrames);
        else
            throw new Report.InternalError();
        //ImcGen.exprImc.put(funDecl.expr, new ImcMOVE( new ImcTEMP(memFrames.peek().RV), expr));
        memFrames.pop();
        return null;
    }
}
