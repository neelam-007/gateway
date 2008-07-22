/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.io;

import com.l7tech.util.BufferPool;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream filter that will sniff and discard any XML Byte Order Mark at the beginning of the wrapped InputStream.
 *
 */
public class ByteOrderMarkInputStream extends FilterInputStream {
/*  Standard Java encoding names:
ASCII                   American Standard Code for Information Interchange
Cp1252                  Windows Latin-1
ISO8859_1               ISO 8859-1, Latin alphabet No. 1
UnicodeBig              Sixteen-bit Unicode Transformation Format, big-endian byte order, with byte-order mark
UnicodeBigUnmarked      Sixteen-bit Unicode Transformation Format, big-endian byte order
UnicodeLittle           Sixteen-bit Unicode Transformation Format, little-endian byte order, with byte-order mark
UnicodeLittleUnmarked   Sixteen-bit Unicode Transformation Format, little-endian byte order
UTF8                    Eight-bit Unicode Transformation Format
UTF-16                  Sixteen-bit Unicode Transformation Format, byte order specified by a mandatory initial byte-order mark
*/
    public static final String ASCII = "ASCII";
    public static final String CP1252 = "Cp1252";
    public static final String ISO8859_1 = "ISO8859_1";
    public static final String UNICODE_BIG = "UnicodeBig";
    public static final String UNICODE_BIG_UNMARKED = "UnicodeBigUnmarked";
    public static final String UNICODE_LITTLE = "UnicodeLittle";
    public static final String UNICODE_LITTLE_UNMARKED = "UnicodeLittleUnmarked";
    public static final String UTF8 = "UTF8";
    public static final String UTF16 = "UTF-16";  // mandatory initial byte-order mark

    // Probably unsupported encodings, using best guess -- will let them take UnsupportedEncodingException later
    public static final String UTF32 = "UTF-32";  // probably mandatory BOM
    public static final String SCSU = "SCSU";     // Standard Compression Scheme for Unicode, pretty much only used inside Reuters

    public static final int MAX_BOM_LENGTH = 4; // longest byte order mark is 4 bytes long (acutally UTF-7 can go up to 5, but who's counting)

    private byte[] firstBlock = BufferPool.getBuffer(MAX_BOM_LENGTH);  // Must be non-null if pendingBytes > 0
    private int pendingOffset = 0;       // Offset into firstBlock of next pending byte to return.  Meaningless unless pendingBytes > 0
    private int pendingBytes = 0;        // If positive, fill reads from firstBlock instead of delegate
    private boolean pendingEof = false;  // If true, return EOF when pendingBytes < 1
    private boolean srippingBom = false;
    private final String encoding;

    /**
     * Wrap the specified InputStream.  The start of the wrapped stream will be immediately sniffed for a Byte
     * Order Mark.
     *
     * @param in            the stream to wrap.  Must not be null.
     * @throws IOException  if there is a problem reading the start of the wrapped stream to look for a BOM
     */
    public ByteOrderMarkInputStream(InputStream in) throws IOException {
        super(in);
        if (in == null) throw new NullPointerException();
        if (firstBlock.length < MAX_BOM_LENGTH) throw new IllegalStateException();

        int remaining = firstBlock.length;
        int off = 0;
        while (remaining > 0) {
            int got = super.read(firstBlock, off, remaining);
            assert got != 0;
            assert got <= remaining;
            if (got < 0) {
                pendingEof = true;
                break;
            }
            off += got;
            remaining -= got;
            pendingBytes += got;
        }

        encoding = setupEncoding();
    }

    /**
     * Get the encoding, if any, that was sniffed from the underlying stream's
     * byte order mark when this wrapper input stream was first created.
     *
     * @return a Java encoding name (but not one guaranteed to be available on the current system), or null if
     *         a recognized byte order mark was not found.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * Check whether this wrapper is stripped a recognized byte order mark (BOM).
     *
     * @return true if a recognized byte order mark was found at the start of the wrapped stream and is being stripped;
     *         false if either no BOM was recognized, or the indicated encoding required that we preserve the BOM.
     */
    public boolean isSrippingBom() {
        return srippingBom;
    }

    /**
     * Examine the bytes pointed to by offset and length and, if they form a valid Unicode Byte Order Mark, return
     * the corresponding Java encoding.
     */
    private String setupEncoding() {
        if (pendingBytes < 2) return null; // Not enough bytes to be a BOM

        final int byte1 = 0xFF & (int)firstBlock[0];
        final int byte2 = 0xFF & (int)firstBlock[1];

        final int skip;
        final String encoding;
        switch (byte1) {

            case 0xEF:
                if (byte2 != 0xBB || pendingBytes < 3 || (0xFF & firstBlock[2]) != 0xBF)
                    return null;

                encoding = UTF8;
                skip = 3;
                break;

            case 0xFE:
                if (byte2 != 0xFF)
                    return null;

                encoding = UNICODE_BIG_UNMARKED;
                skip = 2;
                break;

            case 0xFF:
                if (byte2 != 0xFE)
                    return null;

                if (pendingBytes < 4 || (0xFF & firstBlock[2]) != 0x00 || (0xFF & firstBlock[3]) != 0x00) {
                    encoding = UNICODE_LITTLE_UNMARKED;
                    skip = 2;
                    break;
                }

                encoding = UTF32;
                skip = 0; // do not strip BOM -- if UTF-32 is even supported, a BOM would have to be mandatory
                break;

            case 0x00:
                if (byte2 != 0x00 || pendingBytes < 4 || (0xFF & firstBlock[2]) != 0xFE || (0xFF & firstBlock[3]) != 0xFF)
                    return null;
                encoding = UTF32;
                skip = 0; // do not strip BOM -- if UTF-32 is even supported, a BOM would have to be mandatory
                break;

            case 0x0E:
                if (byte2 != 0xFE || pendingBytes < 3 || (0xFF & firstBlock[2]) != 0xFF)
                    return null;

                encoding = SCSU;
                skip = 0; // do not strip BOM -- it's probably going to be needed by whoever tries to read this stuff
                break;

            case 0x2B:
                // Might be UTF-7
                // TODO UTF-7 support.  For now we treat it as unrecognized binary data
                return null;

            case 0xDD:
                // Might be UTF-EBCDIC
                // TODO UTF-EBCDIC support.   For now we treat it as unrecognized binary data
                return null;

            case 0xFB:
                // Might be BOCU-1
                // TODO BOCU-1 support.   For now we treat it as unrecognized binary data
                return null;

            default:
                return null;
        }

        pendingOffset += skip;
        pendingBytes -= skip;
        srippingBom = skip > 0;
        return encoding;
    }

    public int read() throws IOException {
        if (firstBlock == null) throw new IllegalStateException("ByteOrderMarkInputStream has already been closed");
        if (pendingBytes < 1) {
            if (pendingEof) return -1;
            return super.read();
        }
        pendingBytes--;
        return firstBlock[pendingOffset++];
    }

    public int read(byte b[], int off, int len) throws IOException {
        if (firstBlock == null) throw new IllegalStateException("ByteOrderMarkInputStream has already been closed");
        if (pendingBytes < 1) {
            if (pendingEof) return -1;
            return super.read(b, off, len);
        }

        if (len > pendingBytes) len = pendingBytes;
        System.arraycopy(firstBlock, pendingOffset, b, off, len);
        pendingBytes -= len;
        pendingOffset += len;
        return len;
    }

    public int available() throws IOException {
        if (pendingBytes < 1) {
            if (pendingEof) return 0;
            return super.available();
        }

        return pendingBytes + super.available();
    }

    public synchronized void mark(int readlimit) {
        // Noop
    }

    public boolean markSupported() {
        return false;
    }

    public void close() throws IOException {
        if (firstBlock != null) {
            BufferPool.returnBuffer(firstBlock);
            firstBlock = null;
        }
        super.close();
    }
}
