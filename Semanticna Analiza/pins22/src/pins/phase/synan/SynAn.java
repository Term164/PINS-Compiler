package pins.phase.synan;

import pins.common.report.Location;
import pins.common.report.Report;
import pins.data.ast.*;
import pins.data.symbol.Symbol;
import pins.data.symbol.Token;
import pins.phase.lexan.*;

import java.util.ArrayList;

public class SynAn implements AutoCloseable {

	private final LexAn lexAn;
	private Symbol currentSymbol;
	private int cache = 0;
	private boolean expectedErrorMode = false;
	private String expectedCharacter;
	//private ArrayList<AST> asts;

	public SynAn(LexAn lexan) {
		this.lexAn = lexan;
	}
	
	public void close() {
	}
	
	public AST parser() {
		return parsePRG();
	}

	private void readAndConfirm(Token[] tokens){
		boolean legal = true;
		Token failedToken = Token.EOF;
		for (int i = 0; i < tokens.length; i++){
			read();
			if (peek() != tokens[i]){
				legal = false;
				failedToken = tokens[i];
				break;
			}
		}
		if (!legal) throw new Report.Error(currentSymbol.location, "Expected '" + failedToken.toString().toLowerCase() + "' but got '" + currentSymbol.lexeme+ "'");
	}

	private void readAndConfirm(Token token){
		read();
		if (token != peek())
			throw new Report.Error(currentSymbol.location, "Expected '" + token.toString().toLowerCase() + "' but got '" + currentSymbol.lexeme+ "'");
	}

	private void confirm(Token token){
		if (token != peek())
			throw new Report.Error(currentSymbol.location, "Expected '" + token.toString().toLowerCase() + "' but got '" + currentSymbol.lexeme+ "'");
	}

	private AST parsePRG(){
		read();
		switch (peek()) {
			case TYP, VAR, FUN -> {
				ArrayList<AstDecl> asts = new ArrayList<>();
				asts.add(parseDECL());
				return parsePRG_(asts);
			}
			case EOF -> throw new Report.Error("Empty file");
			default -> error(currentSymbol, "outside scope");
		}
		throw new Report.Error("Internal error when parsing the program");
	}

	private ASTs<AstDecl> parsePRG_(ArrayList<AstDecl> declarations){
		read();
		switch (peek()) {
			case TYP, VAR, FUN -> {
				declarations.add(parseDECL());
				return parsePRG_(declarations);
			}
			case RB, EOF -> {return new ASTs<AstDecl>(currentSymbol.location, declarations);}
			default -> error(currentSymbol, "outside scope");
		}
		throw new Report.Error("Internal error when constructing the abstract syntax tree at recursive prg");
	}

	private AstDecl parseDECL(){
		switch (peek()) {
			case VAR -> {
				Location startLocation = currentSymbol.location;
				readAndConfirm(Token.ID);
				String name = currentSymbol.lexeme;
				readAndConfirm(Token.COLON);
				AstVarDecl variableDeclaration = new AstVarDecl(startLocation, name, parseTYPE());
				readAndConfirm(Token.SEMICOLON);
				return variableDeclaration;
			}
			case TYP -> {
				Location startLocation = currentSymbol.location;
				readAndConfirm(Token.ID);
				String name = currentSymbol.lexeme;
				readAndConfirm(Token.ASSIGN);
				AstTypDecl typeDeclaration = new AstTypDecl(startLocation, name, parseTYPE());
				readAndConfirm(Token.SEMICOLON);
				return typeDeclaration;
			}
			case FUN -> {
				Location startingLocation = currentSymbol.location;
				readAndConfirm(Token.ID);
				String name = currentSymbol.lexeme;
				readAndConfirm(Token.LB);
				ASTs<AstParDecl> pars = parseARG();
				readAndConfirm(Token.COLON);
				AstType type = parseTYPE();
				readAndConfirm(Token.ASSIGN);
				AstExpr expr = parseEXPR();
				confirm(Token.SEMICOLON);
				return new AstFunDecl(startingLocation, name, pars, type, expr);
			}
			default -> error(currentSymbol, "declaration");
		}
		throw new Report.Error("Internal error when parsing declaration");
	}

	private ASTs<AstParDecl> parseARG(){
		ArrayList<AstParDecl> declarations = new ArrayList<>();
		read();
		switch (peek()) {
			case ID -> {
				Location location = currentSymbol.location;
				String name = currentSymbol.lexeme;
				readAndConfirm(Token.COLON);
				AstType type = parseTYPE();
				declarations.add(new AstParDecl(location, name, type));
				parseARG_(declarations);
				return new ASTs<AstParDecl>(currentSymbol.location, declarations);
			}
			case RB -> {
				return new ASTs<AstParDecl>(currentSymbol.location, declarations);
			}
			default -> error(currentSymbol, "function arguments");
		}
		throw new Report.Error("Internal error when constructing Abstract syntax tree at arg");
	}

	private void parseARG_(ArrayList<AstParDecl> declarations){
		read();
		switch (peek()){
			case COMMA -> {
				readAndConfirm(Token.ID);
				Location startLocation = currentSymbol.location;
				String name = currentSymbol.lexeme;
				readAndConfirm(Token.COLON);
				AstType type = parseTYPE();
				declarations.add(new AstParDecl(startLocation, name, type));
				parseARG_(declarations);
			}
			case RB -> {}
			default -> error(currentSymbol, "function arguments");
		}
	}

	private AstType parseTYPE(){
		read();
		switch (peek()) {
			case ID -> {
				return new AstTypeName(currentSymbol.location, currentSymbol.lexeme);
			}
			case INT -> {
				return new AstAtomType(currentSymbol.location, AstAtomType.Kind.INT);
			}
			case CHAR -> {
				return new AstAtomType(currentSymbol.location, AstAtomType.Kind.CHAR);
			}
			case VOID -> {
				return new AstAtomType(currentSymbol.location, AstAtomType.Kind.VOID);
			}
			case EXP -> {
				return new AstPtrType(currentSymbol.location, parseTYPE());
			}
			case LB -> {
				AstType type = parseTYPE();
				readAndConfirm(Token.RB);
				return type;
			}
			case LSB -> {
				Location location = currentSymbol.location;
				AstExpr expr = parseEXPR();
				confirm(Token.RSB);
				AstType type = parseTYPE();
				return new AstArrType(location, type, expr);
			}
			default -> error(currentSymbol, "type");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at type");
	}

	private AstExpr parseEXPR(){
		read();
		switch (peek()) {
			case ID, LB, EXP, CONST_CHAR, CONST_INT, CONST_POINT, CONST_VOID, LCB, PLUS, MINUS, NOT -> {
				AstExpr conjuctive = parseConjunctive();
				return parseDisjunctive(conjuctive);
			}
			case NEW,DEL -> {
				Location location = currentSymbol.location;
				AstPreExpr.Oper operand = parseNewDel();
				AstExpr expr = parseEXPR();
				return new AstPreExpr(location, operand, expr);
			}
			default -> error(currentSymbol, "expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at expr");
	}

	private AstExpr parseEXPR_(){
		switch (peek()){
			case CONST_CHAR -> {
				return new AstConstExpr(currentSymbol.location, AstConstExpr.Kind.CHAR, currentSymbol.lexeme);
			}
			case CONST_INT -> {
				return new AstConstExpr(currentSymbol.location, AstConstExpr.Kind.INT, currentSymbol.lexeme);
			}
			case CONST_POINT -> {
				return new AstConstExpr(currentSymbol.location, AstConstExpr.Kind.PTR, currentSymbol.lexeme);
			}
			case CONST_VOID -> {
				return new AstConstExpr(currentSymbol.location, AstConstExpr.Kind.VOID, currentSymbol.lexeme);
			}
			case LB -> {
				AstExpr newExpr = parseEXPR();
				AstExpr result = parseTypeCast(newExpr);
				confirm(Token.RB);
				return result;
			}
			case ID -> {
				return parseFuncall();
			}
			case LCB -> {
				Location location = currentSymbol.location;
				ArrayList<AstStmt> statements = new ArrayList<>();
				statements.add(parseStmt());
				ASTs<AstStmt> asts = parseCompound(statements);
				return new AstStmtExpr(location, asts);
			}
			default -> error(currentSymbol, "expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at final expression");
	}

	private AstStmt parseStmt(){
		read();
		switch (peek()){
			case ID,LB,EXP,CONST_CHAR,CONST_INT,CONST_POINT,CONST_VOID,LCB,NEW,DEL,PLUS,MINUS,NOT -> {
				Location location = currentSymbol.location;
				cache++;
				AstExpr expr = parseEXPR();
				expectedCharacter = ";";
				AstStmt result = parseAssigment(expr, location);
				expectedCharacter = "";
				return result;
			}
			case IF -> {
				ArrayList<AstStmt> stmts = new ArrayList<>();
				Location location = currentSymbol.location;
				// Set the error to expected as we know what should follow.
				expectedCharacter = "then";
				AstExpr expr = parseEXPR();
				expectedCharacter = "";
				stmts.add(parseStmt());
				expectedCharacter = "end";
				Location location2 = stmts.get(0).location;
				ASTs<AstStmt> asts = parseCondition(stmts);
				expectedCharacter = "";
				ASTs<AstStmt> asts2 = parseELIF();
				readAndConfirm(Token.SEMICOLON);
				AstExprStmt firstStatments = new AstExprStmt(location2, new AstStmtExpr(location2, asts));
				AstExprStmt secodStatments = new AstExprStmt(location2, new AstStmtExpr(location2, asts2));
				if (asts2 == null)
					return new AstIfStmt(location, expr, firstStatments);
				else return new AstIfStmt(location, expr, firstStatments, secodStatments);
			}
			case WHILE -> {
				ArrayList<AstStmt> statments = new ArrayList<>();
				Location location = currentSymbol.location;
				expectedCharacter = "do";
				AstExpr expr = parseEXPR();
				expectedCharacter = "";
				statments.add(parseStmt());
				expectedCharacter = "end";
				Location location2 = statments.get(0).location;
				ASTs<AstStmt> asts = parseCondition(statments);
				expectedCharacter = "";
				readAndConfirm(Token.SEMICOLON);
				return new AstWhileStmt(location, expr, new AstExprStmt(location2, new AstStmtExpr(location2, asts)));
			}
			default -> error(currentSymbol, "Statement");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at stmt");
	}

	private ASTs<AstStmt> parseELIF(){
		switch (peek()){
			case ELSE -> {
				ArrayList<AstStmt> stmts = new ArrayList<>();
				stmts.add(parseStmt());
				return parseCondition(stmts);
			}
			case END -> {return null;}
			default -> error(currentSymbol, "conditional else if statement");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at elif");
	}

	private ASTs<AstStmt> parseCondition(ArrayList<AstStmt> statments){
		if (expectedCharacter != null) expectedErrorMode = true;
		read();
		switch (peek()){
			case ID,LB,EXP,CONST_CHAR,CONST_INT,CONST_POINT,CONST_VOID,LCB,NEW,DEL,PLUS,MINUS,NOT,IF,WHILE -> {
				expectedErrorMode = false;
				cache++;
				statments.add(parseStmt());
				return parseCondition(statments);
			}
			case END,ELSE -> {
				expectedErrorMode = false;
				return new ASTs<>(currentSymbol.location, statments);
			}
			default -> error(currentSymbol, "conditional statement");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at condition");
	}

	private AstStmt parseAssigment(AstExpr left, Location location){
		if (expectedCharacter != null) expectedErrorMode = true;
		switch (peek()){
			case ASSIGN -> {
				expectedErrorMode = false;
				return new AstAssignStmt(location,left,parseEXPR());
			}
			case SEMICOLON -> {
				expectedErrorMode = true;
				return new AstExprStmt(location, left);
			}
			default -> error(currentSymbol, "assigment expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at assigment");
	}

	private ASTs<AstStmt> parseCompound(ArrayList<AstStmt> statements){
		read();
		switch (peek()){
			case ID,LB,EXP,CONST_CHAR,CONST_INT,CONST_POINT,CONST_VOID,LCB,NEW,DEL,PLUS,MINUS,NOT,IF,WHILE -> {
				cache++;
				statements.add(parseStmt());
				return parseCompound(statements);
			}
			case RCB -> {return new ASTs<>(currentSymbol.location, statements);}
			default -> error(currentSymbol, "compound expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at compound");
	}

	private AstExpr parseFuncall(){
		Location location = currentSymbol.location;
		String name = currentSymbol.lexeme;
		read();
		switch (peek()){
			case LB -> {
				ArrayList<AstExpr> arguments = new ArrayList<>();
				ASTs<AstExpr> args = parseFuncallArg(arguments);
				confirm(Token.RB);
				return new AstCallExpr(location, name, args);
			}
			case ASSIGN,SEMICOLON,COLON,RB,COMMA,LSB,RSB,EXP,WHERE,OR,AND,EQUAL,NOTEQUAL,LESS,GREATER,LESSEQUAL,GREATEREQUAL,PLUS,MINUS,MUL,DIV,MOD,THEN,DO -> {
				cache++;
				return new AstNameExpr(location, name);
			}
			default -> error(currentSymbol, "function call");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at funcall");
	}

	private ASTs<AstExpr> parseFuncallArg(ArrayList<AstExpr> arguments){
		read();
		switch (peek()){
			case ID,LB,EXP,CONST_CHAR,CONST_INT,CONST_POINT,CONST_VOID,LCB,NEW,DEL,PLUS,MINUS,NOT -> {
				cache++;
				arguments.add(parseEXPR());
				return parseFuncallArg_(arguments);
			}
			case RB -> {return new ASTs<AstExpr>(currentSymbol.location, arguments);}
			default -> error(currentSymbol, "function call arguments");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at funcall arg");
	}

	private ASTs<AstExpr> parseFuncallArg_(ArrayList<AstExpr> arguments){
		switch (peek()){
			case COMMA -> {
				arguments.add(parseEXPR());
				return parseFuncallArg_(arguments);
			}
			case RB -> {return new ASTs<AstExpr>(currentSymbol.location, arguments);}
			default -> error(currentSymbol, "function call arguments");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at recursive funcall arg");
	}

	private AstExpr parseTypeCast(AstExpr left){
		switch (peek()){
			case COLON -> {
				Location location = currentSymbol.location;
				AstType type = parseTYPE();
				read();
				return new AstCastExpr(location, left, type);
			}
			case RB -> {return left;}
			case WHERE -> {
				ArrayList<AstDecl> declarations = new ArrayList<>();

				Location location = currentSymbol.location;
				read();
				declarations.add(parseDECL());
				ASTs<AstDecl> decls = parsePRG_(declarations);
				return new AstWhereExpr(location, decls, left);
			}
			default -> error(currentSymbol, "typecast expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at typecast");
	}

	private AstPreExpr.Oper parseNewDel(){
		switch (peek()){
			case NEW -> {return AstPreExpr.Oper.NEW;}
			case DEL -> {return AstPreExpr.Oper.DEL;}
			default -> error(currentSymbol, "allocation expression");
		}
		throw new Report.Error("Internal error when parsing Abstract syntax tree at newDel");
	}

	private AstExpr parseDisjunctive(AstExpr left){
		switch (peek()){
			case OR -> {
				Location location = currentSymbol.location;
				read();
				AstExpr conjuctive = parseConjunctive();
				AstExpr right = parseDisjunctive(conjuctive);
				return new AstBinExpr(location, AstBinExpr.Oper.OR, left, right);
			}
			case ASSIGN,SEMICOLON,COLON,RB,COMMA,RSB,WHERE,THEN,DO -> {return left;}
			default -> error(currentSymbol, "arithmetic expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at disjunctive");
	}

	private AstExpr parseConjunctive(){
		switch (peek()){
			case ID, LB, EXP, CONST_CHAR, CONST_INT, CONST_POINT, CONST_VOID, LCB, PLUS, MINUS, NOT -> {
				return parseConjunctive_(parseRelational());
			}
			default -> error(currentSymbol, "arithmetic expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at conjunctive");
	}

	private AstExpr parseConjunctive_(AstExpr left){
		switch (peek()){
			case AND -> {
				Location location = currentSymbol.location;
				read();
				AstExpr right = parseConjunctive_(parseRelational());
				return new AstBinExpr(location, AstBinExpr.Oper.AND, left, right);
			}
			case ASSIGN,SEMICOLON,COLON,RB,COMMA,RSB,WHERE,THEN,DO,OR -> {return left;}
			default -> error(currentSymbol, "arithmetic expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at conjunctive recursive");
	}

	private AstExpr parseRelational(){
		switch (peek()){
			case ID, LB, EXP, CONST_CHAR, CONST_INT, CONST_POINT, CONST_VOID, LCB, PLUS, MINUS, NOT -> {
				return parseRelational_(parseAdditive());
			}
			default -> error(currentSymbol, "arithmetic expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at relational");
	}

	private AstExpr parseRelational_(AstExpr left){
		Location location = currentSymbol.location;
		switch (peek()){
			case EQUAL -> {
				read();
				AstExpr right = parseRelational_(parseAdditive());
				return new AstBinExpr(location, AstBinExpr.Oper.EQU, left, right);
			}
			case NOTEQUAL -> {
				read();
				AstExpr right = parseRelational_(parseAdditive());
				return new AstBinExpr(location, AstBinExpr.Oper.NEQ, left, right);
			}
			case GREATER -> {
				read();
				AstExpr right = parseRelational_(parseAdditive());
				return new AstBinExpr(location, AstBinExpr.Oper.GTH, left, right);
			}
			case LESS -> {
				read();
				AstExpr right = parseRelational_(parseAdditive());
				return new AstBinExpr(location, AstBinExpr.Oper.LTH, left, right);
			}
			case GREATEREQUAL -> {
				read();
				AstExpr right = parseRelational_(parseAdditive());
				return new AstBinExpr(location, AstBinExpr.Oper.GEQ, left, right);
			}
			case LESSEQUAL -> {
				read();
				AstExpr right = parseRelational_(parseAdditive());
				return new AstBinExpr(location, AstBinExpr.Oper.LEQ, left, right);
			}
			case ASSIGN,SEMICOLON,COLON,RB,COMMA,RSB,WHERE,THEN,DO,OR,AND -> {return left;}
			default -> error(currentSymbol, "arithmetic expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at recursive relational");
	}

	private AstExpr parseAdditive(){
		switch (peek()){
			case ID, LB, EXP, CONST_CHAR, CONST_INT, CONST_POINT, CONST_VOID, LCB, PLUS, MINUS, NOT -> {
				return parseAdditive_(parseMultiplicative());
			}
			default -> error(currentSymbol, "arithmetic expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at additive");
	}

	private AstExpr parseAdditive_(AstExpr left){
		Location location = currentSymbol.location;
		switch (peek()){
			case PLUS -> {
				read();
				AstExpr right = parseAdditive_(parseMultiplicative());
				return new AstBinExpr(location, AstBinExpr.Oper.ADD, left, right);
			}
			case MINUS -> {
				read();
				AstExpr right = parseAdditive_(parseMultiplicative());
				return new AstBinExpr(location, AstBinExpr.Oper.SUB, left, right);
			}
			case ASSIGN,SEMICOLON,COLON,RB,COMMA,RSB,WHERE,THEN,DO,OR,AND,EQUAL,NOTEQUAL,GREATER,LESS,GREATEREQUAL,LESSEQUAL -> {return left;}
			default -> error(currentSymbol, "arithmetic expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at recursive additive");
	}

	private AstExpr parseMultiplicative(){
		switch (peek()){
			case ID, LB, EXP, CONST_CHAR, CONST_INT, CONST_POINT, CONST_VOID, LCB, PLUS, MINUS, NOT -> {
				return parseMultiplicative_(parsePrefix());
			}
			default -> error(currentSymbol, "arithmetic expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at recursive multiplicative");
	}

	private AstExpr parseMultiplicative_(AstExpr left){
		Location location = currentSymbol.location;
		switch (peek()){
			case MUL -> {
				read();
				AstExpr right = parseMultiplicative_(parsePrefix());
				return new AstBinExpr(location, AstBinExpr.Oper.MUL, left, right);
			}
			case DIV -> {
				read();
				AstExpr right = parseMultiplicative_(parsePrefix());
				return new AstBinExpr(location, AstBinExpr.Oper.DIV, left, right);
			}
			case MOD -> {
				read();
				AstExpr right = parseMultiplicative_(parsePrefix());
				return new AstBinExpr(location, AstBinExpr.Oper.MOD, left, right);
			}
			case ASSIGN,SEMICOLON,COLON,RB,COMMA,RSB,WHERE,THEN,DO,OR,AND,EQUAL,NOTEQUAL,GREATER,LESS,GREATEREQUAL,LESSEQUAL,MINUS,PLUS -> {return left;}
			default -> error(currentSymbol, "arithmetic expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at recursive recursive multiplicative");
	}

	private AstExpr parsePrefix(){
		Location location = currentSymbol.location;
		switch (peek()){
			case ID, LB, CONST_CHAR, CONST_INT, CONST_POINT, CONST_VOID, LCB -> {
				return parsePostfix();
			}
			case EXP -> {
				read();
				return new AstPreExpr(location, AstPreExpr.Oper.PTR, parsePostfix());
			}
			case NOT -> {
				read();
				return new AstPreExpr(location, AstPreExpr.Oper.NOT, parsePostfix());
			}
			case PLUS -> {
				read();
				return new AstPreExpr(location, AstPreExpr.Oper.ADD, parsePostfix());
			}
			case MINUS -> {
				read();
				return new AstPreExpr(location, AstPreExpr.Oper.SUB, parsePostfix());
			}
			default -> error(currentSymbol, "arithmetic expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at prefix");
	}

	private AstExpr parsePostfix(){
		switch (peek()){
			case ID, LB, CONST_CHAR, CONST_INT, CONST_POINT, CONST_VOID, LCB  -> {
				return parsePostfix_(parseEXPR_());
			}
			default -> error(currentSymbol, "arithmetic expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at postfix");
	}

	private AstExpr parsePostfix_(AstExpr left){
		if (expectedCharacter != null){
			expectedErrorMode = true;
		}
		read();
		switch (peek()){
			case LSB -> {
				Location location = currentSymbol.location;
				expectedErrorMode = false;
				AstExpr inner = parseEXPR();
				AstExpr result = new AstBinExpr(location, AstBinExpr.Oper.ARR, left, inner);
				confirm(Token.RSB);
				return parsePostfix_(result);
			}
			case EXP -> {
				Location location = currentSymbol.location;
				expectedErrorMode = false;
				AstExpr result = parsePostfix_(left);
				return new AstPstExpr(location, AstPstExpr.Oper.PTR, result);
			}
			case ASSIGN,SEMICOLON,COLON,RB,COMMA,RSB,WHERE,THEN,DO,OR,AND,EQUAL,NOTEQUAL,GREATER,LESS,GREATEREQUAL,LESSEQUAL,MINUS,PLUS,DIV,MUL,MOD -> {
				expectedErrorMode = false;
				return left;
			}
			default -> error(currentSymbol, "arithmetic expression");
		}
		throw new Report.Error("Internal error when constructing abstract syntax tree at recursive postfix");
	}

	private void error(Symbol symbol, String production){
		if (expectedErrorMode){
			throw new Report.Error(symbol.location, "SYNTAX ERROR: expected '" + expectedCharacter + "' but got '" + symbol.lexeme + "'");
		}
		else{
			if (symbol.lexeme.length() == 1){
				throw new Report.Error(symbol.location, "SYNTAX ERROR: Unexpected character '" + symbol.lexeme + "' when parsing " + production);
			}else {
				throw new Report.Error(symbol.location, "SYNTAX ERROR: Unexpected sequence '" + symbol.lexeme + "' when parsing " + production);
			}
		}
	}

	private Token peek(){
		return currentSymbol.token;
	}

	private void read(){
		if (cache == 0){
			currentSymbol = lexAn.lexer();
		}else cache--;
	}

	private void read(boolean save){
		if (save){
			cache++;
			currentSymbol = lexAn.lexer();
		}
	}

}
