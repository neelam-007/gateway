/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.mime;

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
class MimeBoundaryTerminatedInputStream extends FilterInputStream {
    private final byte[] crlfBoundary; // crlfBoundary with leading CRLF and leading dashes but no trailing CRLF
    private final PushbackInputStream in; // the main inputstream for the MultipartMessage
    private final int pushbackSize; // the maximum number of bytes that can be pushed back on this inputstream.
                                   // we will limit block reads to this many bytes

    /** Number of bytes of the boundary that were matched at the end of the last block read. */
    private int boundMatchBytes = 0;

    /** Number of bytes of a failed boundary match that remain to be passed through to the user. */
    private int boundDrainBytes = 0;

    /** Offset into the boundary of boundDrainBytes to pass through to the user. */
    private int boundDrainOff = 0;

    /**
     * If true, we have seen a boundary, have fixed up the position of the input stream, have already
     * drained any boundDrainBytes, have returned all part data, and are now always returning EOF to the caller.
     */
    private boolean atEof = false;

    /**
     * If true, we have finished reading the very last part in the overall multipart message per the "--" marker.
     * If the main InputStream has not reached EOF, it is positioned at the first byte following the multipart
     * message.
     */
    private boolean lastPartProcessed = false;

    /** Hook invoked when current part ends. */
    private Runnable endOfStreamHook = null;

    private Runnable finalBoundaryHook = null;

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
     * @param rawBoundary  the multipart boundary bytes, including the initial "--" but not including any CRLFs.
     * @param in the PushbackInputStream to wrap
     * @param pushbackSize the maximum amount that this PushbackInputStream is configured to allow pushing back.
     *                     This is used to limit the maximum blocksize the client can read.
     */
    public MimeBoundaryTerminatedInputStream(byte[] rawBoundary, PushbackInputStream in, int pushbackSize) {
        super(in);
        if (pushbackSize < 32)
            throw new IllegalArgumentException("pushbackSize must be at least 32 bytes");
        if (in == null)
            throw new NullPointerException("input stream must not be null");
        if (rawBoundary == null || rawBoundary.length < 1)
            throw new IllegalArgumentException("multipart boundary must be non-null and non-empty");
        this.pushbackSize = pushbackSize;
        this.in = in;
        crlfBoundary = new byte[rawBoundary.length + 2];
        System.arraycopy(rawBoundary, 0, crlfBoundary, 2, rawBoundary.length);
        crlfBoundary[0] = '\r';
        crlfBoundary[1] = '\n';
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
        if (boundDrainBytes > 0) {
            // Return as many as will fit
            if (boundDrainBytes < len) {
                // Short read.  Fill in the front of the user buffer, then clean up invariants and recurse to get the rest.
                System.arraycopy(crlfBoundary, boundDrainOff, b, off, boundDrainBytes);
                int drained = boundDrainBytes;
                boundDrainBytes = 0;
                boundDrainOff = 0;
                int got = this.read(b, off + drained, len - drained); // recurse to fill out short read
                if (got > 0)
                    return drained + got;
                atEof = true;
                return drained;
            }

            // We have equal or more stuff to drain than will fit all at once.  Return what will fit.
            System.arraycopy(crlfBoundary, boundDrainOff, b, off, len);
            boundDrainBytes -= len;
            boundDrainOff += len;
            return len;
        }

        // If we reach this point, we have no boundDrainBytes remaining to drain.

        // Are we in mid-match of the boundary?
        if (boundMatchBytes > 0) {
            int got = in.read(b, off, len);
            if (got < 1)
                throw new IOException("Multipart stream ended before a terminating boundary was encountered");

            // See if match continues
            int boundRemainder = crlfBoundary.length - boundMatchBytes;
            if (boundRemainder <= got) {
                // check for exact match of rest of boundary
                if (HexUtils.compareArrays(b, off, crlfBoundary, boundMatchBytes, boundRemainder)) {
                    // Found the rest of the boundary.  Unread everything after it, adjust input stream, then return EOF.

                    in.unread(b, off + boundRemainder, got - boundRemainder);
                    eatTrailingCrlfOrDoubleDash();
                    atEof = true;
                    return -1;
                }

                // Did not find a match.  Unread block, convert matchbytes to drainbytes, and recurse to drain.
                in.unread(b, off, len);
                boundDrainBytes = boundMatchBytes;
                boundDrainOff = 0;
                boundMatchBytes = 0;
                return this.read(b, off, len); // recurse to drain
            }

            // check for prefix match of rest of boundary
            if (HexUtils.compareArrays(b, off, crlfBoundary, boundMatchBytes, got)) {
                // Buffer matched up, so continue searching
                boundMatchBytes += got;
                return this.read(b, off, len); // recurse to check the next block
            }

            // Match ruled out at that position.  Unread block, convert matchbytes to drainbytes, and recurse to drain.
            in.unread(b, off, len);
            boundDrainBytes = boundMatchBytes;
            boundDrainOff = 0;
            boundMatchBytes = 0;
            return this.read(b, off, len); // recurse to drain
        }

        // At this point, we have no boundDrainBytes remaining to drain, and no boundMatchBytes to check.

        // Read the next block
        int got = in.read(b, off, len);
        if (got < 1)
            throw new IOException("Multipart stream ended before a terminating boundary was encountered");

        int match = HexUtils.matchSubarrayOrPrefix(b, off, got, crlfBoundary, 0);
        if (match < 0) {
            // No boundary or start of boundary found -- entire block is data.
            return got;
        }

        int bytesBeforeMatch = (match - off); // always nonnegative
        int bytesAfterMatch = (got - crlfBoundary.length - bytesBeforeMatch); // might be negative if partial match
        if (bytesAfterMatch >= 0) {
            // We have an exact match within the block.

            // Unread everything after the boundary, adjust input stream, then return everything before it.
            if (bytesAfterMatch > 0)
                in.unread(b, match + crlfBoundary.length, bytesAfterMatch);
            eatTrailingCrlfOrDoubleDash();
            atEof = true;

            // Return the bit before the boundary as the final short read
            return bytesBeforeMatch;
        }

        // We have a partial match at the end of the block.
        boundMatchBytes = got - bytesBeforeMatch;

        int igot = this.read(b, match, boundMatchBytes); // recurse to fill out short read
        if (igot > 0)
            return bytesBeforeMatch + igot;
        atEof = true;
        return bytesBeforeMatch;
    }

    /** Consumes the inputstream up to the next CRLF.  If it sees a double dash, assumes the multipart has finished. */
    private void eatTrailingCrlfOrDoubleDash() throws IOException {
        boolean seencr = false;
        boolean seenlf = false;
        boolean sawonedash = false;

        int c;
        CHARLOOP: while ((c = in.read()) >= 0) {
            switch (c) {
                case '\r':
                    seencr = true;
                    if (seenlf) break CHARLOOP;
                    sawonedash = false;
                    break;
                case '\n':
                    seenlf = true;
                    if (seencr) break CHARLOOP;
                    sawonedash = false;
                    break;
                case '-':
                    if (sawonedash)
                        lastPartProcessed = true;
                    else
                        sawonedash = true;
                    break;
                default:
                    sawonedash = false;
            }
        }
        if (lastPartProcessed && finalBoundaryHook != null)
            finalBoundaryHook.run();
        else if (endOfStreamHook != null)
            endOfStreamHook.run();
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

    /**
     * @return true iff. we have finished reading the very last part in the overall multipart message,
     * as understood by having seen the final "--" marker.
     * If the main InputStream has not reached EOF, it is positioned at the first byte following the multipart
     * message.
     */
    public boolean isLastPartProcessed() {
        return lastPartProcessed;
    }

    /**
     * If one of these is set, it will be notified when the stream ends after encountering the multipart
     * boundary at the end of the current part.
     * <p>
     * At this point the main InputStream will be left pointing to the next byte just beyond the boundary's
     * CRLF; or equivalently, according to MIME, pointing at the first byte of the next part's MIME headers.
     * <p>
     * @param endOfStreamHook a callback hook that will be invoked when the current part ends.
     */
    public void setEndOfStreamHook(Runnable endOfStreamHook) {
        this.endOfStreamHook = endOfStreamHook;
    }

    /**
     * If one of these is set, it will be notified (<i>INSTEAD</i> of the endOfStreamHook, if any) when the stream
     * ends after seeing the final boundary, with the terminating "--" suffix indicating the end of the final Part in
     * the multipart message.
     * <p>
     * At this point the main InputStream will be left pointing at the next byte just beyond the final delimiter's
     * CRLF, or at EOF if there was nothing after the final CRLF.
     * <p>
     * @param finalBoundaryHook a callback hook that will be invoked when the current part ends if the
     *                          current part turns out to be the last part in the multipart message.
     */
    public void setFinalBoundaryHook(Runnable finalBoundaryHook) {
        this.finalBoundaryHook = finalBoundaryHook;
    }
}
