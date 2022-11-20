package pins.phase.seman;

import pins.common.report.Report;
import pins.data.ast.*;
import pins.data.ast.visitor.AstFullVisitor;

public class NameResolver extends AstFullVisitor<Object, Boolean> {
    private final SymbTable symbTable;

    public NameResolver(){
        symbTable = new SymbTable();
    }

    @Override
    public Object visit(ASTs<? extends AST> trees, Boolean saveDeclaration) {

        // Loop through all Type declarations
        for (AST t : trees.asts())
            if (t instanceof AstTypDecl)
                t.accept(this, true);

        // Loop through all variable declarations
        for (AST t : trees.asts())
            if (t instanceof AstVarDecl)
                t.accept(this, true);

        // Loop through all parameter declarations
        for (AST t : trees.asts())
            if (t instanceof AstParDecl)
                t.accept(this, true);

        // Loop through all function declarations
        for (AST t : trees.asts())
            if (t instanceof AstFunDecl)
                t.accept(this, true);

        // visit everything after the declarations have been handled
        super.visit(trees, false);

        return null;
    }

    // =========================================================================
    // ========================== DECLARATIONS =================================
    // =========================================================================

    @Override
    public Object visit(AstTypDecl typDecl, Boolean saveDeclaration){
        try {
            if (saveDeclaration)
                symbTable.ins(typDecl.name, typDecl);
        }
        catch (SymbTable.CannotInsNameException e){
            throw new Report.Error(typDecl.location, "Semantic Error: cannot redefine '" + typDecl.name + "' as a type");
        }
        if (!saveDeclaration)
            super.visit(typDecl, false);
        return null;
    }

    @Override
    public Object visit(AstVarDecl varDecl, Boolean saveDeclaration) {
        try {
            if (saveDeclaration)
                symbTable.ins(varDecl.name, varDecl);
        }
        catch (SymbTable.CannotInsNameException e){
            throw new Report.Error(varDecl.location, "Semantic Error: cannot redefine '" + varDecl.name + "' as a variable");
        }
        if (!saveDeclaration)
            super.visit(varDecl, false);
        return null;
    }

    @Override
    public Object visit(AstParDecl parDecl, Boolean saveDeclaration) {
        try {
            if (saveDeclaration)
                symbTable.ins(parDecl.name, parDecl);
        }
        catch (SymbTable.CannotInsNameException e){
            throw new Report.Error(parDecl.location, "Semantic Error: cannot redefine '" + parDecl.name + "' as a parameter");
        }
        if (!saveDeclaration)
            super.visit(parDecl, false);
        return null;
    }

    @Override
    public Object visit(AstFunDecl funDecl, Boolean saveDeclaration) {
        try {
            if (saveDeclaration)
                symbTable.ins(funDecl.name, funDecl);
        }
        catch (SymbTable.CannotInsNameException e){
            throw new Report.Error(funDecl.location, "Semantic Error: cannot redefine '" + funDecl.name + "' as a function");
        }
        if (!saveDeclaration){
            symbTable.newScope();
            super.visit(funDecl, false);
            symbTable.oldScope();
        }
        return null;
    }

    // =========================================================================
    // ========================== EXPRESSIONS ==================================
    // =========================================================================

    @Override
    public Object visit(AstTypeName typeName, Boolean saveDeclaration) {
        try {
            SemAn.declaredAt.put(typeName, symbTable.fnd(typeName.name));
        }catch (SymbTable.CannotFndNameException e){
                throw new Report.Error(typeName.location,"Semantic Error: type '" + typeName.name + "' is not declared.");
        }
        return null;
    }

    @Override
    public Object visit(AstNameExpr nameExpr, Boolean saveDeclaration) {
        try {
            SemAn.declaredAt.put(nameExpr, symbTable.fnd(nameExpr.name));
        }catch (SymbTable.CannotFndNameException e) {
                throw new Report.Error(nameExpr.location,"Semantic Error: variable '" + nameExpr.name + "' is not declared.");
        }
        return null;
    }

    @Override
    public Object visit(AstWhereExpr whereExpr, Boolean saveDeclaration) {
        symbTable.newScope();
        if (whereExpr.decls != null)
            whereExpr.decls.accept(this, saveDeclaration);
        if (whereExpr.subExpr != null)
            whereExpr.subExpr.accept(this, saveDeclaration);
        symbTable.oldScope();
        return null;
    }

    @Override
    public Object visit(AstCallExpr callExpr, Boolean saveDeclaration) {
        try {
            SemAn.declaredAt.put(callExpr, symbTable.fnd(callExpr.name));
        }catch (SymbTable.CannotFndNameException e ){
                throw new Report.Error(callExpr.location,"Semantic Error: function '" + callExpr.name + "' is not declared.");
        }
        if (!saveDeclaration)
            super.visit(callExpr, false);
        return null;
    }
}
