package com.l7tech.common.io;

import java.io.OutputStream;
import java.io.IOException;

/**
 * Output stream wrapper that does not close the underlying output stream.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class NonCloseableOutputStream extends OutputStream {

    //- PUBLIC

    /**
     *
     */
    public NonCloseableOutputStream(OutputStream delegate) {
        if(delegate==null) throw new IllegalArgumentException("delegate must not be null");
        this.delegate = delegate;
    }

    /**
     * Get the wrapped output stream.
     *
     * @return the underlying outputstream.
     */
    public OutputStream getOutputStream() {
        return delegate;
    }

    /**
     * Close the wrapped stream.
     *
     * @throws IOException if the underlying stream throws.
     */
    public void reallyClose() throws IOException {
        delegate.close();
    }

    /**
     * This call to close is NOT passed to the wrapped stream.
     *
     * <p>This method will invoke flush() on the underlying stream.</p>
     *
     * @see #flush()
     * @see #reallyClose()
     */
    public void close() throws IOException {
        flush();
    }

    public void flush() throws IOException {
        delegate.flush();
    }

    public void write(byte[] b) throws IOException {
        delegate.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        delegate.write(b, off, len);
    }

    public void write(int b) throws IOException {
        delegate.write(b);
    }

    //- PRIVATE

    private final OutputStream delegate;
}
