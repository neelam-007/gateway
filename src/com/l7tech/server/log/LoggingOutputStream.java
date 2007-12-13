package com.l7tech.server.log;

import java.io.OutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OutputStream that logs using the logging API.
 *
 * @author Steve Jones
 */
class LoggingOutputStream extends OutputStream {

    //- PUBLIC

    /**
     * Create an output stream that logs at the given level to the given logger.
     *
     * @param logger The logger to log using
     * @param level The level to log messages
     */
    public LoggingOutputStream(final Logger logger,
                               final Level level)
            throws IllegalArgumentException {
        if ( logger == null ) throw new IllegalArgumentException("logger must not be null");
        if ( level == null ) throw new IllegalArgumentException("level must not be null");

        this.logger = logger;
        this.level = level;
        this.bufLength = DEFAULT_BUFFER_LENGTH;
        this.buf = new byte[DEFAULT_BUFFER_LENGTH];
        this.count = 0;
    }

    @Override
    public void close() {
        flush();
        this.wasClosed = true;
    }

    public void write(final int b) throws IOException {
        if (this.wasClosed) {
            throw new IOException("The stream has been closed.");
        }

        // don't log nulls
        if (b == 0) {
            return;
        }

        // would this be writing past the buffer?
        if (this.count == this.bufLength) {
            // grow the buffer
            final int newBufLength = this.bufLength + DEFAULT_BUFFER_LENGTH;
            final byte[] newBuf = new byte[newBufLength];

            System.arraycopy(this.buf, 0, newBuf, 0, this.bufLength);

            this.buf = newBuf;
            this.bufLength = newBufLength;
        }

        this.buf[this.count] = (byte) b;
        this.count++;
    }

    @Override
    public void flush() {
        if ( this.count == 0 ) {
            return;
        }

        boolean lineBreakEnding = endsWithLineBreak();

        // skip any empty lines
        if ( !lineBreakEnding || this.count != LINE_SEP_LEN ) {
            final byte[] bytes;

            if ( lineBreakEnding ) {
                bytes = new byte[this.count - LINE_SEP_LEN];
            } else {
                bytes = new byte[this.count];
            }

            System.arraycopy(this.buf, 0, bytes, 0, bytes.length);

            this.logger.log(this.level, new String(bytes));
        }

        reset();
    }

    //- PRIVATE

    private static final int DEFAULT_BUFFER_LENGTH = 2048;
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final int LINE_SEP_LEN = LINE_SEPARATOR.length();

    private final Logger logger;
    private final Level level;

    private boolean wasClosed = false;
    private byte[] buf;
    private int count;
    private int bufLength;
    
    private void reset() {
        this.count = 0;
    }

    private boolean endsWithLineBreak() {
        boolean lb = false;

        if ( count >= LINE_SEP_LEN ) {
            if ( LINE_SEP_LEN == 1 ) {
                lb = ((char) this.buf[count-1]) == LINE_SEPARATOR.charAt(0);
            } else {
                lb = ((char) this.buf[count-2]) == LINE_SEPARATOR.charAt(0) &&
                     ((char) this.buf[count-1]) == LINE_SEPARATOR.charAt(1);
            }
        }

        return lb;
    }
}