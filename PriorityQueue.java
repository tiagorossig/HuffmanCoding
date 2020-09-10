
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

import java.util.LinkedList;

public class PriorityQueue<E extends Comparable<E>> {

	// internal storage is a LinkedList
	private LinkedList<E> queue;

	// default constructor
	public PriorityQueue() {
		queue = new LinkedList<>();
	}

	// if list is empty, return true. Return false otherwise
	public boolean isEmpty() {
		return queue.size() == 0;
	}

	// add item to this queue according to its priority
	// if there are other items of the same priority, add in a fair way
	// pre: item != null
	public void add(E item) {
		if (item == null)
			throw new IllegalArgumentException("item must not be null");

		int index = 0;
		for (E element : queue) {
			int prio = item.compareTo(element);
			if (prio < 0) { // found where to insert item. We are done
				queue.add(index, item);
				return;
			}
			index++;
		}
		queue.addLast(item); // item has lowest priority
	}

	// remove and return the first element of this queue
	public E remove() {
		return queue.removeFirst();
	}

	// return the first element of this queue
	public E peek() {
		return queue.peek();
	}

	// return size of queue
	public int size() {
		return queue.size();
	}
}