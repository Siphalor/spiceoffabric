package de.siphalor.spiceoffabric.util.queue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class FixedLengthIntFIFOQueueWithStatsTest extends FixedLengthIntFIFOQueueTest<FixedLengthIntFIFOQueueWithStats> {
	@Override
	FixedLengthIntFIFOQueueWithStats createQueue(int length) {
		return new FixedLengthIntFIFOQueueWithStats(new ArrayFixedLengthIntFIFOQueue(length));
	}

	@Test
	@Override
	void clear() {
		super.clear();

		FixedLengthIntFIFOQueueWithStats queue = createQueue(2);
		queue.enqueue(1);
		queue.clear();
		Assertions.assertTrue(queue.getStats().isEmpty());
	}

	@Test
	@Override
	void enqueue() {
		super.enqueue();

		FixedLengthIntFIFOQueueWithStats queue = createQueue(10);
		queue.enqueue(1);
		Assertions.assertEquals(Map.of(1, 1), queue.getStats());
		queue.enqueue(1);
		Assertions.assertEquals(Map.of(1, 2), queue.getStats());
		queue.enqueue(2);
		Assertions.assertEquals(Map.of(1, 2, 2, 1), queue.getStats());
		queue.enqueue(2);
		Assertions.assertEquals(Map.of(1, 2, 2, 2), queue.getStats());
		queue.enqueue(3);
		Assertions.assertEquals(Map.of(1, 2, 2, 2, 3, 1), queue.getStats());
	}

	@Test
	@Override
	void forceEnqueue() {
		super.forceEnqueue();

		FixedLengthIntFIFOQueueWithStats queue = createQueue(3);
		queue.forceEnqueue(1);
		Assertions.assertEquals(Map.of(1, 1), queue.getStats());
		queue.forceEnqueue(1);
		Assertions.assertEquals(Map.of(1, 2), queue.getStats());
		queue.forceEnqueue(2);
		Assertions.assertEquals(Map.of(1, 2, 2, 1), queue.getStats());
		queue.forceEnqueue(2);
		Assertions.assertEquals(Map.of(1, 1, 2, 2), queue.getStats());
		queue.forceEnqueue(3);
		Assertions.assertEquals(Map.of(2, 2, 3, 1), queue.getStats());
	}

	@Test
	@Override
	void dequeue() {
		super.dequeue();

		FixedLengthIntFIFOQueueWithStats queue = createQueue(10);
		queue.enqueue(1);
		queue.enqueue(1);
		queue.enqueue(2);
		queue.enqueue(2);
		queue.enqueue(2);
		queue.enqueue(3);
		queue.enqueue(4);

		Assertions.assertEquals(1, queue.dequeue());
		Assertions.assertEquals(Map.of(1, 1, 2, 3, 3, 1, 4, 1), queue.getStats());
		Assertions.assertEquals(1, queue.dequeue());
		Assertions.assertEquals(Map.of(2, 3, 3, 1, 4, 1), queue.getStats());
		Assertions.assertEquals(2, queue.dequeue());
		Assertions.assertEquals(Map.of(2, 2, 3, 1, 4, 1), queue.getStats());
		Assertions.assertEquals(2, queue.dequeue());
		Assertions.assertEquals(Map.of(2, 1, 3, 1, 4, 1), queue.getStats());
		Assertions.assertEquals(2, queue.dequeue());
		Assertions.assertEquals(Map.of(3, 1, 4, 1), queue.getStats());
		Assertions.assertEquals(3, queue.dequeue());
		Assertions.assertEquals(Map.of(4, 1), queue.getStats());
		Assertions.assertEquals(4, queue.dequeue());
		Assertions.assertTrue(queue.getStats().isEmpty());
	}

	@Test
	@Override
	void setLengthIncrease() {
		super.setLengthIncrease();

		FixedLengthIntFIFOQueueWithStats queue = createQueue(2);
		queue.enqueue(1);
		queue.enqueue(2);
		Assertions.assertEquals(Map.of(1, 1, 2, 1), queue.getStats());
		queue.setLength(3);
		Assertions.assertEquals(Map.of(1, 1, 2, 1), queue.getStats());
	}

	@Test
	@Override
	void setLengthDecrease() {
		super.setLengthDecrease();

		FixedLengthIntFIFOQueueWithStats queue = createQueue(3);
		queue.enqueue(1);
		queue.enqueue(2);
		queue.enqueue(3);
		Assertions.assertEquals(Map.of(1, 1, 2, 1, 3, 1), queue.getStats());
		queue.setLength(2);
		Assertions.assertEquals(Map.of(2, 1, 3, 1), queue.getStats());
	}
}
