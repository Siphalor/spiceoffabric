package de.siphalor.spiceoffabric.util;


import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;

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

	public int size() {
		return size;
	}

	public void clear() {
		size = 0;
	}

	public boolean isEmpty() {
		return size <= 0;
	}


	public boolean enqueue(int x) {
		if (array.length == 0) {
			return false;
		}
		if (size == array.length) {
			array[start] = x;
			if (++start >= size) {
				start = 0;
			}
			return true;
		} else {
			array[(start + size++) % array.length] = x;
			return false;
		}
	}

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

	public int first() {
		if (size <= 0) {
			throw new NoSuchElementException();
		}
		return array[start];
	}

	@Override
	public IntIterator iterator() {
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

	public int getLength() {
		return array.length;
	}

	public void setLength(int newLength) {
		if (newLength == size) {
			return;
		}
		int[] newArray = new int[newLength];
		int toEnd = array.length - start;
		if (newLength < size) {
			if (newLength > 0) {
				int diff = size - newLength;
				if (size <= toEnd) {
					System.arraycopy(array, start + diff, newArray, start, newLength);
				} else {
					System.arraycopy(array, start, newArray, Math.max(0, start - diff), toEnd);
					System.arraycopy(array, 0, newArray, 0, size - toEnd);
				}
			}
			size = Math.min(size, newLength);
		} else {
			if (array.length > 0) {
				if (size <= toEnd) {
					System.arraycopy(array, start, newArray, start, toEnd);
				} else {
					System.arraycopy(array, start, newArray, 0, toEnd);
					System.arraycopy(array, 0, newArray, toEnd, size - toEnd);
					start = 0;
				}
			}
		}
		array = newArray;
	}
}
