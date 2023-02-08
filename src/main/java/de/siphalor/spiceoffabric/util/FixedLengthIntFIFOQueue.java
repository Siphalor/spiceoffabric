package de.siphalor.spiceoffabric.util;


import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;

public class FixedLengthIntFIFOQueue implements IntPriorityQueue, IntIterable {
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

	@Override
	public void clear() {
		size = 0;
	}

	public boolean isEmpty() {
		return size <= 0;
	}

	@Override
	public void enqueue(int x) {
		if (array.length == 0) {
			return;
		}
		if (size == array.length) {
			array[start] = x;
			if (++start >= size) {
				start = 0;
			}
		} else {
			array[(start + size++) % array.length] = x;
		}
	}

	@Override
	public int dequeueInt() {
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

	@Override
	public int firstInt() {
		if (size <= 0) {
			throw new NoSuchElementException();
		}
		return array[start];
	}

	@Override
	public IntComparator comparator() {
		return null;
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

	public int uniqueElements() {
		if (size < 250) {
			// O(n^2), but great locality
			int unique = size;
			for (int current = start + 1; current < start + size; ++current) {
				for (int past = start; past < current; ++past) {
					if (array[current % array.length] == array[past % array.length]) {
						--unique;
						break;
					}
				}
			}
			return unique;
		} else {
			HashSet<Integer> all = new HashSet<Integer>();
			for (int current = start; current < start + size; ++current) {
				all.add(array[current]);
			}
			return all.size();
		}
	}
}
