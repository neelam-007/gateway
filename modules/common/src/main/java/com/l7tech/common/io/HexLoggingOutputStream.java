package com.l7tech.common.io;

import com.l7tech.util.ResourceUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * Output stream wrapper for debug logging of stream contents.
 *
 * <p>The debug output is in the format: </p>
 *
 * <pre>
 *    000000: 16 03 00 00 D7 01 00 00 D3 03 03 4E 60 24 A1 2F  ...........N`$./
 *    000010: 3E 2A E7 42 A7 6E BB CE 37 85 F0 62 0E C4 94 04  >*.B.n..7..b....
 *    000020: 25 D0 70 4D 01 F0 7F AD 47 91 F0 00 00 4A C0 2C  %.pM....G....J.,
 *    000030: C0 24 C0 0A C0 30 C0 28 C0 14 C0 2B C0 23 C0 09  .$...0.(...+.#..
 *    000040: C0 2F C0 27 C0 13 C0 08 C0 12 C0 07 C0 11 00 9F  ./.'............
 *    000050: 00 A3 00 39 00 38 00 9D 00 35 00 9E 00 A2 00 33  ...9.8...5.....3
 *    000060: 00 32 00 9C 00 2F 00 16 00 13 00 0A 00 05 00 04  .2.../..........
 *    000070: 00 15 00 12 00 09 00 FF 01 00 00 60 00 00 00 14  ...........`....
 *    000080: 00 12 00 00 0F 68 75 67 68 2E 6C 37 74 65 63 68  .....hugh.l7tech
 *    000090: 2E 63 6F 6D 00 0B 00 02 01 00 00 0A 00 20 00 1E  .com......... ..
 * </pre>
 */
public class HexLoggingOutputStream extends OutputStream {

    //- PUBLIC

    public HexLoggingOutputStream( final String prefix,
                                   final Logger logger,
                                   final OutputStream out ) {
        this( new HexLogger(logger,prefix), out);
    }

    @Override
    public void close() throws IOException {
        try {
            out.close();
        } finally {
            ResourceUtils.closeQuietly( hexLogger );
        }
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void write( final byte[] b ) throws IOException {
        if ( traceEnabled() ) {
            trace( b, 0, b.length );
        }
        out.write( b );
    }

    @Override
    public void write( final byte[] b, final int off, final int len ) throws IOException {
        if ( traceEnabled() ) {
            trace( b, off, len );
        }
        out.write( b, off, len );
    }

    @Override
    public void write( final int b ) throws IOException {
        if ( traceEnabled() ) {
            trace( new byte[]{ (byte) b }, 0, 1 );
        }
        out.write( b );
    }

    //- PROTECTED

    protected HexLoggingOutputStream( final HexLogger hexLogger,
                                      final OutputStream out ) {
        this.hexLogger = hexLogger;
        this.out = out;
    }

    //- PRIVATE

    private final HexLogger hexLogger;
    private final OutputStream out;

    private boolean traceEnabled() {
        return hexLogger.traceEnabled();
    }

    private void trace( final byte[] data, int offset, int length ) throws IOException {
        hexLogger.trace( data, offset, length );
    }
}
