/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

import com.l7tech.common.util.HexUtils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.PushbackInputStream;

/**
 * An inputstream tha treads the specified PushbackInputStream and passes it through to to the user,
 * but simlates EOF when the PushbackInputStream returns the specified MIME crlfBoundary.  After this
 * stream returns EOF, the PushbackInputStream will be positioned at the first byte past the crlfBoundary,
 * or at EOF if there were no further bytes past the crlfBoundary.
 */
class SubstringFilterInputStream extends FilterInputStream {
    private final byte[] substring; // crlfBoundary with leading CRLF and leading dashes but no trailing CRLF
    private final PushbackInputStream in; // the main inputstream for the MimeBody
    private final int pushbackSize; // the maximum number of bytes that can be pushed back on this inputstream.
                                   // we will limit block reads to this many bytes

    /** Number of bytes of the boundary that were matched at the end of the last block read. */
    private int substringMatchBytes = 0;

    /** Number of bytes of a failed boundary match that remain to be passed through to the user. */
    private int substringDrainBytes = 0;

    /** Offset into the boundary of boundDrainBytes to pass through to the user. */
    private int substringDrainOff = 0;

    /**
     * If true, we have seen a boundary, have fixed up the position of the input stream, have already
     * drained any boundDrainBytes, have returned all part data, and are now always returning EOF to the caller.
     */
    private boolean atEof = false;

    private Runnable foundHook = null;

    /**
     * Creates a filtered InputStream that will read the specified PushbackInputStream and pass through the results
     * up to (but not including) the next MIME multipart boundary.  At that point, it will return EOF to the client,
     * and leave the input InputStream positioned at the first byte beyond the multipart boundary (that is, the
     * first byte of the headers of the next part, if there is one, or the next message and/or end of the stream
     * if there isn't).
     * <p>
     * This cannot be used on its own for reading the initial preamble of a multipart message if the preamble might
     * be empty -- this is because in that situation, the first byte of the inputstream is positioned at a "--" rather
     * than at a CRLF.  To work around this, unread a CRLF before using this class to consume the preamble. 
     *
     * @param substring  the multipart boundary bytes, including the initial "--" but not including any CRLFs.
     * @param in the PushbackInputStream to wrap
     * @param pushbackSize the maximum amount that this PushbackInputStream is configured to allow pushing back.
     *                     This is used to limit the maximum blocksize the client can read.
     */
    public SubstringFilterInputStream(byte[] substring, PushbackInputStream in, int pushbackSize, Runnable foundHook) {
        super(in);
        if (pushbackSize < 32)
            throw new IllegalArgumentException("pushbackSize must be at least 32 bytes");
        if (in == null)
            throw new NullPointerException("input stream must not be null");
        if (substring == null || substring.length < 1)
            throw new IllegalArgumentException("multipart boundary must be non-null and non-empty");
        this.pushbackSize = pushbackSize;
        this.in = in;
        this.substring = substring;
        this.foundHook = foundHook;
    }

    // This isn't really expected to be the primary mode of use for bulk reading.
    // We'll just translate this into a single-byte block.
    public int read() throws IOException {
        if (atEof)
            return -1;

        byte[] b = new byte[1];
        int got = this.read(b);
        if (got < 1)
            return -1;
        return b[0] < 0 ? b[0] + 256 : b[0];
    }

    // Before Cases:
    //   - We might have one or more bytes of partial boundary match from previous blocks read.
    //     Such bytes have not yet been returned to the client.
    //   - We might have one or mote bytes of failed partial boundary match from previous blocks read.
    //     Such bytes must be returned to the client before any new data is read from the stream.
    // After cases:
    //   - We might have accumulated some (more) partial boundary match bytes that remain inconclusive
    //   - We might have ruled out a partial boundary match, and now have some boundary bytes to return
    public int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        if (len > pushbackSize) len = pushbackSize;

        if (atEof) return -1;

        // Do we have any failed match bytes to drain?
        if (substringDrainBytes > 0) {
            // Return as many as will fit
            if (substringDrainBytes < len) {
                // Short read.  Fill in the front of the user buffer, then clean up invariants and recurse to get the rest.
                System.arraycopy(substring, substringDrainOff, b, off, substringDrainBytes);
                int drained = substringDrainBytes;
                substringDrainBytes = 0;
                substringDrainOff = 0;
                int got = this.read(b, off + drained, len - drained); // recurse to fill out short read
                if (got > 0)
                    return drained + got;
                atEof = true;
                return drained;
            }

            // We have equal or more stuff to drain than will fit all at once.  Return what will fit.
            System.arraycopy(substring, substringDrainOff, b, off, len);
            substringDrainBytes -= len;
            substringDrainOff += len;
            return len;
        }

        // If we reach this point, we have no substringDrainBytes remaining to drain.

        // Are we in mid-match of the substring?
        if (substringMatchBytes > 0) {
            int got = in.read(b, off, len);
            if (got < 1)
                return got;

            // See if match continues
            int boundRemainder = substring.length - substringMatchBytes;
            if (boundRemainder <= got) {
                // check for exact match of rest of boundary
                if (HexUtils.compareArrays(b, off, substring, substringMatchBytes, boundRemainder)) {
                    // Found the rest of the substring.  Unread everything after it.
                    if (foundHook != null) foundHook.run();
                    in.unread(b, off + boundRemainder, got - boundRemainder);
                }

                // Did not find a match.  Unread block, convert matchbytes to drainbytes, and recurse to drain.
                in.unread(b, off, len);
                substringDrainBytes = substringMatchBytes;
                substringDrainOff = 0;
                substringMatchBytes = 0;
                return this.read(b, off, len); // recurse to drain
            }

            // check for prefix match of rest of substring
            if (HexUtils.compareArrays(b, off, substring, substringMatchBytes, got)) {
                // Buffer matched up, so continue searching
                substringMatchBytes += got;
                return this.read(b, off, len); // recurse to check the next block
            }

            // Match ruled out at that position.  Unread block, convert matchbytes to drainbytes, and recurse to drain.
            in.unread(b, off, len);
            substringDrainBytes = substringMatchBytes;
            substringDrainOff = 0;
            substringMatchBytes = 0;
            return this.read(b, off, len); // recurse to drain
        }

        // At this point, we have no boundDrainBytes remaining to drain, and no boundMatchBytes to check.

        // Read the next block
        int got = in.read(b, off, len);
        if (got < 1)
            return got;

        int match = HexUtils.matchSubarrayOrPrefix(b, off, got, substring, 0);
        if (match < 0) {
            // No boundary or start of boundary found -- entire block is data.
            return got;
        }

        int bytesBeforeMatch = (match - off); // always nonnegative
        int bytesAfterMatch = (got - substring.length - bytesBeforeMatch); // might be negative if partial match
        if (bytesAfterMatch >= 0) {
            // We have an exact match within the block.

            // Unread everything after the boundary, adjust input stream, then return everything before it.
            if (bytesAfterMatch > 0)
                in.unread(b, match + substring.length, bytesAfterMatch);

            // Return the bit before the boundary as the final short read
            return bytesBeforeMatch;
        }

        // We have a partial match at the end of the block.
        substringMatchBytes = got - bytesBeforeMatch;

        int igot = this.read(b, match, substringMatchBytes); // recurse to fill out short read
        if (igot > 0)
            return bytesBeforeMatch + igot;
        atEof = true;
        return bytesBeforeMatch;
    }

    public long skip(long n) throws IOException {
        if (n < 1) return 0;
        int toskip = (int)(n > 8192 ? 8192 : n);
        byte[] junk = new byte[toskip];
        return this.read(junk, 0, toskip);
    }

    public void close() throws IOException {
        // Read ourself up to EOF
        byte[] junk = new byte[8192];
        while (this.read(junk, 0, 8192) > 0) {
            // do nothing
        }
        atEof = true;
    }

    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    public boolean markSupported() {
        return false;
    }

    public synchronized void mark(int readlimit) {
    }

}
