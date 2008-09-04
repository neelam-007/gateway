/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.util;

import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility methods for hex encoding and dealing with streams and byte buffers.
 * <p/>
 * For the byte stream handling utility methods that used to be in this class,
 * see IOUtils in the layer7-utility module.
 *
 * @noinspection UnnecessaryUnboxing,ForLoopReplaceableByForEach,unchecked
 */
public class HexUtils {
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

    public static byte[] getSha512Digest(byte[] stuffToDigest) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.reset();
            return md.digest(stuffToDigest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    //private HexUtils() {}

    private static final char[] hexadecimal = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

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

}
