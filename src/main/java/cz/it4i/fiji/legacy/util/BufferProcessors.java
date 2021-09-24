package cz.it4i.fiji.legacy.util;

import java.nio.ByteBuffer;

public class BufferProcessors {
	public interface BufferProcessor {
		void put(final ByteBuffer byteBuffer, final double value);
		double get(final ByteBuffer byteBuffer);
	}

	final static BufferProcessor BYTE_BUFFERS = new BufferProcessor() {
		@Override
		public void put(ByteBuffer byteBuffer, double value) { byteBuffer.put((byte)value); }
		@Override
		public double get(ByteBuffer byteBuffer) { return byteBuffer.get(); }
	};

	final static BufferProcessor SHORT_BUFFERS = new BufferProcessor() {
		@Override
		public void put(ByteBuffer byteBuffer, double value) { byteBuffer.putShort((short)value); }
		@Override
		public double get(ByteBuffer byteBuffer) { return byteBuffer.getShort(); }
	};

	final static BufferProcessor INT_BUFFERS = new BufferProcessor() {
		@Override
		public void put(ByteBuffer byteBuffer, double value) { byteBuffer.putInt((int)value); }
		@Override
		public double get(ByteBuffer byteBuffer) { return byteBuffer.getInt(); }
	};

	final static BufferProcessor LONG_BUFFERS = new BufferProcessor() {
		@Override
		public void put(ByteBuffer byteBuffer, double value) { byteBuffer.putLong((long)value); }
		@Override
		public double get(ByteBuffer byteBuffer) { return byteBuffer.getLong(); }
	};

	final static BufferProcessor FLOAT_BUFFERS = new BufferProcessor() {
		@Override
		public void put(ByteBuffer byteBuffer, double value) { byteBuffer.putFloat((float)value); }
		@Override
		public double get(ByteBuffer byteBuffer) { return byteBuffer.getFloat(); }
	};

	final static BufferProcessor DOUBLE_BUFFERS = new BufferProcessor() {
		@Override
		public void put(ByteBuffer byteBuffer, double value) { byteBuffer.putDouble(value); }
		@Override
		public double get(ByteBuffer byteBuffer) { return byteBuffer.getDouble(); }
	};
}
