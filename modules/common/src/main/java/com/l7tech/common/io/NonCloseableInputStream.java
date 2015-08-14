package com.l7tech.common.io;

import org.jetbrains.annotations.NotNull;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Simple {@code InputStream} wrapper that does not close the underlying input stream.
 */
public class NonCloseableInputStream extends FilterInputStream {

    /**
     * Creates a {@code NonCloseableInputStream} using {@code in} as underlying input stream.
     *
     * @param in the underlying input stream.  Required and cannot be null.
     */
    public NonCloseableInputStream(@NotNull final InputStream in) {
        super(in);
    }

    /**
     * Override close to simply do nothing
     */
    @Override
    public void close() throws IOException {
        // do nothing
    }
}
