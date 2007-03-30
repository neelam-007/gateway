/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.util;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.URLEncoder;
import java.net.URLDecoder;

/**
 * Utility methods for hex encoding and dealing with streams and byte buffers.
 * @noinspection UnnecessaryUnboxing,ForLoopReplaceableByForEach,unchecked
 */
public class HexUtils {
    private static final int DEFAULT_LOCAL_BUFFER_SIZE = 256 * 1024;
    private static final int MIN_LOCAL_BUFFER_SIZE = 32 * 1024; // contract guarantees at least 32k
    private static final int CFG_LOCAL_BUFFER_SIZE = SyspropUtil.getInteger("com.l7tech.common.util.localBufferSize",
                                                                        DEFAULT_LOCAL_BUFFER_SIZE).intValue();

    /** The size of the thread-local buffer returned by getBuffer() and used by slurpStreamLimited. */
    public static final int LOCAL_BUFFER_SIZE = CFG_LOCAL_BUFFER_SIZE >= MIN_LOCAL_BUFFER_SIZE
                                                      ? CFG_LOCAL_BUFFER_SIZE : MIN_LOCAL_BUFFER_SIZE;

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    public static byte[] getMd5Digest(byte[] stuffToDigest) {
        return getMd5().digest(stuffToDigest);
    }

    public static byte[] getMd5Digest(byte[][] stuffToDigest) {
        MessageDigest md = getMd5();
        for (int i = 0; i < stuffToDigest.length; i++) {
            byte[] bytes = stuffToDigest[i];
            md.update(bytes);
        }
        return md.digest();
    }

    public static byte[] getSha1Digest(byte[] stuffToDigest) {
        return getSha1().digest(stuffToDigest);
    }

    /**
     * if the passed string is not null and is longer than the max passed, then it will be truncated
     * in the middle with "..." in place of the truncated portion. the returning string has a length
     * of max or a little shorter. if the incoming string does not exceed desired max or is null, it
     * will simply be returned as is (untouched)
     * @return truncated string
     * @param s string to trim
     * @param max lenght beyond which the string is trimmed
     */
    public static String truncStringMiddle(String s, int max) {
        if (s != null && s.length() > max) {
            s = s.substring(0, ((max/2) - 3)) + "..." + s.substring(s.length() - ((max/2) - 3));
        }
        return s;
    }

    private HexUtils() {}

    private static final char[] hexadecimal = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Get a thread-local buffer for i.e. reading stuff that is at least 32 kilobytes and has not been zeroed
     * since the last person used it.  You may use this method ONLY if you can prove that
     * no method that you call while still using your thread's buffer might also try to use it -- be especially
     * careful when using InputStreams and OutputStreams which may actually be wrappers for other Layer 7 code.
     *
     * @return a thread-local byte buffer that is at least 32k in size and has NOT been zeroed:
     *         it may contain random leftover garbage.  Never null.
     */
    public static byte[] getLocalBuffer() {
        return ((ByteBuffer)localBuffer.get()).array();
    }

    private static ThreadLocal localBuffer = new ThreadLocal() {
        protected Object initialValue() {
            return ByteBuffer.allocate(LOCAL_BUFFER_SIZE);
        }
    };

    public static String encodeBase64(byte[] binaryData) {
        return encodeBase64(binaryData, false);
    }

    public static String encodeBase64(byte[] binaryData, boolean stripWhitespace) {
        return decodeUtf8(Base64.encodeBase64(binaryData, !stripWhitespace)).trim();
    }

    public static byte[] decodeBase64(String s) throws IOException {
        return Base64.decodeBase64(encodeUtf8(s));
    }

    /**
     * Decode a string of Base-64.
     *
     * @param s  the Base64 string to decode
     * @param stripWhitespaceFirst  if true, strip whitespace that might confuse the decoder.  Currently ignored.
     * @return the decoded bytes
     * @noinspection UnusedDeclaration
     * @throws java.io.IOException  if the base64 is not well formed
     */
    public static byte[] decodeBase64(String s, boolean stripWhitespaceFirst) throws IOException {
        // The 'stripWhitespaceFirst' param used to remove whitespace before
        // passing to suns BASE64Decoder.
        //
        // This was necessary since that decoder would silently translate all
        // input to bytes even though it was not valid base64.
        //
        // The commons codec decoder handles whitespace, so this is no longer
        // stripped out.
        return decodeBase64(s);
    }

    public static byte[] encodeUtf8(String text) {
        ByteBuffer buffer = UTF8_CHARSET.encode(text);
        byte[] backingArray = buffer.array();
        byte[] encoded;

        if (backingArray.length == buffer.limit()) {
            encoded = backingArray;
        } else {
            encoded = new byte[buffer.limit()];
            buffer.get(encoded);
        }

        return encoded;
    }

    public static String decodeUtf8(byte[] encodedText) {
        return UTF8_CHARSET.decode(ByteBuffer.wrap(encodedText)).toString();
    }

    /**
     * Convert an int to an 8-digit hex string, with leading zeroes as needed.
     *
     * @param i  the integer to convert to hex
     * @return a hex string with exactly 8 nybbles, ie 000F0238
     */
    public static String to8NybbleHexString(int i) {
        String hs = Integer.toHexString(i).toUpperCase();
        final int hsl = hs.length();
        if (hsl == 8) return hs;
        StringBuffer sb = new StringBuffer();
        while (sb.length() + hsl < 8) sb.append("0");
        sb.append(hs);
        return sb.toString();
    }

    /**
     * Encodes the 128 bit (16 bytes) MD5 into a 32 character String.
     *
     * @param binaryData Array containing the digest
     * @return A String containing the encoded MD5, or empty string if encoding failed
     */
    public static String encodeMd5Digest(byte[] binaryData) {
        if (binaryData == null) return "";
        if (binaryData.length != 16) return "";
        return hexDump(binaryData);
    }

    /**
     * Convert the specified binary data into a string containing hexadecimal digits.
     * Example:  hexDump(new byte[] { (byte)0xAB, (byte)0xCD }).equals("abcd")
     * @param binaryData  the data to dump
     * @return the hex dump of the data
     */
    public static String hexDump(byte[] binaryData) {
        return hexDump(binaryData, 0, binaryData.length);
    }

    public static String hexDump(byte[] binaryData, int off, int len) {
        if (binaryData == null) throw new NullPointerException();
        if (off < 0 || len < 0 || off + len > binaryData.length) throw new IllegalArgumentException();
        char[] buffer = new char[len * 2];
        for (int i = 0; i < len; i++) {
            int low = (binaryData[off + i] & 0x0f);
            int high = ((binaryData[off + i] & 0xf0) >> 4);
            buffer[i*2] = hexadecimal[high];
            buffer[i*2 + 1] = hexadecimal[low];
        }
        return new String(buffer);
    }

    /**
     * Convert a string of hexidecimal digits in the form "FF0E7BCC"... into a byte array.
     * The example would return the byte array { 0xFF, 0x0E, 0x7b, 0xCC }.  This is the inverse
     * of the operation performed by hexDump().
     * @param hexData the string containing the hex data to decode.  May not contain whitespace.
     * @return the decoded byte array, which may be zero length but is never null.
     * @throws IOException if the input string contained characters other than '0'..'9', 'a'..'f', 'A'..'F'.
     */
    public static byte[] unHexDump( String hexData ) throws IOException {
        if ( hexData.length() % 2 != 0 ) throw new IOException( "String must be of even length" );
        byte[] bytes = new byte[hexData.length()/2];
        for ( int i = 0; i < hexData.length(); i+=2 ) {
            int b1 = nybble( hexData.charAt(i) );
            int b2 = nybble( hexData.charAt(i+1) );
            bytes[i/2] = (byte)((b1 << 4) + b2);
        }
        return bytes;
    }

    private static byte nybble( char hex ) throws IOException {
        if ( hex <= '9' && hex >= '0' ) {
            return (byte)(hex - '0');
        } else if ( hex >= 'a' && hex <= 'f' ) {
            return (byte)(hex - 'a' + 10 );
        } else if ( hex >= 'A' && hex <= 'F' ) {
            return (byte)(hex - 'F' + 10 );
        } else {
            throw new IOException( "Invalid hex digit " + hex );
        }
    }

    /**
     * Slurp at most maxLength bytes from stream into a byte array and return an array of the appropriate size.
     * This requires reading into a new byte array of size maxLength, and then copying to a new array with the
     * exact resulting size.
     * If you don't need the the returned array to be the same size as the total amount of data read,
     * you can avoid a copy by supplying your own array to the alternate call
     * {@link #slurpStream(java.io.InputStream, byte[])}.
     * <p/>
     * If the stream contains more than maxLength bytes of data, additional unread information may be left in
     * the stream, and
     * the amount returned will be silently truncated to maxLength.  To detect if this might
     * have been the case, check if the returned amount exactly equals maxLength.  (Unfortunately
     * there is no way to tell this situation apart from the non-lossy outcome of the stream containing exactly
     * maxLength bytes.)
     *
     * @param stream  the stream to read.  Must not be null.
     * @param maxLength  the maximum number of bytes you are willing to recieve
     * @return the newly read array, sized to the amount read or maxLength if the data may have been truncated.
     *         never null.
     * @throws java.io.IOException on IOException
     */
    public static byte[] slurpStream(InputStream stream, int maxLength) throws IOException {
        byte[] buffer = new byte[maxLength];
        int got = slurpStream(stream, buffer);
        byte[] ret = new byte[got];
        System.arraycopy(buffer, 0, ret, 0, got);
        return ret;
    }

    /**
     * Slurp at most {@link #LOCAL_BUFFER_SIZE} bytes of a stream into a byte array and return
     * a new array of the appropriate size.
     * This requires reading into a thread-local buffer array, and then copying to a new array.
     * If you don't need the the returned array to be the same size as the total amount of data read,
     * you can avoid a copy by supplying your own array to the alternate call
     * {@link #slurpStream(java.io.InputStream, byte[])}.
     * <p/>
     * If the stream contains more than {@link #LOCAL_BUFFER_SIZE} bytes of data, additional unread information
     * might be left in the stream, and
     * the amount returned will be silently truncated to {@link #LOCAL_BUFFER_SIZE}.  To detect if this might
     * have been the case, check if the returned amount exactly equals {@link #LOCAL_BUFFER_SIZE}.  (Unfortunately
     * there is no way to tell this situation apart from the non-lossy outcome of the stream containing exactly
     * {@link #LOCAL_BUFFER_SIZE} bytes.)
     *
     * @param stream  the stream to read.  Must not be null.
     * @return the newly read array, sized to the amount read or {@link #LOCAL_BUFFER_SIZE} if the data may have been truncated.
     *         never null.
     * @throws IOException if there was a problem reading the stream
     */
    public static byte[] slurpStreamLocalBuffer(InputStream stream) throws IOException {
        byte[] buffer = getLocalBuffer();
        int got = slurpStream(stream, buffer);
        byte[] ret = new byte[got];
        System.arraycopy(buffer, 0, ret, 0, got);
        return ret;
    }

    /**
     * Slurp a stream into a byte array without doing any copying, and return the number of bytes that
     * were read from stream.  The stream will be read until EOF or until the maximum specified number of bytes have
     * been read.  If you would like the array created for you with the exact size required, and don't mind
     * an extra array copy being involved, use the other form of slurpStream().
     *
     * @param stream the stream to read
     * @param bb the array of bytes in which to read it
     * @return the number of bytes read from the stream, up to bb.length.  If this returns bb.length, the InputStream
     *         may contain additional unread data.
     * @throws java.io.IOException on IOException
     */
    public static int slurpStream(InputStream stream, byte[] bb) throws IOException {
        int remaining = bb.length;
        int offset = 0;
        for (;;) {
            int n = stream.read(bb, offset, remaining);
            if (n < 1)
                return offset;
            offset += n;
            remaining -= n;
            if (remaining < 1)
                return offset;
        }
        /* NOTREACHED */
    }

    /**
     * This ballistic podiatry version of slurpStream will slurp until memory is full.
     * If you wish to limit the size of the returned
     * byte array, use slurpStream64k(InputStream).  If you wish to provide your own buffer to prevent
     * copying, use slurpStream(InputStream, byte[]).
     *
     * @param stream  the stream to slurp
     * @return a byte array containing the entire content of the stream, to EOF.  Never null.
     * @throws IOException if there is an IOException while reading the stream
     * @throws OutOfMemoryError if the stream is too big to fit into memory
     */
    public static byte[] slurpStream(InputStream stream) throws IOException {
        BufferPoolByteArrayOutputStream out = new BufferPoolByteArrayOutputStream(4096);
        try {
            copyStream(stream, out);
            return out.toByteArray();
        } finally {
            out.close();
        }
    }

    /**
     * Compare that treats null as being less than any other Comparable.
     *
     * @param s1 a Comparable object.  May be null.
     * @param s2 a Comparable object.  May be null.
     * @return  a negative integer, zero, or a positive integer as s1
     *		is less than, equal to, or greater than s2; with null comparing
     *      as equal to null and less than any non-null.
     *
     */
    public static int compareNullable(Comparable s1, Comparable s2) {
        if (s1 == null && s2 == null)
            return 0;
        else if (s1 == null)
            return -1;
        else if (s2 == null)
            return 1;
        else
            return s1.compareTo(s2);
    }

    /**
     * Join the specified array of CharSequence into a single StringBuffer, joined with the specified delimiter.
     * @param start   a StringBuffer containing the starting text.  If null, a new StringBuffer will be created.
     * @param delim   delimiter to join with.  may not be null.
     * @param tojoin  array of eg. String to join.  May be null or empty.
     * @return start, after having XdYdZ appended to it where X, Y and Z were memebers of tojoin and d is delim.
     *         Returns a StringBuffer containing the empty string if tojoin is null or empty.
     */
    public static StringBuffer join(StringBuffer start, String delim, CharSequence[] tojoin) {
        if (start == null)
            start = new StringBuffer();
        if (tojoin == null)
            return start;
        for (int i = 0; i < tojoin.length; i++) {
            if (i > 0)
                start.append(delim);
            start.append(tojoin[i]);
        }
        return start;
    }

    /**
     * Join the specified array of CharSequence into a single StringBuffer, joined with the specified delimiter.
     * @param delim   delimiter to join with.  may not be null.
     * @param tojoin  array of eg. String to join. may be null or empty.
     * @return a new StringBuffer containing XdYdZ where X, Y and Z were memebers of tojoin and d is delim.  Returns
     *         a StringBuffer containing the empty string if tojoin is null or empty.
     */
    public static StringBuffer join(String delim, CharSequence[] tojoin) {
        return join(null, delim, tojoin);
    }

    /**
     * Copy all of the in, right up to EOF, into out.  Does not flush or close either stream.
     *
     * @param in  the InputStream to read.  Must not be null.
     * @param out the OutputStream to write.  Must not be null.
     * @return the number bytes copied
     * @throws IOException if in could not be read, or out could not be written
     */
    public static long copyStream(InputStream in, OutputStream out) throws IOException {
        if (in == null || out == null) throw new NullPointerException("in and out must both be non-null");
        byte[] buf = getLocalBuffer();
        int got;
        long total = 0;
        while ((got = in.read(buf)) > 0) {
            out.write(buf, 0, got);
            total += got;
        }
        return total;
    }

    /**
     * Copy all of the in, right up to EOF, into out.  Does not flush or close either stream.
     *
     * @param in  the Reader to read.  Must not be null.
     * @param out the Writer to write.  Must not be null.
     * @return the number bytes copied
     * @throws IOException if in could not be read, or out could not be written
     */
    public static long copyReader(Reader in, Writer out) throws IOException {
        if (in == null || out == null) throw new NullPointerException("in and out must both be non-null");
        char[] buf = new char[4096];
        int got;
        long total = 0;
        while ((got = in.read(buf)) > 0) {
            out.write(buf, 0, got);
            total += got;
        }
        return total;
    }


    /**
     * Compare two InputStreams for an exact match.  This method returns true if and only if both InputStreams
     * produce exactly the same bytes when read from the current position through EOF, and that both reach EOF
     * at the same time.  If so requested, each stream can be closed after reading.  Otherwise, if the comparison
     * succeeds, both streams are left positioned at EOF; if the comparison fails due to a mismatched byte,
     * both streams will be positioned somewhere after the mismatch; and, if the comparison fails due to one of the
     * streams reaching EOF early, the other stream will be left positioned somewhere after its counterpart
     * reached EOF.  The states of both streams is undefined if IOException is thrown.
     *
     * @param left          one of the InputStreams to compare
     * @param closeLeft     if true, left will be closed when the comparison finishes
     * @param right         the other InputStream to compare
     * @param closeRight    if true, right will be closed when the comparison finishes
     * @return              true if both streams produced the same byte stream and ended at the same time;
     *                      false if one of streams ended early or produced a mismatch.
     * @throws IOException  if there was an IOException reading or closing one of the streams.
     *                      the state of the streams is undefined if this method throws.
     */
    public static boolean compareInputStreams(InputStream left, boolean closeLeft,
                                              InputStream right, boolean closeRight) throws IOException
    {
        byte[] lb = BufferPool.getBuffer(4096);
        byte[] rb = BufferPool.getBuffer(4096);
        try {
            boolean match = true;

            for (;;) {
                int gotleft = readFullBlock(left, lb);
                int gotright = readFullBlock(right, rb);
                if (gotleft != gotright) {
                    match = false;
                    break;
                } else if (gotleft < 1)
                    break;
                else if (!ArrayUtils.compareArrays(lb, 0, rb, 0, gotleft)) {
                    match = false;
                    break;
                }
            }

            if (closeLeft) left.close();
            if (closeRight) right.close();

            return match;
        } finally {
            BufferPool.returnBuffer(lb);
            BufferPool.returnBuffer(rb);
        }
    }

    /**
     * Read an entire block from the specified InputStream, if possible,
     * blocking until a full block has been read or EOF is reached.
     *
     * @return the number of bytes read, possibly zero.  If this number is less than the
     *         size of the buffer, EOF has been reached.
     * @param is   the InputStream to read.  Must be non-null.
     * @param buf  the buffer to read into.  Must be non-null and of nonzero length.
     * @throws java.io.IOException  if the underlying read throws IOException
     */
    public static int readFullBlock(InputStream is, byte[] buf) throws IOException {
        int size = 0;
        int remaining = buf.length;

        while (remaining > 0) {
            int got = is.read(buf, size, remaining);
            if (got < 1) break;
            size += got;
            remaining -= got;
        }
        return size;
    }

    private static ThreadLocal md5s = new ThreadLocal();
    private static ThreadLocal sha1s = new ThreadLocal();

    /**
     * Get a thread-local MD5 MessageDigest instance.
     * @return a thread-local digestor
     */
    private static MessageDigest getMd5() {
        MessageDigest md5 = (MessageDigest)md5s.get();
        if (md5 == null) {
            try {
                md5 = MessageDigest.getInstance("MD5");
                md5s.set(md5);
            } catch ( NoSuchAlgorithmException e ) {
                throw new RuntimeException(e);
            }
        }
        md5.reset();
        return md5;
    }

    /**
     * Get a thread-local SHA-1 MessageDigest instance.
     * @return a thread-local digestor
     */
    private static MessageDigest getSha1() {
        MessageDigest sha1 = (MessageDigest)sha1s.get();
        if (sha1 == null) {
            try {
                sha1 = MessageDigest.getInstance("SHA-1");
                sha1s.set(sha1);
            } catch ( NoSuchAlgorithmException e ) {
                throw new RuntimeException(e);
            }
        }
        sha1.reset();
        return sha1;
    }

    public static boolean containsOnlyHex(String arg) {
        if (arg == null || arg.length() != 32) return false;
        String hexmembers = "0123456789abcdef";
        for (int i = 0; i < arg.length(); i++) {
            char toto = arg.charAt(i);
            if (hexmembers.indexOf(toto) == -1) {
                return false;
            }
        }
        return true;
    }

    public static String encodePasswd(String login, String passwd) {
        String toEncode = login + ":" + HttpDigest.REALM + ":" + passwd;
        try {
            return hexDump(getMd5Digest(toEncode.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // Can't happen
        }
    }

    public static String encodePasswd( String login, String passwd, String realm ) {
        String toEncode = login + ":" + realm + ":" + passwd;
        try {
            return hexDump(getMd5Digest(toEncode.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // Can't happen
        }
    }

    /**
     * URL encode the specified string, using UTF-8 to get the bytes of unsafe characters.
     *
     * @param stuff the string to encode.  Must not be null.
     * @return the URL encoded form, using UTF-8 representations of escaped unsafe characters.
     */
    public static String urlEncode(String stuff) {
        try {
            return URLEncoder.encode(stuff, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding not available"); // not possible
        }
    }

    /**
     * Decode the specified URL encoded string, assuming that escaped characters use UTF-8 encoding.
     *
     * @param encoded the string to decode.  Must not be null.
     * @return the decoded string.
     * @throws IOException if the URL encoding is invalid.
     */
    public static String urlDecode(String encoded) throws IOException {
        try {
            return URLDecoder.decode(encoded, "UTF-8");
        } catch (IllegalArgumentException e) {
            throw new CausedIOException(e);
        }            
    }

    /**
     * Replace a list of single bytes in a buffer with corresponding bytes from a second list.
     * For example, with these arguments:
     * <pre>
     *      buffer = { 01 02 03 04 05 06 07 }
     *      find = { 03 05 }
     *      replace = { fe ff }
     * </pre>
     * this method would mutate buffer into the following:
     * <pre>
     *      { 01 02 fe 04 ff 06 07 }
     * </pre>
     *
     * @param big          the buffer whose bytes to replace.
     * @param find         a list of bytes to find.  Must be the same length as replaceWith.
     * @param replaceWith  a list of replacement bytes.  Must be the same length as find.
     */
    public static void replaceBytes(byte[] big, int[] find, int[] replaceWith) {
        for (int i = 0; i < big.length; i++) {
            for (int j = 0; j < find.length; j++) {
                if (big[i] == (byte)(find[j] & 0xFF))
                    big[i] = (byte)replaceWith[j];
            }
        }
    }
}
