package de.siphalor.spiceoffabric.util.queue;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

abstract class FixedLengthIntFIFOQueueTest<T extends FixedLengthIntFIFOQueue> {
	abstract T createQueue(int length);

	@Test
	void constructor() {
		Assertions.assertDoesNotThrow(() -> createQueue(0));
		Assertions.assertDoesNotThrow(() -> createQueue(1));
		Assertions.assertDoesNotThrow(() -> createQueue(1000));
		Assertions.assertThrows(IllegalArgumentException.class, () -> createQueue(-1));
	}

	@Test
	void size() {
		FixedLengthIntFIFOQueue queue = createQueue(2);
		Assertions.assertEquals(0, queue.size());
		queue.enqueue(1);
		Assertions.assertEquals(1, queue.size());
		queue.enqueue(1);
		Assertions.assertEquals(2, queue.size());
		queue.dequeue();
		Assertions.assertEquals(1, queue.size());
	}

	@Test
	void clear() {
		FixedLengthIntFIFOQueue queue = createQueue(2);
		queue.enqueue(1);
		queue.clear();
		Assertions.assertEquals(0, queue.size());
		Assertions.assertFalse(queue.iterator().hasNext());
	}

	@Test
	void isEmpty() {
		FixedLengthIntFIFOQueue queue = createQueue(2);
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
	void enqueue() {
		FixedLengthIntFIFOQueue queue = createQueue(2);
		queue.enqueue(1);
		Assertions.assertEquals(1, queue.first());
		queue.enqueue(2);
		Assertions.assertEquals(1, queue.first());

		Assertions.assertThrows(IllegalStateException.class, () -> queue.enqueue(3));
		Assertions.assertEquals(1, queue.first());
	}

	@Test
	void forceEnqueue() {
		FixedLengthIntFIFOQueue queue = createQueue(2);
		Assertions.assertNull(queue.forceEnqueue(1));
		Assertions.assertEquals(1, queue.first());
		Assertions.assertNull(queue.forceEnqueue(2));
		Assertions.assertEquals(1, queue.first());
		Assertions.assertEquals(1, queue.forceEnqueue(3));
		Assertions.assertEquals(2, queue.first());
		Assertions.assertEquals(2, queue.forceEnqueue(4));
		Assertions.assertEquals(3, queue.first());
	}

	@Test
	void dequeue() {
		FixedLengthIntFIFOQueue queue = createQueue(2);
		Assertions.assertThrows(NoSuchElementException.class, queue::dequeue);
		queue.enqueue(1);
		Assertions.assertEquals(1, queue.dequeue());
		Assertions.assertEquals(0, queue.size());
		queue.enqueue(2);
		queue.enqueue(3);
		Assertions.assertEquals(2, queue.dequeue());
		queue.enqueue(4);
		Assertions.assertEquals(3, queue.dequeue());
		Assertions.assertEquals(1, queue.size());
		Assertions.assertEquals(4, queue.first());
		Assertions.assertEquals(4, queue.dequeue());
		Assertions.assertEquals(0, queue.size());
		Assertions.assertThrows(NoSuchElementException.class, queue::dequeue);
	}

	@Test
	void first() {
		FixedLengthIntFIFOQueue queue = createQueue(2);
		Assertions.assertThrows(NoSuchElementException.class, queue::first);
		queue.enqueue(1);
		Assertions.assertEquals(1, queue.first());
		queue.enqueue(2);
		Assertions.assertEquals(1, queue.first());
		queue.dequeue();
		queue.enqueue(3);
		Assertions.assertEquals(2, queue.first());
		queue.dequeue();
		queue.enqueue(4);
		Assertions.assertEquals(3, queue.first());
		queue.dequeue();
		queue.dequeue();
		Assertions.assertThrows(NoSuchElementException.class, queue::first);
	}

	@Test
	void iterator() {
		FixedLengthIntFIFOQueue queue = createQueue(2);
		queue.enqueue(1);
		queue.enqueue(2);
		queue.dequeue();
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
	void forEach() {
		FixedLengthIntFIFOQueue queue = createQueue(2);
		queue.enqueue(1);
		queue.enqueue(2);
		queue.dequeue();
		queue.enqueue(3);

		IntList data = new IntArrayList(2);
		queue.forEach(data::add);
		Assertions.assertArrayEquals(new int[]{2, 3}, data.toIntArray());
	}

	@Test
	void setLengthIncrease() {
		FixedLengthIntFIFOQueue queue = createQueue(3);
		queue.enqueue(1);
		queue.enqueue(2);
		queue.enqueue(3);
		setLengthIncreaseHelper(queue);

		queue = createQueue(3);
		queue.enqueue(0);
		queue.enqueue(1);
		queue.enqueue(2);
		queue.dequeue();
		queue.enqueue(3);
		setLengthIncreaseHelper(queue);
	}

	private void setLengthIncreaseHelper(FixedLengthIntFIFOQueue queue) {
		queue.setLength(4);
		Assertions.assertEquals(4, queue.getLength());
		Assertions.assertEquals(3, queue.size());
		IntIterator iterator = queue.iterator();
		Assertions.assertEquals(1, iterator.nextInt());
		Assertions.assertEquals(2, iterator.nextInt());
		Assertions.assertEquals(3, iterator.nextInt());
		Assertions.assertFalse(iterator.hasNext());
		queue.dequeue();
		queue.enqueue(4);
		queue.enqueue(5);
		Assertions.assertEquals(2, queue.dequeue());
		Assertions.assertEquals(3, queue.dequeue());
		Assertions.assertEquals(4, queue.dequeue());
		Assertions.assertEquals(5, queue.dequeue());
		Assertions.assertThrows(NoSuchElementException.class, queue::dequeue);
	}

	@Test
	void setLengthDecrease() {
		FixedLengthIntFIFOQueue queue = createQueue(4);
		queue.enqueue(1);
		queue.enqueue(2);
		queue.enqueue(3);
		queue.enqueue(4);
		setLengthDecreaseHelper(queue);

		queue = createQueue(4);
		queue.enqueue(0);
		queue.enqueue(1);
		queue.enqueue(2);
		queue.enqueue(3);
		queue.dequeue();
		queue.enqueue(4);
		setLengthDecreaseHelper(queue);
	}

	private void setLengthDecreaseHelper(FixedLengthIntFIFOQueue queue) {
		queue.setLength(3);
		Assertions.assertEquals(3, queue.getLength());
		Assertions.assertEquals(3, queue.size());
		IntIterator iterator = queue.iterator();
		Assertions.assertEquals(2, iterator.nextInt());
		Assertions.assertEquals(3, iterator.nextInt());
		Assertions.assertEquals(4, iterator.nextInt());
		Assertions.assertFalse(iterator.hasNext());
		queue.dequeue();
		queue.enqueue(5);
		Assertions.assertEquals(3, queue.dequeue());
		Assertions.assertEquals(4, queue.dequeue());
		Assertions.assertEquals(5, queue.dequeue());
		Assertions.assertThrows(NoSuchElementException.class, queue::dequeue);
	}
}
