package de.siphalor.spiceoffabric.util.queue;

import it.unimi.dsi.fastutil.ints.IntIterable;

import java.util.NoSuchElementException;

public interface FixedLengthIntFIFOQueue extends IntIterable {
	/**
	 * Returns the actual size of the queue that is occupied.
	 *
	 * @return the size
	 */
	int size();

	/**
	 * Clears the queue.
	 */
	void clear();

	/**
	 * Returns whether the queue is empty.
	 *
	 * @return whether the queue is empty
	 */
	default boolean isEmpty() {
		return size() <= 0;
	}

	/**
	 * Returns whether the queue is completely filled.
	 *
	 * @return whether there is no more space left
	 */
	default boolean isFull() {
		return size() == getLength();
	}

	/**
	 * Gets the value at the given index.
	 * @param index the index
	 * @return the value
	 * @throws IndexOutOfBoundsException if the index is out of bounds
	 */
	int get(int index);

	/**
	 * Enqueues the given value, if there is enough space left.
	 *
	 * @param x the value to enqueue
	 * @throws IllegalStateException if the queue is already completely filled.
	 * @see #forceEnqueue(int)
	 */
	void enqueue(int x);

	/**
	 * Enqueues the given value. If the queue is already full, old values will be overwritten.
	 *
	 * @param x the value to enqueue
	 * @return the value that had to be overwritten, or {@code null} if no value was overwritten.
	 */
	Integer forceEnqueue(int x);

	/**
	 * Dequeues the first element, alias the oldest element.
	 *
	 * @return the first element
	 */
	int dequeue();

	/**
	 * Gets the first element, alias the oldest element.
	 *
	 * @return the first element
	 * @throws NoSuchElementException if the queue is empty
	 */
	default int first() {
		if (isEmpty()) {
			throw new NoSuchElementException();
		}
		return get(0);
	}

	/**
	 * Returns the length, alias the max size of this queue.
	 *
	 * @return the length
	 */
	int getLength();

	void setLength(int newLength);
}
