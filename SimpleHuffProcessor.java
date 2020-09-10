
/*  Student information for assignment:
 *
 *  On OUR honor, Tiago Grimaldi Rossi and Caesar Gutierrez, this programming assignment is OUR own work
 *  and WE have not provided this code to any other student.
 *
 *  Number of slip days used: 1
 *
 *  Student 1: Tiago Grimaldi Rossi
 *  UTEID: tg24645
 *  email address: tiagogrimaldirossi@gmail.com
 *  Grader name: Terrel
 *  Section number: 50250
 *
 *  Student 2: Caesar Gutierrez
 *  UTEID: ckg499
 *  email address: karim-100gtz@utexas.edu
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class SimpleHuffProcessor implements IHuffProcessor {

	// instance vars
	private IHuffViewer myViewer;
	private HuffTree huffTree;
	private int compressedBits;
	private int headerFormat;
	private int savedBits;
	private HashMap<Integer, String> map;
	private int[] myCounts; // frequencies of values

	/**
	 * Preprocess data so that compression is possible --- count characters/create
	 * tree/store state so that a subsequent call to compress will work. The
	 * InputStream is <em>not</em> a BitInputStream, so wrap it in one as needed.
	 * 
	 * @param in           is the stream which could be subsequently compressed
	 * @param headerFormat a constant from IHuffProcessor that determines what kind
	 *                     of header to use, standard count format, standard tree
	 *                     format, or possibly some format added in the future.
	 * @return number of bits saved by compression or some other measure Note, to
	 *         determine the number of bits saved, the number of bits written
	 *         includes ALL bits that will be written including the magic number,
	 *         the header format number, the header to reproduce the tree, AND the
	 *         actual data.
	 * @throws IOException if an error occurs while reading from the input file.
	 */
	public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
		myCounts = new int[ALPH_SIZE + 1]; // position in list represents ASCII value. PEOF goes in the last slot
		BitInputStream inbit = new BitInputStream(in);
		int read = 0; // counter to store # of bits read off file
		this.headerFormat = headerFormat;

		// get the characters and update their frequencies in myCounts
		boolean done = false;
		while (!done) {
			int bit = inbit.readBits(BITS_PER_WORD);
			if (bit == -1) {
				done = true;
			} else {
				read += BITS_PER_WORD;
				myCounts[bit]++;
			}
		}
		// get counts and then create the tree
		myCounts[ALPH_SIZE] = 1; // add PEOF value
		huffTree = new HuffTree(myCounts);

		// create the map
		map = new HashMap<>();
		huffTree.createMap(map);
		// get amount of bits of the file if it is compressed
		compressedBits = compressedBits(inbit);
		savedBits = read - compressedBits;
		return savedBits;
	}

	// Method is called by preprocess, it counts the total amount of bits that will
	// be compressed depending on whether it's a tree format or count format.
	private int compressedBits(BitInputStream inbit) throws IOException {

		int result = BITS_PER_INT * 2; // magic # and format code are each 32 bits
		if (headerFormat == STORE_COUNTS) { // Standard Count Format
			// SCF header always has a header that is 256 * 32 bits long
			// one 32-bit int value for each 8-bit chunk, from 0-255
			result += BITS_PER_INT * ALPH_SIZE;
			
		} else if (headerFormat == STORE_TREE) { // Standard Tree Format
			// STF header always start with a 32-bit int that represents size
			result += BITS_PER_INT;
			result += huffTree.countBits();
		}
		for (int key : map.keySet()) {
			// increment result by frequency of each ASCII value times its code length
			result += myCounts[key] * map.get(key).length();
		}

		return result;
	}

	/**
	 * Compresses input to output, where the same InputStream has previously been
	 * pre-processed via <code>preprocessCompress</code> storing state used by this
	 * call. <br>
	 * pre: <code>preprocessCompress</code> must be called before this method
	 * 
	 * @param in    is the stream being compressed (NOT a BitInputStream)
	 * @param out   is bound to a file/stream to which bits are written for the
	 *              compressed file (not a BitOutputStream)
	 * @param force if this is true create the output file even if it is larger than
	 *              the input file. If this is false do not create the output file
	 *              if it is larger than the input file.
	 * @return the number of bits written.
	 * @throws IOException if an error occurs while reading from the input file or
	 *                     writing to the output file.
	 */
	public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
		BitInputStream inbit = new BitInputStream(in);
		BitOutputStream outbit = new BitOutputStream(out);

		// check if forced compression is selected 
		if (savedBits < 0 && force == false) {
			myViewer.showError("Compressed file has " + savedBits * -1 + " more bits than the "
					+ "uncompressed file. \nSelect \"force compression\" to compress");
			inbit.close();
			outbit.close();
			return 0;
			
		} else { // compression can proceed 
			outbit.writeBits(BITS_PER_INT, MAGIC_NUMBER); // write magic number
			outbit.writeBits(BITS_PER_INT, headerFormat); // write header format 
			if (headerFormat == STORE_COUNTS) { // Standard Count Format
				// write header
				for (int i = 0; i < ALPH_SIZE; i++) {
					outbit.writeBits(BITS_PER_INT, myCounts[i]);
				}
				// write compressed data 
				writeCompressedData(inbit, outbit);
				
			} else if (headerFormat == STORE_TREE) { // Standard Tree Format
				// write header
				int size = huffTree.countBits();
				outbit.writeBits(BITS_PER_INT, size);
				// call writeTree to write the information in the tree into the file 
				huffTree.writeTree(outbit);
				// write compressed data
				writeCompressedData(inbit, outbit);
			}
			inbit.close();
			outbit.close();
			return compressedBits;
		}
	}

	// read raw data from inbit and write out compressed data
	// called by compress
	private void writeCompressedData(BitInputStream inbit, BitOutputStream outbit) throws IOException {
		boolean done = false;
		while (!done) {
			int bit = inbit.readBits(BITS_PER_WORD);
			if (bit == -1) {
				// done, write PEOF
				String PEOF = map.get(ALPH_SIZE);
				writeCode(PEOF, outbit);
				done = true;
			} else { // continue going through the bits and write their directions to the file 
				// write new codes for each ASCII value read from inbit
				String code = map.get(bit);
				writeCode(code, outbit);
			}
		}
	}

	// write bit per bit of code for an ASCII value
	private void writeCode(String code, BitOutputStream outbit) {
		for (int i = 0; i < code.length(); i++) {
			if (code.charAt(i) == '0') {
				outbit.writeBits(1, 0);
			} else {
				outbit.writeBits(1, 1);
			}
		}
	}

	/**
	 * Uncompress a previously compressed stream in, writing the uncompressed
	 * bits/data to out.
	 * 
	 * @param in  is the previously compressed data (not a BitInputStream)
	 * @param out is the uncompressed file/stream
	 * @return the number of bits written to the uncompressed file/stream
	 * @throws IOException if an error occurs while reading from the input file or
	 *                     writing to the output file.
	 */
	public int uncompress(InputStream in, OutputStream out) throws IOException {

		BitInputStream inbit = new BitInputStream(in);
		BitOutputStream outbit = new BitOutputStream(out);

		// check for magic number 
		int magic_num = inbit.readBits(BITS_PER_INT);
		if (magic_num != MAGIC_NUMBER) {
			inbit.close();
			outbit.close();
			throw new IOException("ERROR reading compressed file. \n" + "File did not start with huff magic number.");
		}
		// get format of the file 
		int format = inbit.readBits(BITS_PER_INT);

		if (format == STORE_COUNTS) {
			// file is in standard count format
			// make huffTree
			huffTree = new HuffTree(createCounts(inbit));
			return huffTree.decomp(inbit, outbit);

		} else if (format == STORE_TREE) {
			// file is in standard tree format
			inbit.readBits(BITS_PER_INT); // read size of tree
			// make huffTree
			huffTree = new HuffTree(inbit);
			return huffTree.decomp(inbit, outbit);
		}

		outbit.close();
		inbit.close();
		return -1;
	}

	// get the frequencies from the file. Used for SCF.
	private int[] createCounts(BitInputStream bitInput) {
		int[] counts = new int[ALPH_SIZE + 1];
		try {
			for (int k = 0; k < ALPH_SIZE; k++) {
				// Get the count
				int bits = bitInput.readBits(BITS_PER_INT);
				// If char does have a val add it to the map.
				counts[k] = bits;
			}
			// add the frequency of our PEOF
			counts[ALPH_SIZE] = 1;
			return counts;
		} catch (IOException e) {
			myViewer.showError("Incorrect amount of bits for alphabet.");
			return counts;
		}
	}

	public void setViewer(IHuffViewer viewer) {
		myViewer = viewer;
	}

	private void showString(String s) {
		if (myViewer != null)
			myViewer.update(s);
	}
}
