package de.siphalor.spiceoffabric.util;


import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;
import java.util.function.IntConsumer;

public class FixedLengthIntFIFOQueue implements IntIterable {
	protected int[] array;
	protected int size; // actual length, != array.length
	protected int start; // inclusive

	public FixedLengthIntFIFOQueue(int size) {
		if (size < 0) {
			throw new IllegalArgumentException("A fixed length fifo queue must not have a negative length!");
		}
		array = new int[size];
	}

	/**
	 * Returns the actual size of the queue that is occupied.
	 * @return the size
	 */
	public int size() {
		return size;
	}

	/**
	 * Clears the queue.
	 */
	public void clear() {
		size = 0;
	}

	/**
	 * Returns whether the queue is empty.
	 * @return whether the queue is empty
	 */
	public boolean isEmpty() {
		return size <= 0;
	}

	/**
	 * Returns whether the queue is completely filled.
	 * @return whether there is no more space left
	 */
	public boolean isFull() {
		return size() == getLength();
	}

	public int get(int index) {
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException(index + " is out of bounds for fixed FIFO queue (s: " + size + ", l: " + array.length + ")");
		}
		index += start;
		if (index >= array.length) {
			index -= array.length;
		}
		return array[index];
	}

	/**
	 * Enqueues the given value.
	 * @param x the value to enqueue
	 * @throws IllegalStateException if the queue is already completely filled.
	 * @see #forceEnqueue(int)
	 */
	public void enqueue(int x) {
		if (getLength() == 0) {
			return;
		}
		if (size == getLength()) {
			// An override would happen!
			throw new IllegalStateException("Tried to enqueue more elements than length permits onto fixed FIFO queue");
		} else {
			array[(start + size++) % array.length] = x;
		}
	}

	/**
	 * Enqueues the given value. If the queue is already full, old values will be overwritten.
	 * @param x the value to enqueue
	 * @return the value that had to be overwritten, or {@code null} if no value was overwritten.
	 */
	public Integer forceEnqueue(int x) {
		if (getLength() == 0) {
			return null;
		}
		if (isFull()) {
			int overwrite = dequeue();
			enqueue(x);
			return overwrite;
		} else {
			enqueue(x);
			return null;
		}
	}

	/**
	 * Dequeues the first element, alias the oldest element.
	 * @return the first element
	 */
	public int dequeue() {
		if (size <= 0) {
			throw new NoSuchElementException();
		}
		size--;
		int x = array[start++];
		if (start >= array.length) {
			start = 0;
		}
		return x;
	}

	/**
	 * Gets the first element, alias the oldest element.
	 * @return the first element
	 */
	public int first() {
		if (size <= 0) {
			throw new NoSuchElementException();
		}
		return array[start];
	}

	@Override
	public @NotNull IntIterator iterator() {
		return new IntIterator() {
			private int offset;

			@Override
			public int nextInt() {
				if (offset >= size) {
					throw new NoSuchElementException();
				}
				return array[(start + offset++) % array.length];
			}

			@Override
			public boolean hasNext() {
				return offset < size;
			}
		};
	}

	@Override
	public void forEach(IntConsumer action) {
		int pos = start;
		int l = size;
		while (l > 0) {
			action.accept(array[pos++]);
			if (pos >= array.length) {
				pos = 0;
			}
			l--;
		}
	}

	/**
	 * Returns the length, alias the max size of this queue.
	 * @return the length
	 */
	public int getLength() {
		return array.length;
	}

	public void setLength(int newLength) {
		if (newLength < 0) {
			throw new IllegalArgumentException("A fixed length fifo queue must not have a negative length!");
		}

		if (newLength < array.length) {
			shortenTo(newLength);
		} else if (newLength > array.length) {
			extendTo(newLength);
		}
	}

	private void shortenTo(int newLength) {
		int overflow = size - newLength;
		if (overflow > 0) {
			// If the queue is overflowing, we need to remove the oldest elements
			start = (start + overflow) % array.length;
			size -= overflow;
		}

		changeSizeTo(newLength);
	}

	private void extendTo(int newLength) {
		changeSizeTo(newLength);
	}

	private void changeSizeTo(int newLength) {
		int[] newArray = new int[newLength];
		int spaceToEnd = array.length - start;
		if (size <= spaceToEnd) {
			System.arraycopy(array, start, newArray, 0, size);
		} else {
			System.arraycopy(array, start, newArray, 0, spaceToEnd);
			System.arraycopy(array, 0, newArray, spaceToEnd, size - spaceToEnd);
		}
		start = 0;
		array = newArray;
	}
}
