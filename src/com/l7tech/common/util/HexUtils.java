/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * 
 */

package com.l7tech.common.util;

import com.l7tech.policy.assertion.credential.http.HttpDigest;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for hex encoding.
 * User: mike
 * Date: Jul 15, 2003
 * Time: 2:31:32 PM
 */
public class HexUtils {

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

    private HexUtils() {}

    private static final char[] hexadecimal = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private static Pattern whitespacePattern = Pattern.compile("\\s+", Pattern.MULTILINE | Pattern.DOTALL);

    private static ThreadLocal localDecoder = new ThreadLocal() {
        protected Object initialValue() {
            return new BASE64Decoder();
        }
    };
    private static ThreadLocal localEncoder = new ThreadLocal() {
        protected Object initialValue() {
            return new BASE64Encoder();
        }
    };

    public static String encodeBase64(byte[] binaryData) {
        return encodeBase64(binaryData, false);
    }

    public static String encodeBase64(byte[] binaryData, boolean stripWhitespace) {
        BASE64Encoder encoder = (BASE64Encoder)localEncoder.get();
        String s = encoder.encode( binaryData );
        if (stripWhitespace) {
            Matcher matcher = whitespacePattern.matcher(s);
            return matcher.replaceAll("");
        }
        return s;
    }

    public static byte[] decodeBase64(String s) throws IOException {
        return decodeBase64(s, false);
    }

    public static byte[] decodeBase64(String s, boolean stripWhitespaceFirst) throws IOException {
        if (stripWhitespaceFirst) {
            Matcher matcher = whitespacePattern.matcher(s);
            s = matcher.replaceAll("");
        }
        BASE64Decoder decoder = (BASE64Decoder)localDecoder.get();
        return decoder.decodeBuffer(s);
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
     * @param binaryData
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
     * Slurp a stream into a byte array and return an array of the appropriate size.  This requires
     * reading into a buffer array, and then copying to a new array.. if you don't need the array to be
     * exact size required, you can avoid a copy by supplying your own array to the alternate
     * slurpStream() call.
     *
     * @param stream  the stream to read
     * @param maxLength  the maximum number of bytes you are willing to recieve
     * @return the newly read array
     * @throws IOException if there was a problem reading the stream
     */
    public static byte[] slurpStream(InputStream stream, int maxLength) throws IOException {
        byte[] buffer = new byte[maxLength];
        int got = slurpStream(stream, buffer);
        byte[] ret = new byte[got];
        System.arraycopy(buffer, 0, ret, 0, got);
        return ret;
    }

    /**
     * Slurp a stream into a byte array without doing any copying, and return the number of bytes that
     * were in the stream.  The stream will be read until EOF or until the maximum specified number of bytes have
     * been read.  If you would like the array created for you with the exact size required, and don't mind
     * an extra array copy being involved, use the other form of slurpStream().
     *
     * @param stream the stream to read
     * @param bb the array of bytes in which to read it
     * @return the number of bytes read from the stream.
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
     * byte array, use slurpStream(InputStream, int).  If you wish to provide your own buffer to prevent
     * copying, use slurpStream(InputStream, byte[]).
     *
     * @param stream  the stream to slurp
     * @return a byte array containing the entire content of the stream, to EOF.  Never null.
     * @throws IOException if there is an IOException while reading the stream
     * @throws OutOfMemoryError if the stream is too big to fit into memory
     */
    public static byte[] slurpStream(InputStream stream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
        copyStream(stream, out);
        return out.toByteArray();
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
        return copyStream(in, out, 4096);
    }

    /**
     * Copy all of the in, right up to EOF, into out, using the specified blocksize.  Does not flush
     * or close either stream.
     *
     * @param in  the InputStream to read.  Must not be null.
     * @param out the OutputStream to write.  Must not be null.
     * @param blocksize the block size to use.  Must be positive.
     * @return the number bytes copied
     * @throws IOException if in could not be read, or out could not be written
     */
    public static long copyStream(InputStream in, OutputStream out, int blocksize) throws IOException {
        if (blocksize < 1) throw new IllegalArgumentException("blocksize must be positive");
        if (in == null || out == null) throw new NullPointerException("in and out must both be non-null");
        byte[] buf = new byte[blocksize];
        int got;
        long total = 0;
        while ((got = in.read(buf)) > 0) {
            out.write(buf, 0, got);
            total += got;
        }
        return total;
    }

    /**
     * Search the specified search array positions from start to (start + searchlen - 1), inclusive, for the
     * specified subarray (or subarray
     * prefix, if it occurrs at the end of the search range).  If the subarray occurs more than once in this range,
     * this will only find the leftmost occurrance.
     * <p>
     * This method returns -1 if the subarray was not matched at all in the search array.  If it returns a value
     * between start and (start + searchlen - 1 - subarray.length), inclusive, then the entire subarray was matched at
     * the returned index of the search array.  If it returns a value greater than (start + searchlen - 1 - subarray.length),
     * then the last (start + searchlen - 1 - retval) bytes of the search range matched the first (start + searchlen - 1 - retval)
     * prefix bytes of the subarray.
     * <p>
     * If search.length is greather than subarray.length then this will only find prefix matches.
     * <p>
     * If searchlen is zero, this method will always return -1.
     *
     * @param search     the array to search.  Must not be null
     * @param start      the start position in the search array.  must be nonnegative.
     *                   (start + searchlen - 1) must be less than search.length.
     * @param searchlen  the number of bytes to search in the search array.  must be nonnegative.
     *                   (start + searchlen - 1) must be less than search.length.
     * @param subarray   the subarray to search for.  Must be non-null and non-empty.  Note that the subarray length is allowed
     *                   to exceed the search array length -- in such cases this method will only look for the prefix match.
     * @param substart   the starting position in subarray of the subarray being searched for.  Must be nonnegative
     *                   and must be less than subarray.length.
     * @return -1 if the subarray was not matched at all; or,
     *         a number between zero and (start + searchlen - 1 - subarray.length), inclusive, if the entire
     *         subarray was matched at the returned index in the search array; or,
     *         a number greater than this if the (start + searchlen - 1 - retval) bytes at the end of the search
     *         array matched the corresponding bytes at the start of the subarray.
     * @throws IllegalArgumentException if start or searchlen is less than zero
     * @throws IllegalArgumentException if substart is less than one
     */
    public static int matchSubarrayOrPrefix(byte[] search, int start, int searchlen, byte[] subarray, int substart) {
        if (search == null || subarray == null)
            throw new IllegalArgumentException("search array and subarray must be specified");
        if (start < 0 || searchlen < 0 || substart < 0)
            throw new IllegalArgumentException("search positions and lengths must be nonnegative");
        final int end = (start + searchlen - 1);
        if (substart >= subarray.length || end >= search.length)
            throw new IllegalArgumentException("Search positions would go out of bounds");

        int foundpos = -1;
        int searchpos = start;
        int subarraypos = substart;
        while (searchpos <= end && subarraypos < subarray.length) {
            if (search[searchpos] == subarray[subarraypos]) {
                if (foundpos == -1)
                    foundpos = searchpos;
                subarraypos++;
                searchpos++;
            } else {
                if (foundpos >= 0) {
                    foundpos = -1;
                    subarraypos = substart;
                } else
                    searchpos++;
            }
        }
        return foundpos;
    }

    /**
     * Compare two byte arrays for an exact match.
     *
     * @param left      one of the arrays to compare
     * @param leftoff   the offset in left at which to start the comparison
     * @param right     the other array to compare
     * @param rightoff  the offset in right at which to start the comparison
     * @param len       the number of bytes to compare (for both arrays)
     * @return          true if the corresponding sections of both arrays are byte-for-byte identical; otherwise false
     */
    public static boolean compareArrays(byte[] left, int leftoff, byte[] right, int rightoff, int len) {
        if (leftoff < 0 || rightoff < 0 || len < 1)
            throw new IllegalArgumentException("Array offsets must be nonnegative and length must be positive");
        if (leftoff + len > left.length || rightoff + len > right.length)
            throw new IllegalArgumentException("offsets + length must remain within both arrays");
        for (int i = 0; i < len; ++i) {
            if (left[leftoff + i] != right[rightoff + i])
                return false;
        }
        return true;
    }

    /**
     * Compare two InputStreams for an exact match.  This method returns true if and only if both InputStreams
     * produce exactly the same bytes when read from the current position through EOF, and that both reach EOF
     * at the same time.  If so requested, each stream can be closed after reading.  Otherwise, if the comparison
     * succeeds, both streams are left positioned at EOF; if the comparison fails due to a mismatched byte,
     * both streams will be positioned somewhere after the mismatch; and, if the comparison fails due to one of the
     * streams reaching EOF early, the other stream will be left positioned somewhere after the its counterpart
     * reached EOF.  The states of both streams is undefined if IOException is thrown.
     * <p>
     * <b>Caveat</b>: the comparison may produce a false negative if either stream returns short reads before
     * the final block.
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
        byte[] lb = new byte[2048];
        byte[] rb = new byte[2048];
        boolean match = true;

        for (;;) {
            int gotleft = left.read(lb);
            int gotright = right.read(rb);
            if (gotleft != gotright) {
                match = false;
                break;
            } else if (gotleft == -1)
                break;
            else if (!compareArrays(lb, 0, rb, 0, gotleft)) {
                match = false;
                break;
            }
        }

        if (closeLeft) left.close();
        if (closeRight) right.close();

        return match;
    }

    /** Insert carriage returns into the given string before every columns characters, preferring to insert them before new words begin. */
    public static String wrapString(String in, int maxcol, int maxlines, String wrapSequence) {
        if (in == null) return "null";
        StringBuffer out = new StringBuffer();
        StringTokenizer tok = new StringTokenizer(in);
        int col = 0;
        int line = 0;
        while (tok.hasMoreTokens()) {
            String next = tok.nextToken();
            int len = next.length();
            if (col + len >= (maxcol - 1)) {
                out.append(wrapSequence);
                col = 0;
                line++;
                if (line > maxlines)
                    return out.toString();

            }
            out.append(next);
            col += len;
            if (tok.hasMoreTokens()) {
                out.append(" ");
                col++;
            }
        }
        return out.toString();
    }

    private static ThreadLocal md5s = new ThreadLocal();
    private static ThreadLocal sha1s = new ThreadLocal();

    /**
     * Get a thread-local MD5 MessageDigest instance.
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
}
