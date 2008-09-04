/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.io;

import com.l7tech.util.BufferPool;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * A version of ByteArrayOutputStream that uses the BufferPool.  This class is not synchronized.
 * This class is based on the Sun JDK ByteArrayOutputStream.  Sadly, simply subclassing it did not work
 * because it always allocates a new byte array.
 * <p/>
 * Call {@link #close()} to return the buffer to the pool when you are done with it.
 */
public class BufferPoolByteArrayOutputStream extends OutputStream {
    /**
     * The buffer where data is stored.
     */
    protected byte buf[];

    /**
     * The number of valid bytes in the buffer.
     */
    protected int count;

    /**
     * Creates a new byte array output stream. The buffer capacity is
     * initially 1024 bytes, though its size increases if necessary.
     */
    public BufferPoolByteArrayOutputStream() {
        this(1024);
    }

    /**
     * Creates a new buffer pooled byte array output stream, with a buffer capacity of
     * at least the specified size, in bytes.
     *
     * @param   size   the initial size.
     * @exception  IllegalArgumentException if size is negative.
     */
    public BufferPoolByteArrayOutputStream(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: "
                    + size);
        }
        buf = BufferPool.getBuffer(size);
    }

    /**
     * Writes the specified byte to this byte array output stream.
     *
     * @param   b   the byte to be written.
     */
    public void write(int b) throws IOException {
        int newcount = count + 1;
        if (buf == null) throw new IOException("BufferPool OutputStream is closed");
        if (newcount > buf.length) {
            byte newbuf[] = BufferPool.getBuffer(Math.max(buf.length << 1, newcount));
            System.arraycopy(buf, 0, newbuf, 0, count);
            byte[] oldbuf = buf;
            buf = newbuf;
            BufferPool.returnBuffer(oldbuf);
        }
        buf[count] = (byte)b;
        count = newcount;
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this byte array output stream.
     *
     * @param   b     the data.
     * @param   off   the start offset in the data.
     * @param   len   the number of bytes to write.
     */
    public void write(byte b[], int off, int len) throws IOException {
        if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        int newcount = count + len;
        if (buf == null) throw new IOException("BufferPool OutputStream is closed");
        if (newcount > buf.length) {
            byte newbuf[] = BufferPool.getBuffer(Math.max(buf.length << 1, newcount));
            System.arraycopy(buf, 0, newbuf, 0, count);
            byte[] oldbuf = buf;
            buf = newbuf;
            BufferPool.returnBuffer(oldbuf);
        }
        System.arraycopy(b, off, buf, count, len);
        count = newcount;
    }

    /**
     * Writes the complete contents of this byte array output stream to
     * the specified output stream argument, as if by calling the output
     * stream's write method using <code>out.write(buf, 0, count)</code>.
     *
     * @param      out   the output stream to which to write the data.
     * @exception  java.io.IOException  if an I/O error occurs.
     */
    public void writeTo(OutputStream out) throws IOException {
        if (buf == null) throw new IOException("BufferPool OutputStream is closed");
        out.write(buf, 0, count);
    }

    /**
     * Resets the <code>count</code> field of this byte array output
     * stream to zero, so that all currently accumulated output in the
     * output stream is discarded. The output stream can be used again,
     * reusing the already allocated buffer space.
     *
     * @see     java.io.ByteArrayInputStream#reset()
     */
    public void reset() {
        count = 0;
    }

    /**
     * Creates a newly allocated byte array. Its size is the current
     * size of this output stream and the valid contents of the buffer
     * have been copied into it.
     *
     * @return  the current contents of this output stream, as a byte array.
     * @throws  IllegalStateException if the output stream has been closed.
     * @see     java.io.ByteArrayOutputStream#size()
     */
    public byte[] toByteArray() {
        if (buf == null) throw new IllegalStateException("BufferPool OutputStream is closed");
        byte newbuf[] = new byte[count];
        System.arraycopy(buf, 0, newbuf, 0, count);
        return newbuf;
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
     * Returns the current size of the buffer.
     *
     * @return  the value of the <code>count</code> field, which is the number
     *          of valid bytes in this output stream.
     * @see     java.io.ByteArrayOutputStream#size()
     */
    public int size() {
        return count;
    }

    /**
     * Converts the buffer's contents into a string, translating bytes into
     * characters according to the platform's default character encoding.
     *
     * @return String translated from the buffer's contents.
     * @throws IllegalStateException if the output stream has been closed.
     * @since  JDK1.1
     */
    public String toString() {
        if (buf == null) throw new IllegalStateException("BufferPool OutputStream is closed");
        return new String(buf, 0, count);
    }

    /**
     * Converts the buffer's contents into a string, translating bytes into
     * characters according to the specified character encoding.
     *
     * @param   enc  a character-encoding name.
     * @return String translated from the buffer's contents.
     * @throws IllegalStateException if the output stream has been closed.
     * @throws java.io.UnsupportedEncodingException
     *         If the named encoding is not supported.
     */
    public String toString(String enc) throws UnsupportedEncodingException {
        if (buf == null) throw new IllegalStateException("BufferPool OutputStream is closed");
        return new String(buf, 0, count, enc);
    }

    public void close() {
        if (buf != null) {
            BufferPool.returnBuffer(buf);
            buf = null;
            count = 0;
        }
    }
}
