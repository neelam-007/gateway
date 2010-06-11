package com.l7tech.common.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream that always throws the given IOException.
 */
public class IOExceptionThrowingOutputStream extends OutputStream {

    //- PUBLIC

    public IOExceptionThrowingOutputStream( final IOException ioe ) {
        this.ioe = ioe;
    }

    @Override
    public void write( final byte[] b ) throws IOException {
        super.write( b );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void write( final byte[] b, final int off, final int len ) throws IOException {
        super.write( b, off, len );    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void write( final int b ) throws IOException {
        throw ioe;
    }

    @Override
    public void flush() throws IOException {
        throw ioe;
    }

    @Override
    public void close() throws IOException {
        throw ioe;
    }

    //- PRIVATE

    private final IOException ioe;
}
