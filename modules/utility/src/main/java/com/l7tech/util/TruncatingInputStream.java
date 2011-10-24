package com.l7tech.util;

import java.io.*;

/**
 * A version of InputStream for reading a truncated input stream.  This class is not synchronized.
 * The default chunk size is 512 kb.  Check lastRead for end of input stream or end of truncated stream
 * <p/>
 */
public class TruncatingInputStream extends InputStream {
    public static final long DEFAULT_SIZE_LIMIT = 1024L * 512L; // 512 kb

    private final InputStream inputStream;
    private final long maximumSize;
    private long position; // position for next read location, -1 if eof read

    /**
     * Create a new truncating stream
     *
     * @param inputStream  stream to read from
     * @throws IOException  seeking failed
     */
    public TruncatingInputStream( final InputStream inputStream ) {
        this(inputStream, DEFAULT_SIZE_LIMIT );
    }

    /**
     * Create a new stream with the given size limit
     *
     * @param inputStream   stream to read from
     * @param maximumSize          the number of bytes to truncate after
     * @throws IOException  seeking failed
     */
    public TruncatingInputStream( final InputStream inputStream,
                                  final long maximumSize ) {
        this.inputStream = inputStream;
        this.maximumSize = maximumSize;
        if ( maximumSize < 0L) {
            throw new IllegalArgumentException("size must be nonnegative");
        }
    }

    /**
     * Reads the next byte of data from the input stream.
     * Check the value of @lastRead for end of input stream or end of chunk
     * @return the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     *
     * @throws IOException If an error occurs.
     */
    @Override
    public int read() throws IOException {
        int read = (int)getAdjustedLength( 1L, -1L );
        int data = -1;
        if ( read == 1 ) {
            data = inputStream.read();
            dataRead( data == -1 ? -1L : 1L );
        }
        return data;
    }

    /**
     * Read data from the input stream.
     *
     * @param b Byte array to read into
     * @param off The array offset
     * @param len The length of data to read
     * @return The number of bytes read
     * @throws IOException If an error occurs.
     */
    @Override
    public int read( final byte[] b, final int off, final int len ) throws IOException {
        int read = (int)getAdjustedLength( (long)len, -1L );
        if ( read <= 0 ) {
            return read;
        } else {
            return (int)dataRead( (long)inputStream.read( b, off, read ) );
        }
    }

    /**
     * Is data available from the stream.
     *
     * <p>The available byte count is truncated to match this streams limit.</p>
     *
     * @return The (possibly truncated) available bytes.
     * @throws IOException If an error occurs.
     */
    @Override
    public int available() throws IOException {
        return (int)getAdjustedLength( (long) inputStream.available(), 0L );
    }

    /**
     * Skips over and discards bytes of data from this input stream.
     *
     * @param n The number of bytes to skip
     * @return the  actual number of bytes skipped.
     * @throws IOException If an error occurs
     */
    @Override
    public long skip( final long n ) throws IOException {
        long skip = getAdjustedLength( n, 0L );
        return dataRead( inputStream.skip( skip ) );
    }

    /**
     * Get the position for the next read
     *
     * @return  position for the next read, -1 for end of file
     */
    public long getPosition() {
        return position;
    }

    private long getAdjustedLength( final long length, final long eofValue ) {
        if( position < 0L || position >= maximumSize ){
            return eofValue;
        } else if ( position + length >= maximumSize ) {
            return maximumSize - position;
        } else {
            return length;
        }
    }

    private long dataRead( final long length ) {
        if ( length == -1L ) {
            position = -1L;
        } else {
            position += length;
        }
        return length;
    }
}
