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
        exception = ioe;
    }


    /**
     * Throw the exception for this reader
     *
     * @throws IOException Always thrown
     */
    public int read(char cbuf[], int off, int len) throws IOException {
        throw exception;
    }

    /**
     * Throw the exception for this reader
     *
     * @throws IOException Always thrown
     */
    public void close() throws IOException {
        throw exception;
    }

    //- PRIVATE

    private final IOException exception;
}
