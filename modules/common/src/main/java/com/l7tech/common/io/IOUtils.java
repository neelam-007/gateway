package com.l7tech.common.io;

import com.l7tech.util.HexUtils;
import com.l7tech.util.ResourceUtils;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * @author steve
 */
public class IOUtils extends HexUtils {
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
    public static byte[] slurpStream( InputStream stream, int maxLength) throws IOException {
        byte[] buffer = new byte[maxLength];
        int got = slurpStream(stream, buffer);
        byte[] ret = new byte[got];
        System.arraycopy(buffer, 0, ret, 0, got);
        return ret;
    }

    /**
     * This method is now a synonym for {@link #slurpStream(java.io.InputStream)}, now that that method uses BufferPool.
     *
     * @param stream  the stream to read.  Must not be null.
     * @return the newly read array, sized to the amount read.  never null.
     * @throws java.io.IOException if there was a problem reading the stream
     * @see #slurpStream(java.io.InputStream)
     */
    public static byte[] slurpStreamLocalBuffer(InputStream stream) throws IOException {
        return slurpStream(stream);
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
     * byte array, use {@link #slurpStream(java.io.InputStream, int)}.
     * If you wish to provide your own buffer to prevent
     * copying, use {@link #slurpStream(java.io.InputStream, byte[])}.
     *
     * @param stream  the stream to slurp
     * @return a byte array containing the entire content of the stream, to EOF.  Never null.
     * @throws java.io.IOException if there is an IOException while reading the stream
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
     * Read an entire file into memory.
     *
     * @param file  the file to read.  Required.
     * @return the content of the file.  May be empty but never null.
     * @throws java.io.IOException if the file wasn't found or couldn't be read.
     */
    public static byte[] slurpFile( File file) throws IOException {
        FileInputStream fis = null;
        try {
            return slurpStream(fis = new FileInputStream(file));
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
    }

    /**
     * Read an entire file into memory via a URL.
     *
     * @param url  the file to read.  Required.
     * @return the content of the file.  May be empty but never null.
     * @throws java.io.IOException if the file wasn't found or couldn't be read.
     * @throws NullPointerException if url is null
     */
    public static byte[] slurpUrl( URL url) throws IOException {
        InputStream fis = null;
        try {
            return slurpStream(fis = url.openStream());
        } finally {
            ResourceUtils.closeQuietly(fis);
        }
    }

    /**
     * Decompress the specified bytes using gzip, and return the compressed bytes.
     *
     * @param bytes the bytes to decompress.  Must be valid gzip format.  Required.
     * @return the raw uncompressed contents of the gzipped data.  May be empty but never null.
     * @throws java.io.IOException if there was a problem decompressing the bytes.
     */
    public static byte[] decompressGzip(byte[] bytes) throws IOException {
        return slurpStream(new GZIPInputStream(new ByteArrayInputStream(bytes)));
    }

    /**
     * Decompress the specified bytes using gzip, and return the compressed bytes.
     * This is a synonym for {@link #decompressGzip}.
     *
     * @param bytes the bytes to decompress.  Must be valid gzip format.  Required.
     * @return the raw uncompressed contents of the gzipped data.  May be empty but never null.
     * @throws java.io.IOException if there was a problem decompressing the bytes.
     */
    public static byte[] uncompressGzip(byte[] bytes) throws IOException {
        return decompressGzip(bytes);
    }
}
