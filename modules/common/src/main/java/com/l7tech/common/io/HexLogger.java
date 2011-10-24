package com.l7tech.common.io;

import com.l7tech.util.BufferPool;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.HexUtils;
import com.l7tech.util.TruncatingInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logs bytes in a hex dump format.
 *
 * <p>The output is in the format: </p>
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
 *
 */
public class HexLogger implements Closeable {

    //- PUBLIC

    /**
     * Create a HEX logger for the given logger and prefix.
     *
     * <p>This will create a HEX logger using the default buffer size.</p>
     *
     * @param logger The logger to use
     * @param prefix The prefix to use
     */
    public HexLogger( final Logger logger,
                      final String prefix ) {
        this( logger, prefix, getBufferSize(), getFlushOnUse() );
    }

    /**
     * Create a HEX logger for the given logger and prefix.
     *
     * <p>This will create a HEX logger using the given buffer size. Note that
     * the buffer required for constructing the logged output will be
     * approximately ten times larger than the size of the binary buffer.</p>
     *
     * @param logger The logger to use
     * @param prefix The prefix to use
     * @param bufferSize The size of the output buffer
     * @param flushOnUse Should the output be flushed on every trace invocation?
     */
    public HexLogger( @NotNull final Logger logger,
                      @NotNull final String prefix,
                      final int bufferSize,
                      final boolean flushOnUse ) {
        this.logger = logger;
        this.prefix = prefix;
        this.bufferSize = bufferSize;
        this.flushOnUse = flushOnUse;
        this.buffer = BufferPool.getBuffer( this.bufferSize );
        this.hexBuilder = new StringBuilder( this.bufferSize * 5 );
    }

    /**
     * Is tracing currently enabled for this logger.
     *
     * @return True if enabled
     */
    public boolean traceEnabled() {
        return logger.isLoggable( Level.FINEST );
    }

    /**
     * Trace the given data.
     *
     * @param data The data to trace (required)
     * @throws IllegalStateException If this logger has been closed
     */
    public void trace( final byte[] data ) {
        trace( data, 0, data.length );
    }

    /**
     * Trace the given data.
     *
     * @param data The data to trace (required)
     * @param offset The offset into the array
     * @param length The length of data to trace
     * @throws IllegalStateException If this logger has been closed
     */
    public void trace( final byte[] data, int offset, int length ) {
        acceptData( data, offset, length );
        if ( flushOnUse && bufferOffset >= 16 ) { // For now we refuse to flush less than one line of output
            flushBuffer( buffer, 0, bufferOffset, null, 0, 0 );
            bufferOffset = 0;
        }
    }

    /**
     * Trace the given informational message.
     *
     * @param message The message to trace
     * @param params The message parameters
     */
    public void traceInfo( final String message, final Object... params ) {
        if ( traceEnabled() ) {
            logger.log( Level.FINEST, message, params  );
        }
    }

    /**
     * Close this logger and release the underlying buffer.
     *
     * <p>It is an error to trace data after the logger has been closed. Any
     * invocation of close after the first will be ignored.</p>
     */
    @Override
    public void close() {
        if ( buffer != null ) {
            if ( bufferOffset > 0 ) {
                flushBuffer( buffer, 0, bufferOffset, null, 0, 0 );
            }
            BufferPool.returnBuffer( buffer );
            buffer = null;
        }
    }

    //- PROTECTED

    /**
     * Override to intercept traced data.
     *
     * @param hex The trace output.
     */
    protected void outputHex( final String hex ) {
        logger.log( Level.FINEST, prefix + hex );
    }

    //- PRIVATE

    private static final int DEFAULT_BUFFER_SIZE = 4096; // a supported size for pooled buffers
    private static final int MAX_OFFSET = 0xFFFFFF; // wraps to zero on the 16MB threshold

    private final Logger logger;
    private final String prefix;
    private final int bufferSize;
    private final boolean flushOnUse;
    private byte[] buffer; // note that size may exceed bufferSize
    private int bufferOffset; // position for next buffer write
    private int streamOffset;
    private int partialLineOffset;
    private final StringBuilder hexBuilder; // this will be about 10x the size of the binary data

    private void acceptData(  final byte[] data, final int offset, final int length  ) {
        if ( buffer == null ) throw new IllegalStateException( "Logger closed." );
        if ( length <= ( bufferSize - bufferOffset ) ) {
            // Add to buffer
            System.arraycopy( data, offset, buffer, bufferOffset, length );
            bufferOffset += length;
        } else {
            // flush partial buffer
            int consumed = 0;
            if ( bufferOffset > 0 ) {
                consumed += flushBuffer( buffer, 0, bufferOffset, data, offset, length );
                bufferOffset = 0;
            }

            // flush any full size chunks of data
            for ( int i=offset+consumed; i<length-bufferSize; i+=bufferSize ) {
                flushBuffer( data, i, bufferSize, null, 0, 0 );
                consumed += bufferSize;
            }

            // put remaining in buffer
            if ( length - consumed > 0 ) {
                acceptData( data, offset + consumed, length - consumed );
            }
        }
    }

    private int flushBuffer( final byte[] buffer,
                             final int bufferOffset,
                             final int bufferLength,
                             @Nullable final byte[] extra,
                             final int extraOffset,
                             final int extraLength ) {
        assert extra==null || bufferLength + extraLength >= bufferSize :
                "All flushes with extra data should be of buffer size";

        hexBuilder.setLength( 0 );

        final InputStream dataToFlush = extra==null || extraLength==0 ?
                new ByteArrayInputStream( buffer, bufferOffset, bufferLength ) :
                new TruncatingInputStream( new SequenceInputStream(
                        new ByteArrayInputStream( buffer, bufferOffset, bufferLength ),
                        new ByteArrayInputStream( extra, extraOffset, extraLength ) ), (long)bufferSize);

        try {
            int lineLength;
            byte[] line = new byte[16];
            while ( (lineLength = dataToFlush.read( line, 0, 16 - partialLineOffset )) > -1 ) {
                final int thisLinesOffset = partialLineOffset;
                // sequence inputstream does a short read when switching streams
                // so try to read some more
                if ( lineLength < (16 - partialLineOffset) ) {
                    int read = dataToFlush.read( line, lineLength, (16 - partialLineOffset)-lineLength );
                    if ( read > 0 ) lineLength += read;
                    if ( lineLength < (16 - partialLineOffset) ) {
                        partialLineOffset += lineLength;
                    } else {
                        partialLineOffset = 0;
                    }
                } else {
                    partialLineOffset = 0;
                }

                final String offset = Integer.toHexString( streamOffset );
                pad( hexBuilder, '0', 6, offset.length() );
                hexBuilder.append( offset.toUpperCase() );
                hexBuilder.append( ':' );
                hexBuilder.append( ' ' );
                if ( thisLinesOffset > 0 ) {
                    pad( hexBuilder, ' ', 3*thisLinesOffset, 0 );
                }
                HexUtils.hexAppend( hexBuilder, line, 0, lineLength, true, ' ' );
                pad( hexBuilder, ' ', 47 - (thisLinesOffset*3), (lineLength*3)-1 );
                hexBuilder.append( ' ' );
                hexBuilder.append( ' ' );
                if ( thisLinesOffset > 0 ) {
                    pad( hexBuilder, ' ', thisLinesOffset, 0 );
                }
                HexUtils.asciiAppend( hexBuilder, line, 0, lineLength );
                if ( lineLength==16 || thisLinesOffset>0 ) hexBuilder.append( '\n' );

                if (partialLineOffset==0) streamOffset += 16;
                if ( streamOffset >= MAX_OFFSET ) {
                    streamOffset = 0;
                }
            }
        } catch ( IOException e ) {
            // unexpected for byte array input streams
            logger.log( Level.WARNING, "Error flushing stream trace.", e );
        }

        if ( hexBuilder.length() > 0 ) {
            if ( (int) hexBuilder.charAt( hexBuilder.length() - 1 ) == (int) '\n' ) {
                hexBuilder.setLength( hexBuilder.length()-1 );
            }
            outputHex( hexBuilder.toString() );
        }
        hexBuilder.setLength( 0 );

        int extraBufferUsed = bufferSize - bufferLength;
        if ( extraBufferUsed > extraLength ) {
            extraBufferUsed = extraLength;
        }

        return extraBufferUsed;
    }

    private void pad( final StringBuilder hexBuilder,
                      final char c,
                      final int width,
                      final int reserved ) {
        for ( int i=width-reserved; i>0; i-- ) {
            hexBuilder.append( c );
        }
    }

    private static int getBufferSize() {
        return ConfigFactory.getIntProperty( "com.l7tech.common.io.HexLogger.bufferSize", DEFAULT_BUFFER_SIZE );
    }

    private static boolean getFlushOnUse() {
        return ConfigFactory.getBooleanProperty( "com.l7tech.common.io.HexLogger.flushOnFlush", true );
    }
}
