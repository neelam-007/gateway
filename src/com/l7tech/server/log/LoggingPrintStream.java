package com.l7tech.server.log;

import java.io.PrintStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Locale;
import java.util.Formatter;

import com.l7tech.common.io.NullOutputStream;

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
        process( Character.toString(c) );
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq) {
        if ( csq != null) {
            process( csq.toString() );
        }
        return this;
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        if ( csq != null) {
            process( csq.subSequence(start, end).toString() );
        }
        return this;
    }

    @Override
    public void print(boolean b) {
        process( Boolean.toString(b) );
    }

    @Override
    public void print(char c) {
        process( Character.toString(c) );
    }

    @Override
    public void print(double d) {
        process( Double.toString(d) );
    }

    @Override
    public void print(float f) {
        process( Float.toString(f) );
    }

    @Override
    public void print(int i) {
        process( Integer.toString(i) );
    }

    @Override
    public void print(long l) {
        process( Long.toString(l) );
    }

    @Override
    public void print(Object obj) {
        if ( obj != null ) {
            process( obj.toString() );
        }
    }

    @Override
    public void print(char s[]) {
        if ( s != null ) {
            process( new String(s) );
        }
    }

    @Override
    public void print(String s) {
        if ( s != null ) {
            process( s );
        }
    }

    @Override
    public void println() {
        // no
    }

    @Override
    public void println(boolean x) {
        print(x);
    }

    @Override
    public void println(char x) {
        print(x);
    }

    @Override
    public void println(char x[]) {
        print(x);
    }

    @Override
    public void println(double x) {
        print(x);
    }

    @Override
    public void println(float x) {
        print(x);
    }

    @Override
    public void println(int x) {
        print(x);
    }

    @Override
    public void println(long x) {
        print(x);
    }

    @Override
    public void println(Object x) {
        print(x);
    }

    @Override
    public void println(String x) {
        print(x);
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

    private void process(final String text) {
        if ( logger.isLoggable( level) ) {
            logger.logp( level, logger.getName(), "", text );
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
            process( builder.toString() );
        }
    }
}
