package com.l7tech.server.log;

import java.io.PrintStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Locale;
import java.util.Formatter;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.util.ConfigFactory;

/**
 * PrintStream that outputs to the given Logger.
 *
 * <p>Suitable for use to redirect System.out/err to logging.</p>
 *
 * @author Steve Jones
 */
public class LoggingPrintStream extends PrintStream {

    //- PUBLIC

    /**
     * Create a logging print stream that outputs to the given logger.
     *
     * @param logger The logger to use
     * @param level The level to log messages
     */
    public LoggingPrintStream(final Logger logger, final Level level) {
        //noinspection IOResourceOpenedButNotSafelyClosed
        super(new NullOutputStream(), true);

        if ( logger == null ) throw new IllegalArgumentException("logger must not be null");
        if ( level == null ) throw new IllegalArgumentException("level must not be null");
        this.logger = logger;
        this.level = level;
    }

    @Override
    public PrintStream append(char c) {
        process( Character.toString(c), false );
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq) {
        if ( csq != null) {
            process( csq.toString(), false );
        }
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        if ( csq != null) {
            process( csq.subSequence(start, end).toString(), false );
        }
        return this;
    }

    @Override
    public void print(boolean b) {
        process( Boolean.toString(b), false );
    }

    @Override
    public void print(char c) {
        process( Character.toString(c), false );
    }

    @Override
    public void print(double d) {
        process( Double.toString(d), false );
    }

    @Override
    public void print(float f) {
        process( Float.toString(f), false );
    }

    @Override
    public void print(int i) {
        process( Integer.toString(i), false );
    }

    @Override
    public void print(long l) {
        process( Long.toString(l), false );
    }

    @Override
    public void print(Object obj) {
        if ( obj != null ) {
            process( obj.toString(), false );
        }
    }

    @Override
    public void print(char s[]) {
        if ( s != null ) {
            process( new String(s), false );
        }
    }

    @Override
    public void print(String s) {
        if ( s != null ) {
            process( s, false );
        }
    }

    @Override
    public void println() {
        process( "", true );
    }

    @Override
    public void println(boolean x) {
        process( Boolean.toString(x), true );
    }

    @Override
    public void println(char x) {
        process( Character.toString(x), true );
    }

    @Override
    public void println(char x[]) {
        if (x != null) {
            process( new String(x), true );
        }
    }

    @Override
    public void println(double x) {
        process( Double.toString( x ), true );
    }

    @Override
    public void println(float x) {
        process( Float.toString( x ), true );
    }

    @Override
    public void println(int x) {
        process( Integer.toString( x ), true );
    }

    @Override
    public void println(long x) {
        process( Long.toString( x ), true );
    }

    @Override
    public void println(Object x) {
        if ( x != null ) {
            process( x.toString(), true );
        }
    }

    @Override
    public void println(String x) {
        if ( x != null ) {
            process( x, true );
        }
    }

    /**
     * Attempt to process byte as a character.
     */
    @Override
    public void write(int b) {
        print(Character.toString((char)(b & 0xFFFF)));
    }

    /**
     * Attempt to process byte[] as a string
     */
    @Override
    public void write(byte buf[], int off, int len) {
        print(new String(buf, off, len));
    }

    /**
     * Attempt to process byte[] as a string
     */
    @Override
    public void write(byte b[]) throws IOException {
        print(new String(b));
    }

    @Override
    public PrintStream format(String format, Object... args) {
        processFormat( Locale.getDefault(), format, args );
        return this;
    }

    @Override
    public PrintStream format(Locale l, String format, Object... args) {
        processFormat( l, format, args );
        return this;
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        processFormat( Locale.getDefault(), format, args );
        return this;
    }

    @Override
    public PrintStream printf(Locale l, String format, Object... args) {
        processFormat( l, format, args );
        return this;
    }

    //- PRIVATE

    private final Logger logger;
    private final Level level;
    private ThreadLocal<StringBuilder> buffer = new ThreadLocal<StringBuilder>();
    private final int MAX_BUFFER = ConfigFactory.getIntProperty( "com.l7tech.server.log.LoggingPrintStream.bufferSize", 256 );

    private void process( final String text, final boolean newline ) {
        if ( logger.isLoggable( level) ) {
            StringBuilder builder = this.buffer.get();

            if ( newline ) {
                if ( builder != null && builder.length() > 0 ) {
                    builder.append( text );
                    logger.logp( level, logger.getName(), "", builder.toString() );
                    builder.setLength( 0 );
                    if (builder.capacity() > MAX_BUFFER) builder.trimToSize();
                } else {
                    logger.logp( level, logger.getName(), "", text );
                }
            } else {
                if ( builder == null ) {
                    builder = new StringBuilder();
                    buffer.set( builder );
                }
                builder.append( text );
                if ( builder.length() > MAX_BUFFER ) {
                    process( "", true ); // flush buffer
                }
            }
        }
    }

    /**
     * Special handling for format methods to prevent arguments outputting as
     * separate log records.
     */
    private void processFormat(final Locale locale, final String format, Object... args ) {
        if ( logger.isLoggable( level) ) {
            StringBuilder builder = new StringBuilder();
            Formatter formatter = new Formatter(builder);
            formatter.format(locale, format, args);
            process( builder.toString(), false );
        }
    }
}
