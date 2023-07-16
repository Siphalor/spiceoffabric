package de.siphalor.spiceoffabric.util;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

class FixedLengthIntFIFOQueueTest {
	@Test
	void testConstructor() {
		Assertions.assertDoesNotThrow(() -> new FixedLengthIntFIFOQueue(0));
		Assertions.assertDoesNotThrow(() -> new FixedLengthIntFIFOQueue(1));
		Assertions.assertDoesNotThrow(() -> new FixedLengthIntFIFOQueue(1000));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new FixedLengthIntFIFOQueue(-1));
	}

	@Test
	void testSize() {
		FixedLengthIntFIFOQueue queue = new FixedLengthIntFIFOQueue(2);
		Assertions.assertEquals(0, queue.size());
		queue.enqueue(1);
		Assertions.assertEquals(1, queue.size());
		queue.enqueue(1);
		Assertions.assertEquals(2, queue.size());
		queue.enqueue(1);
		Assertions.assertEquals(2, queue.size());
		queue.dequeue();
		Assertions.assertEquals(1, queue.size());
	}

	@Test
	void testClear() {
		FixedLengthIntFIFOQueue queue = new FixedLengthIntFIFOQueue(2);
		queue.enqueue(1);
		queue.clear();
		Assertions.assertEquals(0, queue.size());
		Assertions.assertFalse(queue.iterator().hasNext());
	}

	@Test
	void testIsEmpty() {
		FixedLengthIntFIFOQueue queue = new FixedLengthIntFIFOQueue(2);
		Assertions.assertTrue(queue.isEmpty());
		queue.enqueue(1);
		Assertions.assertFalse(queue.isEmpty());
		queue.enqueue(1);
		Assertions.assertFalse(queue.isEmpty());
		queue.dequeue();
		Assertions.assertFalse(queue.isEmpty());
		queue.dequeue();
		Assertions.assertTrue(queue.isEmpty());
	}

	@Test
	void testEnqueue() {
		FixedLengthIntFIFOQueue queue = new FixedLengthIntFIFOQueue(2);
		Assertions.assertFalse(queue.enqueue(1));
		Assertions.assertEquals(1, queue.first());
		Assertions.assertFalse(queue.enqueue(2));
		Assertions.assertEquals(1, queue.first());
		Assertions.assertTrue(queue.enqueue(3));
		Assertions.assertEquals(2, queue.first());
	}

	@Test
	void testDequeue() {
		FixedLengthIntFIFOQueue queue = new FixedLengthIntFIFOQueue(2);
		Assertions.assertThrows(NoSuchElementException.class, queue::dequeue);
		queue.enqueue(1);
		queue.dequeue();
		Assertions.assertEquals(0, queue.size());
		queue.enqueue(2);
		queue.enqueue(3);
		queue.enqueue(4);
		queue.dequeue();
		Assertions.assertEquals(1, queue.size());
		Assertions.assertEquals(4, queue.first());
		queue.dequeue();
		Assertions.assertEquals(0, queue.size());
		Assertions.assertThrows(NoSuchElementException.class, queue::dequeue);
	}

	@Test
	void testFirstInt() {
		FixedLengthIntFIFOQueue queue = new FixedLengthIntFIFOQueue(2);
		Assertions.assertThrows(NoSuchElementException.class, queue::first);
		queue.enqueue(1);
		Assertions.assertEquals(1, queue.first());
		queue.enqueue(2);
		Assertions.assertEquals(1, queue.first());
		queue.enqueue(3);
		Assertions.assertEquals(2, queue.first());
		queue.enqueue(4);
		Assertions.assertEquals(3, queue.first());
	}

	@Test
	void testIterator() {
		FixedLengthIntFIFOQueue queue = new FixedLengthIntFIFOQueue(2);
		queue.enqueue(1);
		queue.enqueue(2);
		queue.enqueue(3);

		IntIterator iterator = queue.iterator();
		Assertions.assertTrue(iterator.hasNext());
		Assertions.assertEquals(2, iterator.nextInt());
		Assertions.assertTrue(iterator.hasNext());
		Assertions.assertEquals(3, iterator.nextInt());
		Assertions.assertFalse(iterator.hasNext());
		Assertions.assertThrows(NoSuchElementException.class, iterator::nextInt);
	}

	@Test
	void testForEach() {
		FixedLengthIntFIFOQueue queue = new FixedLengthIntFIFOQueue(2);
		queue.enqueue(1);
		queue.enqueue(2);
		queue.enqueue(3);

		IntList data = new IntArrayList(2);
		queue.forEach(data::add);
		Assertions.assertArrayEquals(new int[]{2, 3}, data.toIntArray());
	}

	@Test
	void testSetLengthIncrease() {
		FixedLengthIntFIFOQueue queue = new FixedLengthIntFIFOQueue(3);
		queue.enqueue(1);
		queue.enqueue(2);
		queue.enqueue(3);
		testSetLengthIncreaseHelper(queue);

		queue = new FixedLengthIntFIFOQueue(3);
		queue.enqueue(0);
		queue.enqueue(1);
		queue.enqueue(2);
		queue.enqueue(3);
		testSetLengthIncreaseHelper(queue);
	}

	private void testSetLengthIncreaseHelper(FixedLengthIntFIFOQueue queue) {
		queue.setLength(4);
		Assertions.assertEquals(4, queue.getLength());
		Assertions.assertEquals(3, queue.size());
		IntIterator iterator = queue.iterator();
		Assertions.assertEquals(1, iterator.nextInt());
		Assertions.assertEquals(2, iterator.nextInt());
		Assertions.assertEquals(3, iterator.nextInt());
		Assertions.assertFalse(iterator.hasNext());
		queue.enqueue(4);
		queue.enqueue(5);
		Assertions.assertEquals(2, queue.dequeue());
		Assertions.assertEquals(3, queue.dequeue());
		Assertions.assertEquals(4, queue.dequeue());
		Assertions.assertEquals(5, queue.dequeue());
		Assertions.assertThrows(NoSuchElementException.class, queue::dequeue);
	}

	@Test
	void testSetLengthDecrease() {
		FixedLengthIntFIFOQueue queue = new FixedLengthIntFIFOQueue(4);
		queue.enqueue(1);
		queue.enqueue(2);
		queue.enqueue(3);
		queue.enqueue(4);
		testSetLengthDecreaseHelper(queue);

		queue = new FixedLengthIntFIFOQueue(4);
		queue.enqueue(0);
		queue.enqueue(1);
		queue.enqueue(2);
		queue.enqueue(3);
		queue.enqueue(4);
		testSetLengthDecreaseHelper(queue);
	}

	private void testSetLengthDecreaseHelper(FixedLengthIntFIFOQueue queue) {
		queue.setLength(3);
		Assertions.assertEquals(3, queue.getLength());
		Assertions.assertEquals(3, queue.size());
		IntIterator iterator = queue.iterator();
		Assertions.assertEquals(2, iterator.nextInt());
		Assertions.assertEquals(3, iterator.nextInt());
		Assertions.assertEquals(4, iterator.nextInt());
		Assertions.assertFalse(iterator.hasNext());
		queue.enqueue(5);
		Assertions.assertEquals(3, queue.dequeue());
		Assertions.assertEquals(4, queue.dequeue());
		Assertions.assertEquals(5, queue.dequeue());
		Assertions.assertThrows(NoSuchElementException.class, queue::dequeue);
	}
}
