package com.l7tech.external.assertions.ipm.server.resources;

import java.io.IOException;

/**
 * Interface implemented by compiled templates.
 * <p/>
 * The actual compiled template instances are dynamically generated by {@link com.l7tech.external.assertions.ipm.server.TemplateCompiler}.
 * <P/>
 * <B>NOTE:</b> This class must be completely standalone.  It can use standard Java classes but must refer to
 * no Layer 7 classes.  This is because only this class by itself is fed to the runtime Java compiler alongside the
 * dynamically-generated subclass sources.
 * <p/>
 * Instances of this class are not threadsafe.  Users are responsible for any needed synchronization/thread-locality.
 */
public abstract class CompiledTemplate {
    protected char buf[] = new char[256];
    protected char[] in;
    protected int ip;
    protected char[] out;
    protected int op;

    private int outputBufferSize = 131020;

    /**
     * Change the maximum output buffer size.  Takes effect on next call to init().
     *
     * @param outputBufferSize the new maximum output buffer size.
     */
    public void setOutputBufferSize(int outputBufferSize) {
        this.outputBufferSize = outputBufferSize;
    }

    /**
     * Initialize an expansion job.  No other thread may use this CompiledTemplate instance
     * during the expansion job.
     * @param in input buffer.  This object takes ownership of this buffer until init() or close() is next called.
     */
    public void init(char[] in) {
        if (buf == null)
            buf = new char[256];
        if (out == null || out.length != outputBufferSize)
            out = new char[outputBufferSize];
        //noinspection AssignmentToCollectionOrArrayFieldFromParameter
        this.in = in;
        ip = 0;
        op = 0;
    }

    /** Free all resources being used by this CompiledTemplate. */
    public void close() {
        buf = null;
        in = null;
        out = null;
    }

    public void expand() throws IOException {
        if (in == null || out == null) throw new IllegalStateException("Must call init() first");
        try {
            doExpand();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IOException("Unable to expand template: ran out of input characters, or filled output or temporary buffer: " + e.getMessage(), e);
        }
    }

    protected abstract void doExpand() throws IOException;

    /** @return the number of characters in the result buffer. */
    public int getResultSize() {
        return op;
    }

    /** @return the result buffer.  Caller must immediately copy the data elsewhere -- the buffer only remains valid until init() is called again. */
    public char[] getResult() {
        //noinspection ReturnOfCollectionOrArrayField
        return out;
    }

    private static final char[] ENTAMP = "&amp;".toCharArray();
    private static final char[] ENTLT = "&lt;".toCharArray();
    private static final char[] ENTGT = "&gt;".toCharArray();

    /**
     * Copy count characters from in to out.
     * Output characters ampersand, greater-than and less-than will be replaced by XML entities.
     * Always either succeeds or throws IOException.
     *
     * @param count  number of characters to copy
     * @throws ArrayIndexOutOfBoundsException if we lack temporary buffer space, run out of input characters, or run out of room in the output buffer.
     */
    protected final void copy(int count) {
        while (count-- > 0) {
            char c = in[ip++];
            switch (c) {
                case '&':
                    write(ENTAMP);
                    break;
                case '<':
                    write(ENTLT);
                    break;
                case '>':
                    write(ENTGT);
                    break;
                default:
                    out[op++] = c;
            }
        }
    }

    /**
     * Copy the specified string into the output buffer.
     * Output characters are NOT XML escaped -- caller is responsible for any needed escaping.
     * Always either succeeds or throws IOException.
     *
     * @param what the string to emit.  Required.
     * @throws ArrayIndexOutOfBoundsException if we lack temporary buffer space, run out of input characters, or run out of room in the output buffer.
     */
    protected final void write(char[] what) {
        final int len = what.length;
        System.arraycopy(what, 0, out, op, len);
        op += len;
    }
}
