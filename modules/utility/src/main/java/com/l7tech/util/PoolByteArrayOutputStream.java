/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.util;

import java.io.*;
import java.nio.charset.Charset;

/**
 * A version of ByteArrayOutputStream that uses the BufferPool.  This class is not synchronized.
 * <p/>
 * Call {@link #close()} to return the buffer to the pool when you are done with it.
 */
public class PoolByteArrayOutputStream extends OutputStream {
    private static final int DEFAULT_BUF_SIZE = SyspropUtil.getInteger("com.l7tech.util.BufferPoolBaos.defaultBufSize", 1024);
    public static final int MIN_BUF_SIZE = BufferPool.getMinBufferSize();
    protected byte[] buf; //      The buffer where data is stored.
    protected int count; //       The number of valid bytes in the buffer.

    /** Create a new stream with the default buffer size. */
    public PoolByteArrayOutputStream()
    {
        this(0);
    }

    /**
     * Create a new stream with the specified buffer size.
     *
     * @param size the number of bytes to buffer.  Must be nonnegative.  Ignored if smaller than {@link com.l7tech.util.BufferPool#getMinBufferSize()}.
     */
    public PoolByteArrayOutputStream(int size)
    {
        if (size < 0) {
            throw new IllegalArgumentException("size must be nonnegative");
        }
        if (size > 0 && size < MIN_BUF_SIZE) {
            size = MIN_BUF_SIZE;
        }
        if (size < 1) {
            size = DEFAULT_BUF_SIZE;
        }

        buf = BufferPool.getBuffer(size);
        count = 0;
    }

    @Override
    public void close() {
        if (buf != null) {
            BufferPool.returnBuffer(buf);
            buf = null;
            count = 0;
        }
    }

    /**
     * Discards all buffered data, resetting to an empty stream.
     * <p/>
     * This method resets the byte count to zero, but keeps the buffer on hand so it can be immediately refilled
     * to at least its original size without any reallocation or copying.
     */
    public void reset()
    {
        count = 0;
    }

    /**
     * Returns a peek at the actual backing byte array used by this stream.  This should not be kept
     * since it may be returned to the pool next time this instance is written or closed.
     * <p/>
     * If you want to keep a copy of this data, you must copy it elsewhere yourself.
     *
     * @return a reference to the internal byte array, which will be at least {@link #size()} bytes long,
     *         but may contain an unspecified amount of extra garbage after this point.  Caller must not
     *         retain a reference to this byte array, as it belongs to the pool and is only being borrowed.
     */
    public byte[] getPooledByteArray() {
        return buf;
    }

    /**
     * Remove the backing array from this stream.
     *
     * <p>You can not use this stream after calling this method, this stream
     * will not use the array after this method is called.</p>
     *
     * <p>It is safe (but not necessary) to call {@link #close} after calling
     * this method.</p>
     *
     * <p>The caller is responsible for the returned array.</p>
     *
     * <p>Call {@link #size} before calling this method to determine how much
     * data is in the array.</p>
     *
     * @return The backing array
     * @see BufferPool#returnBuffer(byte[]) BufferPool.returnBuffer(byte[] buffer)
     */
    public byte[] detachPooledByteArray() {
        byte[] buffer = buf;
        buf = null;
        count = 0;
        return buffer;
    }

    /**
     * Removes the backing array from this stream and returns as InputStream.
     *
     * <p>You can not use this stream after calling this method, this stream
     * will not use the array after this method is called.</p>
     *
     * <p>It is safe (but not necessary) to call {@link #close} after calling
     * this method.</p>
     *
     * <p>Closing the returned stream will re-pool the returned array.</p>
     *
     * @return The input stream.
     */
    public InputStream toInputStream() {
        needBufIse();
        final int length = count;
        final byte[] buffer = detachPooledByteArray();

        return new InputStream() {
            private final InputStream delegate = new ByteArrayInputStream( buffer, 0, length );
            private boolean closed = false;

            @Override
            public int read() throws IOException {
                checkOpen();
                return delegate.read();
            }

            @Override
            public int read( byte[] b ) throws IOException {
                checkOpen();
                return delegate.read( b );
            }

            @Override
            public int read( byte[] b, int off, int len ) throws IOException {
                checkOpen();
                return delegate.read( b, off, len );
            }

            @Override
            public long skip( long n ) throws IOException {
                checkOpen();
                return delegate.skip( n );
            }

            @Override
            public int available() throws IOException {
                checkOpen();
                return delegate.available();
            }

            @Override
            public void mark( int readlimit ) {
                delegate.mark( readlimit );
            }

            @Override
            public void reset() throws IOException {
                checkOpen();
                delegate.reset();
            }

            @Override
            public boolean markSupported() {
                return delegate.markSupported();
            }

            @Override
            public void close() throws IOException {
                closed = true;
                BufferPool.returnBuffer( buffer );
            }

            private void checkOpen() throws IOException {
                if ( closed ) throw new IOException( "Stream is closed" );
            }
        };
    }

    /** @return the number of bytes currently accumulated in the buffer. */
    public int size()
    {
        return count;
    }

    /** @return a newly-allocated byte array of exactly the required size. */
    public byte[] toByteArray()
    {
        needBufIse();
        final int size = size();
        byte [] returnBuf = new byte[size];
        System.arraycopy(buf, 0, returnBuf, 0, size);
        return returnBuf;
    }

    /**
     * Convert the buffer into a string using the default encoding.
     *
     * @return a new String.  Never null.
     * @throws IllegalStateException if the stream is closed.
     */
    @Override
    public String toString() {
        needBufIse();
        return new String(buf, 0, count);
    }

    /**
     * Convert the buffer into a string using the specified encoding.
     *
     * @param enc The character encoding name.  Required.
     * @return a new String.  Never null.
     * @throws IllegalStateException if the stream is closed.
     * @throws UnsupportedEncodingException if the specified encoding is not recognized.
     */
    public String toString(String enc) throws UnsupportedEncodingException {
        needBufIse();
        return new String(buf, 0, count, enc);
    }

    /**
     * Convert the buffer into a string using the specified charset.
     *
     * @param enc a Charset instance.  Required.
     * @return a new String.  Never null.
     * @throws IllegalStateException if the stream is closed.
     */
    public String toString(Charset enc) {
        needBufIse();
        return new String(buf, 0, count, enc);
    }

    public void write(final byte[] b, final int off, final int len) throws IOException {
        if (len < 0)
            throw new IllegalArgumentException("len must be nonnegative");
        needBuf();
        if (len > (buf.length - count)) {
            int buffLen = buf.length;
            while (len > (buffLen - count))
                buffLen <<= 1;
            byte[] newBuf = BufferPool.getBuffer(buffLen);
            System.arraycopy(buf, 0, newBuf, 0, count);
            byte[] oldBuf = buf;
            buf = newBuf;
            BufferPool.returnBuffer(oldBuf);
        }
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }

    public void write(int b) throws IOException {
        needBuf();
        if (count >= buf.length) {
            byte [] newBuf = BufferPool.getBuffer(buf.length << 1);
            System.arraycopy(buf, 0, newBuf, 0, count);
            byte[] oldBuf = buf;
            buf = newBuf;
            BufferPool.returnBuffer(oldBuf);
        }
        buf[count++] = (byte)b;
    }

    /**
     * Copy all buffered bytes to the target output stream.
     *
     * @param out output stream to which the buffer should be copied.  Required.
     * @throws java.io.IOException on write error
     */
    public void writeTo(OutputStream out) throws IOException
    {
        needBuf();
        out.write(buf,0,count);
    }

    private void needBuf() throws IOException {
        if (buf == null) throw new IOException("BufferPool OutputStream is closed");
    }

    private void needBufIse() {
        if (buf == null) throw new IllegalStateException("BufferPool OutputStream is closed");
    }
}
