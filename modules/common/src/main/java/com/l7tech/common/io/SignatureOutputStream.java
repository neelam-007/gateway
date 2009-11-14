package com.l7tech.common.io;

import java.io.OutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.security.Signature;
import java.security.SignatureException;

/**
 * An output stream that updates a Signature object with all written data and optionally forwards to a delegate stream as well.
 */
public class SignatureOutputStream extends FilterOutputStream {
    private final Signature signature;

    /**
     * Create a SignatureOutputStream that will pass input to the specified signature.
     *
     * @param signature a Signature object, already initialized for either signing or verification.  Required.
     */
    public SignatureOutputStream(Signature signature) {
        this(null, signature);
    }

    /**
     * Create a SignatureOutputStream that will pass input to the specified delegate, and update the specified
     * signature object with all data that is written.
     *
     * @param delegate an OutputStream to forward all writes to, or null to update the signature only.
     * @param signature a Signature object, already initialized for either signing or verification.  Required.
     */
    public SignatureOutputStream(OutputStream delegate, Signature signature) {
        super(delegate != null ? delegate : new NullOutputStream());
        this.signature = signature;
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);
        try {
            signature.update((byte)b);
        } catch (SignatureException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        try {
            signature.update(b, off, len);
        } catch (SignatureException e) {
            throw new IOException(e);
        }
    }

    /**
     * Returns the signature bytes of all the data written.
     *
     * <p>This method simply forwards to {@link Signature#sign()}.</p>
     *
     * @return the signature bytes of the signing operation's result.  Never null.
     *
     * @throws SignatureException if the signature fails.
     */
    public byte[] sign() throws SignatureException {
        return signature.sign();
    }


    /**
     * Finishes the signature operation and stores the resulting signature
     * bytes in the provided buffer.
     *
     * <p>This method simply forwards to {@link Signature#sign(byte[], int, int)}.</p>
     *
     * @param buf buffer to hold signature result.  Required.
     * @param off offset into buffer where signature will be stored.
     * @param len number of bytes within within buffer available to hold signature.
     * @return the number of bytes that were written to buf.
     * @throws SignatureException if the signing fails, including if len was insufficient to hold the signature.
     */
    public int sign(byte[] buf, int off, int len) throws SignatureException {
        return signature.sign(buf, off, len);
    }
}
