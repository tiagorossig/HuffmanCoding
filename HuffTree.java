
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
import java.util.HashMap;

public class HuffTree {

	// instance var
	private TreeNode root;

	// constructor used for STF files
	public HuffTree(BitInputStream inbit) throws IOException {
		root = builderTreeFormat(inbit);
	}

	// constructor used for SCF files
	public HuffTree(int[] freq) {
		root = null;
		builder(freq);
	}

	// builds the tree based on the frequencies given
	public void builder(int[] freq) {
		if (freq == null) {
			throw new IllegalArgumentException("param cannot be null");
		}
		// create priority queue of the frequencies
		PriorityQueue<TreeNode> q = new PriorityQueue<>();
		// Go through all the characters.
		for (int chr = 0; chr < freq.length; chr++) {
			// if frequency of chr is > 0 enqueue it.
			if (freq[chr] > 0) {
				q.add(new TreeNode(chr, freq[chr]));
			}
		}
		TreeNode t1;
		TreeNode t2;
		// while queue has 2 nodes or more, dequeue two nodes and add a new parent node
		// that references to these two dequeue'd nodes
		while (q.size() > 1) {
			t1 = q.remove();
			t2 = q.remove();
			q.add(new TreeNode(t1, 0, t2));
		}
		// only 1 node left in queue, dequeue it and set root equal to it
		root = q.remove();
	}

	// builds tree based on the BitInputStream given
	public TreeNode builderTreeFormat(BitInputStream inbit) throws IOException {
		int bit = inbit.readBits(1);
		if (bit == 1) { // bit == 1 indicates a leaf node, create and return a new node
			bit = inbit.readBits(IHuffConstants.BITS_PER_WORD + 1);
			return new TreeNode(bit, 0);
		} else { // all internal nodes have two children, make two recursive calls
			TreeNode temp = new TreeNode(0, 0);
			temp.setLeft(builderTreeFormat(inbit));
			temp.setRight(builderTreeFormat(inbit));
			return temp;
		}
	}

	// count size of a tree in bits
	public int countBits() {
		return countBitsHelper(root);
	}

	// helper for countBits
	public int countBitsHelper(TreeNode n) {
		if (n.isLeaf()) { // base case
			return 10;
		} else { // internal node, add 1 and navigate rest of tree
			return 1 + countBitsHelper(n.getRight()) + countBitsHelper(n.getLeft());
		}
	}

	// preorder traverse the huffTree and write out STF header
	public void writeTree(BitOutputStream outbit) {
		writeTreeHelper(root, outbit);
	}

	// helper for writeTree
	private void writeTreeHelper(TreeNode n, BitOutputStream outbit) {
		if (n.isLeaf()) { // base case, write 1 followed by 9 bits that hold the value of the leaf
			outbit.writeBits(1, 1);
			outbit.writeBits(IHuffConstants.BITS_PER_WORD + 1, n.getValue());
			return;
		} else { // internal node, just write a 0 and navigate the tree
			outbit.writeBits(1, 0);
			writeTreeHelper(n.getLeft(), outbit);
			writeTreeHelper(n.getRight(), outbit);
		}
	}

	// method that does most of the heavy work in decompression
	public int decomp(BitInputStream inbit, BitOutputStream outbit) throws IOException {
		int bitsWritten = 0;
		TreeNode currentNode = root;
		boolean done = false;
		while (!done) {
			int dir = inbit.readBits(1);
			if (dir == -1) { // PEOF not found
				throw new IOException(
						"Error reading compressed file. \n" + "unexpected end of input. No PSEUDO_EOF value.");
			} else {
				if (dir == 1) { // bit is 1, go right
					currentNode = currentNode.getRight();
				} else { // bit is 0, go left
					currentNode = currentNode.getLeft();
				}
				if (currentNode.isLeaf()) {
					if (currentNode.getValue() == IHuffConstants.PSEUDO_EOF) { // PEOF found, we are done
						done = true;
					} else { // not PEOF, write value and return to root
						outbit.writeBits(IHuffConstants.BITS_PER_WORD, currentNode.getValue());
						bitsWritten += IHuffConstants.BITS_PER_WORD;
						currentNode = root;
					}
				}
			}
		}
		return bitsWritten;
	}

	// method to create mapping of each value in a tree
	public void createMap(HashMap<Integer, String> map) {
		createMapHelper(root, "", map);
	}

	// helper for createMap
	private void createMapHelper(TreeNode n, String currPattern, HashMap<Integer, String> map) {
		if (n.isLeaf()) { // base case, add current pattern to the map
			map.put(n.getValue(), currPattern);
		} else { // internal node, go left and append 0 and go right and append 1
			createMapHelper(n.getLeft(), currPattern + "0", map);
			createMapHelper(n.getRight(), currPattern + "1", map);
		}
	}
}
