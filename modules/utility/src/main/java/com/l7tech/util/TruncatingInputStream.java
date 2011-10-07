package com.l7tech.util;

import java.io.*;
import java.util.logging.Logger;

/**
 * A version of InputStream for reading a truncated input stream.  This class is not synchronized.
 * The default chunk size is 512 kb.  Check lastRead for end of input stream or end of truncated stream
 * <p/>
 */
public class TruncatingInputStream extends InputStream {
    private static final Logger logger = Logger.getLogger(TruncatingInputStream.class.getName());

    private static final long DEFAULT_CHUNK_SIZE = 1024 * 512; // 512 kb
    protected long lastRead; // last read byte location, -1 for eof read
    private InputStream inputStream;
    private long endOfChunk;
    private long size;

    /**
     *  Create a new stream reading from the beginning
     * @param inputStream  stream to read from
     * @throws IOException  seeking failed
     */
    public TruncatingInputStream(InputStream inputStream)  throws IOException {
        this(inputStream, 0, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Create a new stream reading from the specified start value
     * @param inputStream  stream to read from
     * @param start starting point to read from
     * @throws IOException  seeking failed
     */
    public TruncatingInputStream(InputStream inputStream, long start) throws IOException  {
        this(inputStream, start, DEFAULT_CHUNK_SIZE);
    }

    /**
     * Create a new stream with the specified start value and the chunk size
     *
     * @param inputStream   stream to read from
     * @param start         starting point to read from
     * @param size          the number of bytes to truncate after
     * @throws IOException  seeking failed
     */
    public TruncatingInputStream(InputStream inputStream,long start, long size ) throws IOException {
        this.inputStream = inputStream;
        this.size = size;
        if (size < 0) {
            throw new IllegalArgumentException("size must be nonnegative");
        }
        if (start < 0) {
            throw new IllegalArgumentException("start must be nonnegative");
        }


        lastRead = start;
        endOfChunk = size + start;
        inputStream.skip(start);

        lastRead = start;


    }

    /**
     * Reads the next byte of data from the input stream.
     * Check the value of @lastRead for end of input stream or end of chunk
     * @return the next byte of data, or <code>-1</code> if the end of the
     *             stream is reached.
     * @throws IOException
     */
    @Override
    public int read() throws IOException {
        if(endOfChunk <= lastRead ){
            return -1;
        }
        int data = inputStream.read();
        if(data < 0 ){
            lastRead = -1;
        }else{
            ++lastRead;
        }
        return data;
    }

    /**
     * Skips over and discards bytes of data from this input stream.
     * Resets the truncated end point
     * @param n     bytes to skip
     * @return the  actual number of bytes skipped.
     * @throws IOException
     */
    @Override
    public long skip(long n) throws IOException {
        endOfChunk = size + n;
        return super.skip(n);    //To change body of overridden methods use File | Settings | File Templates.
    }


    /**
     * Get the location of the last read byte
     * @return  location of the last read byte, -1 for end of file
     */
    public long getLastRead() {
        return lastRead;
    }
}
