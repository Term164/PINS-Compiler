package pins;

import java.util.*;
import pins.common.report.*;
import pins.phase.lexan.*;

/**
 * The PINS'22 compiler.
 */
public class Compiler {

	/** All phases of the compiler. */
	private static final String phases = "none|lexan";

	/** Values of command line arguments. */
	private static HashMap<String, String> cmdLine = new HashMap<String, String>();

	public static void main(String[] args) {
		try {
			Report.info("This is the PINS'22 compiler:");

			// Scan the command line.
			for (int argc = 0; argc < args.length; argc++) {
				if (args[argc].startsWith("--")) {
					// Command-line switch.
					if (args[argc].matches("--src-file-name=.*")) {
						if (cmdLine.get("--src-file-name") == null) {
							cmdLine.put("--src-file-name", args[argc]);
							continue;
						}
					}
					if (args[argc].matches("--dst-file-name=.*")) {
						if (cmdLine.get("--dst-file-name") == null) {
							cmdLine.put("--dst-file-name", args[argc]);
							continue;
						}
					}
					if (args[argc].matches("--target-phase=(" + phases + "|all)")) {
						if (cmdLine.get("--target-phase") == null) {
							cmdLine.put("--target-phase", args[argc].replaceFirst("^[^=]*=", ""));
							continue;
						}
					}
					if (args[argc].matches("--logged-phase=(" + phases + "|all)")) {
						if (cmdLine.get("--logged-phase") == null) {
							cmdLine.put("--logged-phase", args[argc].replaceFirst("^[^=]*=", ""));
							continue;
						}
					}
					Report.warning("Command line argument '" + args[argc] + "' ignored.");
				} else {
					// Source file name.
					if (cmdLine.get("--src-file-name") == null) {
						cmdLine.put("--src-file-name", args[argc]);
					} else {
						Report.warning("Source file '" + args[argc] + "' ignored.");
					}
				}
			}
			if (cmdLine.get("--src-file-name") == null) {
				throw new Report.Error("Source file not specified.");
			}
			if (cmdLine.get("--dst-file-name") == null) {
				cmdLine.put("--dst-file-name", cmdLine.get("--src-file-name").replaceFirst("\\.[^./]*$", "") + ".mms");
			}
			if ((cmdLine.get("--target-phase") == null) || (cmdLine.get("--target-phase").equals("all"))) {
				cmdLine.put("--target-phase", phases.replaceFirst("^.*\\|", ""));
			}

			// The compilation process carried out phase by phase.
			while (true) {

				// Lexical analysis.
				if (cmdLine.get("--target-phase").equals("lexan")) {
					try (LexAn lexan = new LexAn(cmdLine.get("--src-file-name"))) {
						pins.data.symbol.Symbol symbol;
						do {
							symbol = lexan.lexer();
							symbol.log("");
						} while (symbol.token != pins.data.symbol.Token.EOF);
					}
					break;
				}
			}

			Report.info("Done.");
		} catch (

		Report.Error __) {
			System.exit(1);
		}
	}

}
