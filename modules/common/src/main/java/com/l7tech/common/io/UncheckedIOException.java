package com.l7tech.common.io;

import java.io.IOException;

/**
 * An unchecked exception for wrapping IOException in situations where IOException can't be thrown due to
 * an existing contract.
 */
public class UncheckedIOException extends RuntimeException {
    public UncheckedIOException() {
        super();
    }

    public UncheckedIOException(String message) {
        super(message);
    }

    public UncheckedIOException(String message, IOException cause) {
        super(message, cause);
    }

    public UncheckedIOException(IOException cause) {
        super(cause);
    }

    public IOException getIOException() {
        Throwable cause = super.getCause();
        return cause instanceof IOException ? (IOException)cause : null;
    }
}
