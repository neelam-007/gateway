package com.l7tech.common.io;

import java.io.IOException;
import java.io.Reader;

/**
 * A Reader that will persistently throw the specified IOException.
 */
public class IOExceptionThrowingReader extends Reader {

    //- PUBLIC

    /**
     * Create a reader that will throw the given exception at every opportunity.
     *
     * @param ioe The exception to throw
     */
    public IOExceptionThrowingReader(IOException ioe) {
        this( ioe, true );
    }


    /**
     * Create a reader that will throw the given exception (optionally on close).
     *
     * @param ioe The exception to throw
     */
    public IOExceptionThrowingReader(IOException ioe, boolean throwOnClose) {
        this.exception = ioe;
        this.throwOnClose = throwOnClose;
    }


    /**
     * Throw the exception for this reader
     *
     * @throws IOException Always thrown
     */
    @Override
    public int read(char cbuf[], int off, int len) throws IOException {
        throw exception;
    }

    /**
     * Throw the exception for this reader
     *
     * @throws IOException Always thrown
     */
    @Override
    public void close() throws IOException {
        if ( throwOnClose ) {
            throw exception;
        }
    }

    //- PRIVATE

    private final IOException exception;
    private final boolean throwOnClose;
}
