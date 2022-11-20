package pins.phase.synan;

import pins.common.report.Report;
import pins.data.symbol.Symbol;
import pins.data.symbol.Token;
import pins.phase.lexan.*;

public class SynAn implements AutoCloseable {

	private final LexAn lexAn;
	private Symbol currentSymbol;
	private int cache = 0;
	private boolean expectedErrorMode = false;
	private String expectedCharacter;

	public SynAn(LexAn lexan) {
		this.lexAn = lexan;
	}
	
	public void close() {
	}
	
	public void parser() {
		parsePRG();
	}

	private void readAndConfirm(Token[] tokens){
		boolean legal = true;
		Token failedToken = Token.EOF;
		for (int i = 0; i < tokens.length; i++){
			read();
			//System.out.println(peek() + " | " + tokens[i]);
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

	private void parsePRG(){
		read();
		switch (peek()) {
			case TYP, VAR, FUN -> {
				System.out.println("prg -> decl prg'");
				parseDECL();
				parsePRG_();
			}
			case EOF -> throw new Report.Error("Empty file");
			default -> error(currentSymbol, "outside scope");
		}
	}

	private void parseDECL(){
		switch (peek()) {
			case VAR -> {
				readAndConfirm(new Token[]{Token.ID, Token.COLON});
				System.out.println("decl -> var id : type;");
				parseTYPE();
				readAndConfirm(Token.SEMICOLON);
			}
			case TYP -> {
				readAndConfirm(new Token[]{Token.ID, Token.ASSIGN});
				System.out.println("decl -> typ id = type;");
				parseTYPE();
				readAndConfirm(Token.SEMICOLON);
			}
			case FUN -> {
				readAndConfirm(new Token[]{Token.ID, Token.LB});
				System.out.println("decl -> fun id (arg) : type=expr;");
				parseARG();
				readAndConfirm(Token.COLON);
				parseTYPE();
				readAndConfirm(Token.ASSIGN);
				parseEXPR();
				confirm(Token.SEMICOLON);
			}
			default -> error(currentSymbol, "declaration");
		}
	}

	private void parsePRG_(){
		read();
		switch (peek()) {
			case TYP, VAR, FUN -> {
				System.out.println("prg' -> decl prg'");
				parseDECL();
				parsePRG_();
			}
			case RB, EOF -> System.out.println("prg' ->  .");
			default -> error(currentSymbol, "outside scope");
		}
	}

	private void parseARG(){
		read();
		switch (peek()) {
			case ID -> {
				System.out.println("arg -> id : type arg'");
				readAndConfirm(Token.COLON);
				parseTYPE();
				parseARG_();
			}
			case RB -> System.out.println("arg ->  .");
			default -> error(currentSymbol, "function arguments");
		}
	}

	private void parseARG_(){
		read();
		switch (peek()){
			case COMMA -> {
				System.out.println("arg' -> , id : type arg'");
				readAndConfirm(new Token[]{Token.ID, Token.COLON});
				parseTYPE();
				parseARG_();
			}
			case RB -> System.out.println("arg' ->  .");
			default -> error(currentSymbol, "function arguments");
		}
	}

	private void parseTYPE(){
		read();
		switch (peek()) {
			case ID -> System.out.println("type -> id");
			case INT -> System.out.println("type -> int");
			case CHAR -> System.out.println("type -> char");
			case VOID -> System.out.println("type -> void");
			case EXP -> {
				System.out.println("type -> ^type");
				parseTYPE();
			}
			case LB -> {
				System.out.println("type -> (type)");
				parseTYPE();
				readAndConfirm(Token.RB);
			}
			case LSB -> {
				System.out.println("type -> [expr] type");
				parseEXPR();
				confirm(Token.RSB);
				parseTYPE();
			}
			default -> error(currentSymbol, "type");
		}
	}

	private void parseEXPR(){
		read();
		switch (peek()) {
			case ID, LB, EXP, CONST_CHAR, CONST_INT, CONST_POINT, CONST_VOID, LCB, PLUS, MINUS, NOT -> {
				System.out.println("expr -> C D'");
				parseConjunctive();
				parseDisjunctive();
			}
			case NEW,DEL -> {
				System.out.println("expr -> newdel expr");
				parseNewDel();
				parseEXPR();
			}
			default -> error(currentSymbol, "expression");
		}
	}

	private void parseEXPR_(){
		switch (peek()){
			case CONST_CHAR,CONST_INT,CONST_POINT,CONST_VOID -> System.out.println("expr' -> const");
			case LB -> {
				System.out.println("expr' -> (expr typecast)");
				parseEXPR();
				parseTypeCast();
				confirm(Token.RB);
			}
			case ID -> {
				System.out.println("expr' -> id funcall");
				parseFuncall();
			}
			case LCB -> {
				System.out.println("expr' -> stmt compound");
				parseStmt();
				parseCompound();
			}
			default -> error(currentSymbol, "expression");
		}
	}

	private void parseStmt(){
		read();
		switch (peek()){
			case ID,LB,EXP,CONST_CHAR,CONST_INT,CONST_POINT,CONST_VOID,LCB,NEW,DEL,PLUS,MINUS,NOT -> {
				System.out.println("stmt -> expr assigment;");
				cache++;
				parseEXPR();
				expectedCharacter = ";";
				parseAssigment();
				expectedCharacter = "";
			}
			case IF -> {
				System.out.println("stmt -> if expr then stmt condition elif end;");
				// Set the error to expected as we know what should follow.
				expectedCharacter = "then";
				parseEXPR();
				expectedCharacter = "";
				parseStmt();
				System.out.println("test");
				expectedCharacter = "end";
				parseCondition();
				expectedCharacter = "";
				parseELIF();
				readAndConfirm(Token.SEMICOLON);
			}
			case WHILE -> {
				System.out.println("stmt -> while expr do stmt condition end;");
				expectedCharacter = "do";
				parseEXPR();
				expectedCharacter = "";
				parseStmt();
				expectedCharacter = "end";
				parseCondition();
				expectedCharacter = "";
				readAndConfirm(Token.SEMICOLON);
			}
			default -> error(currentSymbol, "Statement");
		}
	}

	private void parseELIF(){
		switch (peek()){
			case ELSE -> {
				System.out.println("elif -> stmt condition");
				parseStmt();
				parseCondition();
			}
			case END -> System.out.println("elif ->  .");
			default -> error(currentSymbol, "conditional else if statement");
		}
	}

	private void parseCondition(){
		if (expectedCharacter != null) expectedErrorMode = true;
		read();
		switch (peek()){
			case ID,LB,EXP,CONST_CHAR,CONST_INT,CONST_POINT,CONST_VOID,LCB,NEW,DEL,PLUS,MINUS,NOT,IF,WHILE -> {
				expectedErrorMode = false;
				System.out.println("condition -> stmt condition");
				cache++;
				parseStmt();
				parseCondition();
			}
			case END,ELSE -> {
				expectedErrorMode = false;
				System.out.println("condition ->  .");
			}
			default -> error(currentSymbol, "conditional statement");
		}
	}

	private void parseAssigment(){
		if (expectedCharacter != null) expectedErrorMode = true;
		switch (peek()){
			case ASSIGN -> {
				expectedErrorMode = false;
				System.out.println("assigment -> = expr");
				parseEXPR();
			}
			case SEMICOLON -> {
				expectedErrorMode = true;
				System.out.println("assigment ->  .");
			}
			default -> error(currentSymbol, "assigment expression");
		}
	}

	private void parseCompound(){
		read();
		switch (peek()){
			case ID,LB,EXP,CONST_CHAR,CONST_INT,CONST_POINT,CONST_VOID,LCB,NEW,DEL,PLUS,MINUS,NOT,IF,WHILE -> {
				System.out.println("compound -> stmt compound");
				cache++;
				parseStmt();
				parseCompound();
			}
			case RCB -> System.out.println("compound' ->  .");
			default -> error(currentSymbol, "compound expression");
		}
	}

	private void parseFuncall(){
		read();
		switch (peek()){
			case LB -> {
				System.out.println("funcall -> (funcallarg)");
				parseFuncallArg();
				confirm(Token.RB);
			}
			case ASSIGN,SEMICOLON,COLON,RB,COMMA,LSB,RSB,EXP,WHERE,OR,AND,EQUAL,NOTEQUAL,LESS,GREATER,LESSEQUAL,GREATEREQUAL,PLUS,MINUS,MUL,DIV,MOD,THEN,DO -> {
				cache++;
				System.out.println("funcall ->  .");
			}
			default -> error(currentSymbol, "function call");
		}
	}

	private void parseFuncallArg(){
		read();
		//System.out.println(peek());
		switch (peek()){
			case ID,LB,EXP,CONST_CHAR,CONST_INT,CONST_POINT,CONST_VOID,LCB,NEW,DEL,PLUS,MINUS,NOT -> {
				System.out.println("funcallarg -> expr funcallarg'");
				cache++;
				parseEXPR();
				parseFuncallArg_();
			}
			case RB -> System.out.println("funcallarg ->  .");
			default -> error(currentSymbol, "function call arguments");
		}
	}

	private void parseFuncallArg_(){
		switch (peek()){
			case COMMA -> {
				System.out.println("funcallarg' -> , expr funcallarg'");
				parseEXPR();
				parseFuncallArg_();
			}
			case RB -> System.out.println("funcallarg' ->  .");
			default -> error(currentSymbol, "function call arguments");
		}
	}

	private void parseTypeCast(){
		switch (peek()){
			case COLON -> {
				System.out.println("typecast -> : type");
				parseTYPE();
				read();
			}
			case RB -> System.out.println("typecast ->  .");
			case WHERE -> {
				read();
				System.out.println("typecast -> where decl prg'");
				parseDECL();
				parsePRG_();
			}
			default -> error(currentSymbol, "typecast expression");
		}
	}

	private void parseNewDel(){
		switch (peek()){
			case NEW -> System.out.println("newdel -> new");
			case DEL -> System.out.println("newdel -> del");
			default -> error(currentSymbol, "allocation expression");
		}
	}

	private void parseDisjunctive(){
		switch (peek()){
			case OR -> {
				read();
				System.out.println("D' -> or C D'");
				parseConjunctive();
				parseDisjunctive();
			}
			case ASSIGN,SEMICOLON,COLON,RB,COMMA,RSB,WHERE,THEN,DO -> System.out.println("D' ->  .");
			default -> error(currentSymbol, "arithmetic expression");
		}
	}

	private void parseConjunctive(){
		switch (peek()){
			case ID, LB, EXP, CONST_CHAR, CONST_INT, CONST_POINT, CONST_VOID, LCB, PLUS, MINUS, NOT -> {
				System.out.println("C -> R C'");
				parseRelational();
				parseConjunctive_();
			}
			default -> error(currentSymbol, "arithmetic expression");
		}
	}

	private void parseConjunctive_(){
		switch (peek()){
			case AND -> {
				read();
				System.out.println("C' -> and R C'");
				parseRelational();
				parseConjunctive_();
			}
			case ASSIGN,SEMICOLON,COLON,RB,COMMA,RSB,WHERE,THEN,DO,OR -> System.out.println("C' ->  .");
			default -> error(currentSymbol, "arithmetic expression");
		}
	}

	private void parseRelational(){
		switch (peek()){
			case ID, LB, EXP, CONST_CHAR, CONST_INT, CONST_POINT, CONST_VOID, LCB, PLUS, MINUS, NOT -> {
				System.out.println("R -> A R'");
				parseAdditive();
				parseRelational_();
			}
			default -> error(currentSymbol, "arithmetic expression");
		}
	}

	private void parseRelational_(){
		switch (peek()){
			case EQUAL,NOTEQUAL,GREATER,LESS,GREATEREQUAL,LESSEQUAL -> parseRelational_(currentSymbol.lexeme);
			case ASSIGN,SEMICOLON,COLON,RB,COMMA,RSB,WHERE,THEN,DO,OR,AND -> System.out.println("R' ->  .");
			default -> error(currentSymbol, "arithmetic expression");
		}
	}

	private void parseRelational_(String sign){
		read();
		System.out.println("R' -> "+sign+" A R'");
		parseAdditive();
		parseRelational_();
	}

	private void parseAdditive(){
		switch (peek()){
			case ID, LB, EXP, CONST_CHAR, CONST_INT, CONST_POINT, CONST_VOID, LCB, PLUS, MINUS, NOT -> {
				System.out.println("A -> M A'");
				parseMultiplicative();
				parseAdditive_();
			}
			default -> error(currentSymbol, "arithmetic expression");
		}
	}

	private void parseAdditive_(){
		switch (peek()){
			case PLUS -> {
				read();
				System.out.println("A' -> + M A'");
				parseMultiplicative();
				parseAdditive_();
			}
			case MINUS -> {
				read();
				System.out.println("A' -> - M A'");
				parseMultiplicative();
				parseAdditive_();
			}
			case ASSIGN,SEMICOLON,COLON,RB,COMMA,RSB,WHERE,THEN,DO,OR,AND,EQUAL,NOTEQUAL,GREATER,LESS,GREATEREQUAL,LESSEQUAL -> System.out.println("A' ->  .");
			default -> error(currentSymbol, "arithmetic expression");
		}
	}

	private void parseMultiplicative(){
		switch (peek()){
			case ID, LB, EXP, CONST_CHAR, CONST_INT, CONST_POINT, CONST_VOID, LCB, PLUS, MINUS, NOT -> {
				System.out.println("M -> Pr M'");
				parsePrefix();
				parseMultiplicative_();
			}
			default -> error(currentSymbol, "arithmetic expression");
		}
	}

	private void parseMultiplicative_(){
		switch (peek()){
			case MUL -> {
				read();
				System.out.println("M' -> * Pr M'");
				parsePrefix();
				parseMultiplicative_();
			}
			case DIV -> {
				read();
				System.out.println("M' -> / Pr M'");
				parsePrefix();
				parseMultiplicative_();
			}
			case MOD -> {
				read();
				System.out.println("M' -> % Pr M'");
				parsePrefix();
				parseMultiplicative_();
			}
			case ASSIGN,SEMICOLON,COLON,RB,COMMA,RSB,WHERE,THEN,DO,OR,AND,EQUAL,NOTEQUAL,GREATER,LESS,GREATEREQUAL,LESSEQUAL,MINUS,PLUS -> System.out.println("M' ->  .");
			default -> error(currentSymbol, "arithmetic expression");
		}
	}

	private void parsePrefix(){
		switch (peek()){
			case ID, LB, CONST_CHAR, CONST_INT, CONST_POINT, CONST_VOID, LCB -> {
				System.out.println("Pr -> Po");
				parsePostfix();
			}
			case EXP -> {
				read();
				System.out.println("Pr -> ^Po");
				parsePostfix();
			}
			case NOT -> {
				read();
				System.out.println("Pr -> !Po");
				parsePostfix();
			}
			case PLUS -> {
				read();
				System.out.println("Pr -> +Po");
				parsePostfix();
			}
			case MINUS -> {
				read();
				System.out.println("Pr -> -Po");
				parsePostfix();
			}
			default -> error(currentSymbol, "arithmetic expression");
		}
	}

	private void parsePostfix(){
		switch (peek()){
			case ID, LB, CONST_CHAR, CONST_INT, CONST_POINT, CONST_VOID, LCB  -> {
				System.out.println("Po -> expr' Po'");
				parseEXPR_();
				parsePostfix_();
			}
			default -> error(currentSymbol, "arithmet expression");
		}
	}

	private void parsePostfix_(){
		if (expectedCharacter != null){
			expectedErrorMode = true;
		}
		read();
		switch (peek()){
			case LSB -> {
				expectedErrorMode = false;
				System.out.println("Po' -> [ expr ] Po'");
				parseEXPR();
				confirm(Token.RSB);
				parsePostfix_();
			}
			case EXP -> {
				expectedErrorMode = false;
				System.out.println("Po' -> ^ Po'");
				parsePostfix_();
			}
			case ASSIGN,SEMICOLON,COLON,RB,COMMA,RSB,WHERE,THEN,DO,OR,AND,EQUAL,NOTEQUAL,GREATER,LESS,GREATEREQUAL,LESSEQUAL,MINUS,PLUS,DIV,MUL,MOD -> {
				expectedErrorMode = false;
				System.out.println("Po' ->  .");
			}
			default -> error(currentSymbol, "arithmetic expression");
		}
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
