package pins.phase.lexan;

import java.io.*;
import pins.common.report.*;
import pins.data.symbol.*;

/**
 * Lexical analyzer.
 */
public class LexAn implements AutoCloseable {

	private String srcFileName;

	private FileReader srcFile;

	public LexAn(String srcFileName) {
		this.srcFileName = srcFileName;
		try {
			srcFile = new FileReader(new File(srcFileName));
		} catch (FileNotFoundException __) {
			throw new Report.Error("Cannot open source file '" + srcFileName + "'.");
		}
	}

	public void close() {
		try {
			srcFile.close();
		} catch (IOException __) {
			throw new Report.Error("Cannot close source file '" + srcFileName + "'.");
		}
	}

	private int column = 0;
	private int line = 1;
	private int commentStackCounter = 0;
	private int previouslyRead = 0;

	public Symbol lexer() {

		int current;
		if (previouslyRead != 0) current = previouslyRead;
		else current = readNext();

		if (current == '#') current = readComment(current);
		if (current >= '0' && current <= '9') return constructDigitSymbol(current);
		if (current >= 'A' && current <= 'Z' || current >= 'a' && current <= 'z' || current == '_') return constructWordSymbol(current);

		previouslyRead = 0; // Clear last read char from memory
		switch (current){
			case '\n':
				line++;
				column = 0;
				return lexer();
			case '\r': return lexer();
			case '\t':
				column += 8 - (column % 8);
				return lexer();
			case 32: return lexer();
			case '(': return new Symbol(Token.LB,"(",new Location(line, column));
			case  ')': return new Symbol(Token.RB,")",new Location(line, column));
			case  '{': return new Symbol(Token.LCB,"{",new Location(line, column));
			case  '}': return new Symbol(Token.RCB,"}",new Location(line, column));
			case  '[': return new Symbol(Token.LSB,"[",new Location(line, column));
			case  ']': return new Symbol(Token.RSB,"]",new Location(line, column));
			case  ',': return new Symbol(Token.COMMA,",",new Location(line, column));
			case  ':': return new Symbol(Token.COLON,":",new Location(line, column));
			case  ';': return new Symbol(Token.SEMICOLON,";",new Location(line, column));
			case  '&': return new Symbol(Token.AND,"&",new Location(line, column));
			case  '|': return new Symbol(Token.OR,"|",new Location(line, column));
			case  '*': return new Symbol(Token.MUL,"*",new Location(line, column));
			case  '/': return new Symbol(Token.DIV,"/",new Location(line, column));
			case  '%': return new Symbol(Token.MOD,"%",new Location(line, column));
			case  '^': return new Symbol(Token.EXP,"^",new Location(line, column));
			case  '+': return new Symbol(Token.PLUS,"+",new Location(line, column));
			case  '-': return new Symbol(Token.MINUS,"-",new Location(line, column));
			case  '\'': return readCharacterSymbol(current);
			// Can be != <= >= ==
			case  '!':
			case  '<':
			case  '>':
			case  '=': return checkSymbol(current);

			case -1:
				if (commentStackCounter > 0) Report.warning("Open comment at end of file");
				return new Symbol(Token.EOF,"",null);
		}

		throw new Report.Error(new Location(line, column), "Illegal character " + (char) current);
	}

	// ================================ DIGIT CHECK ===============================
	private Symbol constructDigitSymbol (int current){
		String digit = ""+(char)current;
		while (true){
			current = readNext();
			if (current >= '0' && current <= '9'){
				digit += (char) current;
			}else {
				return new Symbol(Token.CONST_INT, digit, new Location(line, column-digit.length(), line, column-1));
			}
		}
	}

	// ============================== WORD CHECK =====================================
	private Symbol constructWordSymbol(int current){
		String word = ""+(char) current;
		while (true){
			current = readNext();
			if (current >= 'A' && current <= 'Z' || current >= 'a' && current <= 'z' || current == '_' || current >= '0' && current <= '9'){
				word += (char) current;
			}else {
				switch (word){
					case "char": return new Symbol(Token.CHAR, "char", new Location(line, column-word.length(), line, column-1));
					case "del": return new Symbol(Token.DEL, "del", new Location(line, column-word.length(), line, column-1));
					case "do": return new Symbol(Token.DO, "do", new Location(line, column-word.length(), line, column-1));
					case "else": return new Symbol(Token.ELSE, "else", new Location(line, column-word.length(), line, column-1));
					case "end": return new Symbol(Token.END, "end", new Location(line, column-word.length(), line, column-1));
					case "fun": return new Symbol(Token.FUN, "fun", new Location(line, column-word.length(), line, column-1));
					case "if": return new Symbol(Token.IF, "if", new Location(line, column-word.length(), line, column-1));
					case "int": return new Symbol(Token.INT, "int", new Location(line, column-word.length(), line, column-1));
					case "new": return new Symbol(Token.NEW, "new", new Location(line, column-word.length(), line, column-1));
					case "then": return new Symbol(Token.THEN, "then", new Location(line, column-word.length(), line, column-1));
					case "typ": return new Symbol(Token.TYP, "typ", new Location(line, column-word.length(), line, column-1));
					case "var": return new Symbol(Token.VAR, "var", new Location(line, column-word.length(), line, column-1));
					case "void": return new Symbol(Token.VOID, "void", new Location(line, column-word.length(), line, column-1));
					case "where": return new Symbol(Token.WHERE, "where", new Location(line, column-word.length(), line, column-1));
					case "while": return new Symbol(Token.WHILE, "while", new Location(line, column-word.length(), line, column-1));
					case "none": return new Symbol(Token.CONST_VOID, "none", new Location(line, column-word.length(), line, column-1));
					case "nil": return new Symbol(Token.CONST_POINT, "nil", new Location(line, column-word.length(), line, column-1));

					default: return new Symbol(Token.ID, word, new Location(line, column-word.length(), line, column-1));
				}
			}
		}
	}

	private Symbol readCharacterSymbol(int current){
		String character = ""+(char) current;
		int next = readNext();
		if (next == '\\'){
			character += (char) next;
			next = readNext();
			if (next == '\\' || next == '\'') character += (char) next;
			else throw new Report.Error(new Location(line, column), "Illegal character after \\ in char declaration");
		}else if(next >= 32 && next <= 126){
			character += (char) next;
		}else throw new Report.Error(new Location(line, column), "Illegal character '" + (char) next + "' in char declaration");

		next = readNext();
		if (next == '\''){
			character+=(char)next;
			previouslyRead = 0; // Clear the last char read memory
			return new Symbol(Token.CONST_CHAR, character, new Location(line, column-character.length(), line, column-1));
		}else throw new Report.Error(new Location(line, column), "Missing ' in char");
	}

	// ===================================== Comment logic ====================================
	private int readComment(int current){
		if (isLegalComment(current)){
			while (commentStackCounter > 0){
				current = readNext();
				if (current == '#' || current == '}') isLegalComment(current);
				else if (current == 10) line++;
				else if (current == -1) return current;
			}
			return readNext();
		}else throw new Report.Error(new Location(line, column), "Illegal comment declaration");
	}

	private boolean isLegalComment(int current){
		int next = readNext();
		if (current == '#' && next == '{'){
			commentStackCounter++;
			return true;
		}else if(current == '}' && next == '#'){
			commentStackCounter--;
			return true;
		} else if(current == '#' && commentStackCounter == 0) throw new Report.Error(new Location(line, column), "Illegal character #");
		else return false;
	}

	private Symbol checkSymbol(int current){
		char nextChar = (char) readNext();
		if (nextChar == '='){
			previouslyRead = 0; // Clear the last char read memory
			switch (current){
				case '!': return new Symbol(Token.NOTEQUAL,"!=",new Location(line, column-1, line,column));
				case '=': return new Symbol(Token.EQUAL,"==",new Location(line, column-1, line,column));
				case '<': return new Symbol(Token.LESSEQUAL,"<=",new Location(line, column-1, line,column));
				case '>': return new Symbol(Token.GREATEREQUAL,">=",new Location(line, column-1, line,column));
			}
		}else {
			switch (current){
				case '!': return new Symbol(Token.NOT,"!",new Location(line, column-1));
				case '=': return new Symbol(Token.ASSIGN,"=",new Location(line, column-1));
				case '<': return new Symbol(Token.LESS,"<",new Location(line, column-1));
				case '>': return new Symbol(Token.GREATER,">",new Location(line, column-1));
			}
		}
		throw new Report.InternalError();
	}

	private int readNext(){
		try {
			int current = srcFile.read();
			column++;
			previouslyRead = current;
			return current;
		}catch (IOException e){
			e.printStackTrace();
		}
		return -2;
	}

}