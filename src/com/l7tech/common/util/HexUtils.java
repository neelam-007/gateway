/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for hex encoding.
 * User: mike
 * Date: Jul 15, 2003
 * Time: 2:31:32 PM
 */
public class HexUtils {
    private static final Logger log = Logger.getLogger(HexUtils.class.getName());

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
     * @return
     */
    public static String hexDump(byte[] binaryData) {
        char[] buffer = new char[binaryData.length * 2];
        for (int i = 0; i < binaryData.length; i++) {
            int low = (binaryData[i] & 0x0f);
            int high = ((binaryData[i] & 0xf0) >> 4);
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
            byte b = (byte)((b1 << 4) + b2);
            bytes[i/2] = b;
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
        int maxSize = bb.length;
        int remaining = maxSize;
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

    /** Holds the result of a slurpUrl() call. */
    public static class Slurpage {
        public final Map headers;
        public final byte[] bytes;

        private Slurpage(byte[] bytes, Map headers) { this.bytes = bytes; this.headers = headers; }
    }

    /**
     * Execute an HTTP GET or POST on URL and return contents as a byte array.
     * @param url
     * @param dataToPost  post data, or null to do a GET
     * @return
     * @throws java.io.IOException If any connection problems arise, or if number of bytes read does not equal expected number of bytes in HTTP header.
     */
    public static Slurpage slurpUrl(URL url, InputStream dataToPost, String postDataContentType) throws IOException {
        URLConnection urlConnection = url.openConnection();
        urlConnection.setDoInput(true);
        urlConnection.setAllowUserInteraction(false);

        if (dataToPost != null) {
            if (postDataContentType != null)
                urlConnection.setRequestProperty(XmlUtil.CONTENT_TYPE, postDataContentType);
            urlConnection.setDoOutput(true);
            OutputStream os = urlConnection.getOutputStream();
            byte[] block = new byte[8192];
            int size = 0;
            while ((size = dataToPost.read(block)) > 0)
                os.write(block, 0, size);
        }

        int len = urlConnection.getContentLength();
        if (len < 0) {
            // no content lenth, have to do it the hard way
            log.log(Level.FINE, "No content-length header in response; allocating up to 512kb");
            byte[] got = slurpStream(urlConnection.getInputStream(), 1024 * 512);
            return new Slurpage(got, urlConnection.getHeaderFields());
        }

        byte[] byteArray = new byte[len];
        InputStream bin = null;
        try {
            bin = urlConnection.getInputStream();
            int got = slurpStream(bin, byteArray);
            if (got != len)
                throw new IOException("Did not receive the correct number of bytes: " + url.toString());
            return new Slurpage(byteArray, urlConnection.getHeaderFields());
        } finally {
            if (bin != null)
                bin.close();
        }
    }

    /**
     * Execute an HTTP GET or POST on URL and return contents as a byte array.
     * @param url
     * @param dataToPost  post data, or null to do a GET
     * @return
     * @throws java.io.IOException If any connection problems arise, or if number of bytes read does not equal expected number of bytes in HTTP header.
     */
    public static Slurpage slurpUrl(URL url, byte[] dataToPost, String postDataContentType) throws IOException {
        return slurpUrl(url, new ByteArrayInputStream(dataToPost), postDataContentType);
    }

    /**
     * Execute an HTTP GET on URL and return contents as a byte array.
     * @param url
     * @return
     * @throws java.io.IOException If any connection problems arise, or if number of bytes read does not equal expected number of bytes in HTTP header.
     */
    public static Slurpage slurpUrl(URL url) throws IOException {
        return slurpUrl(url, (InputStream)null, null);
    }
}
