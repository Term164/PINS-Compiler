package pins.phase.seman;

import pins.common.report.Report;
import pins.data.ast.*;
import pins.data.ast.visitor.AstFullVisitor;
import pins.data.typ.*;

public class TypeChecker extends AstFullVisitor<Object, Boolean> {

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
            if (t instanceof AstFunDecl){

                t.accept(this, true);
            }

        // visit everything after the declarations have been handled
        super.visit(trees, false);

        // Get the last object if it exists and return its type
        if (trees.asts().size() != 0){
            return trees.asts().get(trees.asts().size()-1).accept(this, saveDeclaration);
        }else return null;
    }

    // =========================================================================
    // ========================== DECLARATIONS =================================
    // =========================================================================

    @Override
    public Object visit(AstFunDecl funDecl, Boolean aBoolean) {
        SemType returnType = (SemType) funDecl.type.accept(this, aBoolean);
        if (returnType instanceof SemArr)
            throw new Report.Error(funDecl.location, "Semantic error: Function cannot return array type.");

        SemType actualType = (SemType) funDecl.expr.accept(this, aBoolean);
        if (returnType.actualType().getClass() != actualType.actualType().getClass())
            throw new Report.Error(funDecl.location, "Semantic error: return type does not match expected return type.");

        if (funDecl.pars != null)
            funDecl.pars.accept(this, aBoolean);
        return  returnType;
    }

    @Override
    public Object visit(AstParDecl parDecl, Boolean aBoolean) {
        SemType type = (SemType) parDecl.type.accept(this, aBoolean);
        if (type instanceof SemVoid) throw new Report.Error(parDecl.location, "Semantic error: Function parameters cannot be of type void.");
        else if (type instanceof SemArr) throw new Report.Error(parDecl.location, "Semantic error: Function parameters cannot be of type array");
        return type;
    }

    @Override
    public Object visit(AstTypDecl typDecl, Boolean aBoolean) {
        if (!SemAn.declaresType.containsKey(typDecl)){
            SemName name = new SemName(typDecl.name);
            SemType type = (SemType) typDecl.type.accept(this, aBoolean);
            name.define(type.actualType());
            SemAn.declaresType.put(typDecl, name);
        }
        return super.visit(typDecl, aBoolean);
    }

    // =========================================================================
    // ============================== TYPES ====================================
    // =========================================================================

    @Override
    public Object visit(AstTypeName typeName, Boolean aBoolean) {
        if (!SemAn.declaresType.containsKey(SemAn.declaredAt.get(typeName))){
            SemAn.declaredAt.get(typeName).accept(this, aBoolean);
        }
        SemName declaration = SemAn.declaresType.get(SemAn.declaredAt.get(typeName));
        SemAn.describesType.put(typeName, declaration.actualType());
        return declaration;
    }

    @Override
    public Object visit(AstPtrType ptrType, Boolean aBoolean) {
        SemType type = new SemPtr( (SemType) ptrType.subType.accept(this, aBoolean));
        SemAn.describesType.put(ptrType, type.actualType());
        super.visit(ptrType, aBoolean);
        return type;
    }

    @Override
    public Object visit(AstArrType arrType, Boolean aBoolean) {
        SemType type = new SemArr((SemType) arrType.elemType.accept(this, aBoolean), Integer.parseInt(((AstConstExpr) (arrType).size).name));
        SemAn.describesType.put(arrType, type.actualType());
        super.visit(arrType, aBoolean);
        return type;
    }

    // =========================================================================
    // ========================== EXPRESSIONS ==================================
    // =========================================================================

    @Override
    public Object visit(AstNameExpr nameExpr, Boolean aBoolean) {
        SemType type = (SemType) SemAn.declaredAt.get(nameExpr).type.accept(this, aBoolean);
        SemAn.exprOfType.put(nameExpr, type.actualType());
        return type;

    }

    @Override
    public Object visit(AstPreExpr preExpr, Boolean aBoolean) {
        SemType type = (SemType) preExpr.subExpr.accept(this, aBoolean);
        switch (preExpr.oper){
            case PTR -> {
                SemAn.exprOfType.put(preExpr, new SemPtr(type));
                return new SemPtr(type);
            }
            case ADD -> {
                if (type instanceof SemInt){
                    SemAn.exprOfType.put(preExpr, new SemInt());
                }else {
                    throw new Report.Error(preExpr.location, "Semantic error: Illegal unary operator '+' used on non integer type.");
                }
            }
            case SUB -> {
                if (type instanceof SemInt){
                    SemAn.exprOfType.put(preExpr, new SemInt());
                }else {
                    throw new Report.Error(preExpr.location, "Semantic error: Illegal unary operator '-' used on non integer type.");
                }
            }
            case NEW -> {
                type = new SemPtr(new SemVoid());
                SemAn.exprOfType.put(preExpr, type.actualType());
            }
            case DEL -> {
                type = new SemVoid();
                SemAn.exprOfType.put(preExpr, type.actualType());
            }
            case NOT -> {
                if (type instanceof SemInt){
                    type = new SemInt();
                    SemAn.exprOfType.put(preExpr, type.actualType());
                }else
                    throw new Report.Error(preExpr.location, "Semantic error: Illegal unary operator '!' used on non integer type");
            }
        }
        return type;
    }

    @Override
    public Object visit(AstBinExpr binExpr, Boolean aBoolean) {
        SemType leftOperand = (SemType) binExpr.fstSubExpr.accept(this, aBoolean);
        SemType rightOperand = (SemType) binExpr.sndSubExpr.accept(this, aBoolean);
        if (binExpr.oper.equals(AstBinExpr.Oper.ARR)){
            if (leftOperand.actualType() instanceof SemArr){
                SemAn.exprOfType.put(binExpr, ((SemArr)leftOperand.actualType()).elemType.actualType());
                return ((SemArr)leftOperand.actualType()).elemType.actualType();
            }else throw new Report.Error(binExpr.location, "ERROR: Cannot index a pointer");

        }else {
            if (leftOperand.actualType().getClass() == rightOperand.actualType().getClass()){
                if (leftOperand.actualType() instanceof SemInt){
                    SemAn.exprOfType.put(binExpr, new SemInt());
                }
                else if (leftOperand.actualType() instanceof SemChar){
                    switch (binExpr.oper){
                        case NEQ,EQU,LTH,GTH,LEQ,GEQ -> SemAn.exprOfType.put(binExpr, new SemInt());
                        default -> throw new Report.Error(binExpr.location, "Semantic error: Illegal binary operator '"+ binExpr.oper +"' between two characters");
                    }
                }else if (leftOperand.actualType() instanceof SemPtr){
                    if (((SemPtr) leftOperand).baseType.actualType().getClass() == ((SemPtr)rightOperand).baseType.actualType().getClass()){
                        switch (binExpr.oper){
                            case NEQ,EQU,LTH,GTH,LEQ,GEQ -> SemAn.exprOfType.put(binExpr, new SemInt());
                            default -> throw new Report.Error(binExpr.location, "Semantic error: Illegal binary operator '"+ binExpr.oper +"' between two pointers");
                        }
                    }else
                        throw new Report.Error(binExpr.location, "Semantic error: Left pointer and right pointer of binary operator '" + binExpr.oper + "' do not have the same base type");
                }
            }else {
                throw new Report.Error(binExpr.location, "Semantic error: Left and right side of binary operator '" + binExpr.oper + "' are not of the same type.");
            }
        }
        return new SemInt();
    }

    @Override
    public Object visit(AstPstExpr pstExpr, Boolean aBoolean) {
        SemType type = (SemType) pstExpr.subExpr.accept(this, aBoolean);
        if (type instanceof SemPtr){
            if (pstExpr.oper.equals(AstPstExpr.Oper.PTR)){
                SemAn.exprOfType.put(pstExpr, ((SemPtr) type.actualType()).baseType);
                return ((SemPtr) type.actualType()).baseType;
            }
            return type;
        }else {
            throw new Report.Error(pstExpr.location, "TYPE ERROR: cannot dereference a non pointer type.");
        }

    }

    @Override
    public Object visit(AstCastExpr castExpr, Boolean aBoolean) {
        SemType type = (SemType) castExpr.type.accept(this, aBoolean);
        SemType expr = (SemType) castExpr.subExpr.accept(this, aBoolean);
        if ((type.actualType() instanceof SemChar || type.actualType() instanceof SemInt || type.actualType() instanceof SemPtr) &&
                (expr.actualType() instanceof SemChar || expr.actualType() instanceof SemInt || expr.actualType() instanceof SemPtr)){
            SemAn.exprOfType.put(castExpr,  type.actualType());
            return type;
        }else throw new Report.Error(castExpr.location, "Cannot typecast objects that are not of type int, char or pointer.");
    }

    @Override
    public Object visit(AstWhereExpr whereExpr, Boolean aBoolean) {
        SemType type = (SemType) whereExpr.subExpr.accept(this, aBoolean);
        SemAn.exprOfType.put(whereExpr, type.actualType());
        super.visit(whereExpr, aBoolean);
        return type;
    }

    @Override
    public Object visit(AstStmtExpr stmtExpr, Boolean aBoolean) {
        SemType type = (SemType) stmtExpr.stmts.accept(this, aBoolean);
        SemAn.exprOfType.put(stmtExpr, type.actualType());
        return type;
    }

    @Override
    public Object visit(AstCallExpr callExpr, Boolean aBoolean) {
        ASTs<AstParDecl> functionParams = ((AstFunDecl) SemAn.declaredAt.get(callExpr)).pars;
        if (functionParams.asts().size() == callExpr.args.asts().size()){
            for (int i = 0; i < functionParams.asts().size(); i++){
                if (functionParams.asts().get(i).type.accept(this, aBoolean).getClass() != callExpr.args.asts().get(i).accept(this, aBoolean).getClass()){
                    throw new Report.Error(callExpr.location, "Semantic error: Function arguments do not match the parameter types.");
                }
            }
        }else throw new Report.Error(callExpr.location, "Semantic error: number of arguments do not match for function '" + callExpr.name +"'");

        SemType type = (SemType) SemAn.declaredAt.get(callExpr).type.accept(this, aBoolean);
        super.visit(callExpr, aBoolean);
        SemAn.exprOfType.put(callExpr, type.actualType());
        return type;
    }

    // =========================================================================
    // =========================== STATEMENTS ==================================
    // =========================================================================

    @Override
    public Object visit(AstExprStmt exprStmt, Boolean aBoolean) {
        SemType type = (SemType) exprStmt.expr.accept(this, aBoolean);
        SemAn.stmtOfType.put(exprStmt, type.actualType());
        return type;
    }

    @Override
    public Object visit(AstAssignStmt assignStmt, Boolean aBoolean) {
        SemType left = (SemType) assignStmt.fstSubExpr.accept(this, aBoolean);
        SemType right = (SemType) assignStmt.sndSubExpr.accept(this, aBoolean);
        if (left.actualType().getClass() == right.actualType().getClass()){
            if (left instanceof SemPtr){
                if (!(((SemPtr) left).baseType.getClass() == ((SemPtr) right).baseType.getClass()))
                    throw new Report.Error(assignStmt.location, "Semantic error: Base type of pointer must be the same when assigning values.");
            }
            SemAn.stmtOfType.put(assignStmt, new SemVoid());
        }else throw new Report.Error(assignStmt.location, "Semantic error: Types must be the same when assigning values.");
        return new SemVoid();
    }

    @Override
    public Object visit(AstIfStmt ifStmt, Boolean aBoolean) {
        SemType expr = (SemType) ifStmt.condExpr.accept(this, aBoolean);
        SemType thenStmt = (SemType) ifStmt.thenBodyStmt.accept(this, aBoolean);
        SemType elseStmt = null;
        if (ifStmt.elseBodyStmt != null)
            elseStmt = (SemType) ifStmt.elseBodyStmt.accept(this, aBoolean);

        if (expr instanceof SemInt && thenStmt instanceof SemVoid){
            if (elseStmt != null){
                if (!(elseStmt instanceof SemVoid)) throw new Report.Error(ifStmt.location, "Semantic error: Else statement must be void.");
            }
            SemAn.stmtOfType.put(ifStmt, new SemVoid());
        }else throw new Report.Error(ifStmt.location, "Semantic error: If expression must be of type int and then statement must be void.");

        return new SemVoid();
    }

    @Override
    public Object visit(AstWhileStmt whileStmt, Boolean aBoolean) {
        SemType expr = (SemType) whileStmt.condExpr.accept(this, aBoolean);
        SemType body = (SemType) whileStmt.bodyStmt.accept(this, aBoolean);
        if (expr instanceof SemInt && body instanceof SemVoid){
            SemAn.stmtOfType.put(whileStmt, new SemVoid());
        }else throw new Report.Error(whileStmt.location, "Semantic error: While expression must be of type int and body must return void");
        return new SemVoid();
    }

    // =========================================================================
    // ============================= CONSTANTS =================================
    // =========================================================================

    @Override
    public Object visit(AstAtomType atomType, Boolean aBoolean) {
        SemType type;
        switch (atomType.kind){
            case INT -> type = new SemInt();
            case CHAR -> type = new SemChar();
            case VOID -> type = new SemVoid();
            default -> throw new Report.Error("Semantic error: Internal compiler error when parsing types");
        }
        SemAn.describesType.put(atomType, type.actualType());
        return type;
    }

    @Override
    public Object visit(AstConstExpr constExpr, Boolean aBoolean) {
        SemType type;
        switch (constExpr.kind){
            case INT -> type = new SemInt();
            case CHAR -> type = new SemChar();
            case VOID -> type = new SemVoid();
            case PTR -> type = new SemPtr(new SemVoid());
            default -> throw new Report.Error("Semantic error: Internal compiler error when parsing types");
        }
        SemAn.exprOfType.put(constExpr, type.actualType());
        return type;
    }
}
