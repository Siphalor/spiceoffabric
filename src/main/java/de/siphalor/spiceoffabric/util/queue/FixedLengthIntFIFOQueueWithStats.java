package de.siphalor.spiceoffabric.util.queue;

import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import it.unimi.dsi.fastutil.ints.IntIterator;
import org.jetbrains.annotations.NotNull;

public class FixedLengthIntFIFOQueueWithStats implements FixedLengthIntFIFOQueue {
	private final FixedLengthIntFIFOQueue queue;
	private final Int2IntMap stats;

	public FixedLengthIntFIFOQueueWithStats(FixedLengthIntFIFOQueue queue) {
		this.queue = queue;
		this.stats = new Int2IntAVLTreeMap();
		this.rebuildStats();
	}

	private void rebuildStats() {
		stats.clear();
		queue.forEach(x -> stats.merge(x, 1, Integer::sum));
	}

	public Int2IntMap getStats() {
		return stats;
	}

	@Override
	public int size() {
		return queue.size();
	}

	@Override
	public void clear() {
		queue.clear();
		stats.clear();
	}

	@Override
	public int get(int index) {
		return queue.get(index);
	}

	@Override
	public void enqueue(int x) {
		queue.enqueue(x);
		stats.merge(x, 1, Integer::sum);
	}

	@Override
	public Integer forceEnqueue(int x) {
		Integer old = queue.forceEnqueue(x);
		if (old != null) {
			decrementStat(old);
		}
		incrementStat(x);
		return old;
	}

	@Override
	public int dequeue() {
		int x = queue.dequeue();
		decrementStat(x);
		return x;
	}

	private void incrementStat(int x) {
		stats.merge(x, 1, Integer::sum);
	}

	private void decrementStat(int x) {
		int amount = stats.computeIfPresent(x, (k, v) -> v - 1);
		if (amount <= 0) {
			stats.remove(x);
		}
	}

	@Override
	public int getLength() {
		return queue.getLength();
	}

	@Override
	public void setLength(int newLength) {
		int oldLength = queue.getLength();
		queue.setLength(newLength);
		if (newLength < oldLength) {
			rebuildStats();
		}
	}

	@Override
	public @NotNull IntIterator iterator() {
		return queue.iterator();
	}

	@Override
	public void forEach(IntConsumer action) {
		queue.forEach(action);
	}
}
