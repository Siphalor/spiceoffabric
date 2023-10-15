package de.siphalor.spiceoffabric.util.queue;

class ArrayFixedLengthIntFIFOQueueTest extends FixedLengthIntFIFOQueueTest<ArrayFixedLengthIntFIFOQueue> {
	@Override
	ArrayFixedLengthIntFIFOQueue createQueue(int length) {
		return new ArrayFixedLengthIntFIFOQueue(length);
	}
}
