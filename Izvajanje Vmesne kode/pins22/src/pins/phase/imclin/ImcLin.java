package pins.phase.imclin;

import java.util.*;

import pins.data.lin.*;

/**
 * Linearization of intermediate code.
 */
public class ImcLin implements AutoCloseable {

	/** All data chunks of the program. */
	private final static Vector<LinDataChunk> dataChunks = new Vector<LinDataChunk>();

	/** All code chunks of the program. */
	private final static Vector<LinCodeChunk> codeChunks = new Vector<LinCodeChunk>();

	/**
	 * Constructs a new phase for the linearization of intermediate code.
	 */
	public ImcLin() {
	}

	public void close() {
	}

	/**
	 * Adds a data chunk to a collection of all data chunks of the program.
	 * 
	 * @param dataChunk A data chunk.
	 */
	public static void addDataChunk(LinDataChunk dataChunk) {
		dataChunks.add(dataChunk);
	}

	/**
	 * Returns a collection of all data chunks of the program.
	 * 
	 * @return A collection of all data chunks of the program.
	 */
	public static Vector<LinDataChunk> dataChunks() {
		return new Vector<LinDataChunk>(dataChunks);
	}

	/**
	 * Adds a code chunk to a collection of all code chunks of the program.
	 * 
	 * @param codeChunk A code chunk.
	 */
	public static void addCodeChunk(LinCodeChunk codeChunk) {
		codeChunks.add(codeChunk);
	}

	/**
	 * Returns a collection of all code chunks of the program.
	 * 
	 * @return A collection of all code chunks of the program.
	 */
	public static Vector<LinCodeChunk> codeChunks() {
		return new Vector<LinCodeChunk>(codeChunks);
	}

}
