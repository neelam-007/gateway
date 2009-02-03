package com.l7tech.util;

import com.l7tech.util.ArrayUtils;
import com.l7tech.util.BufferPool;
import com.l7tech.util.ResourceUtils;

import java.io.*;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author steve
 */
public class IOUtils {
    /**
     * Slurp at most maxLength bytes from stream into a byte array and return an array of the appropriate size.
     * The slurping will fail with an IOException if there are more than maxLength bytes remaining in the stream.
     * <p/>
     * Like the other slurpStream methods, this method does NOT close the input stream.
     *
     * @param stream  the stream to read.  Must not be null.
     * @param maxLength  the maximum number of bytes you are willing to recieve
     * @return the newly read array, sized to the amount read.  Never larger than maxLength bytes.
     * @throws java.io.IOException on IOException; or, if there were more than maxLength bytes remaining in the stream.
     */
    public static byte[] slurpStream(InputStream stream, int maxLength) throws IOException {
        if (maxLength < 0 || maxLength > Integer.MAX_VALUE - 2) throw new IllegalArgumentException("Invalid length limit");
        byte[] buffer = BufferPool.getBuffer(maxLength + 1);
        try {
            int got = slurpStream(stream, buffer);
            if (got > maxLength)
                throw new IOException("Stream size limit exceeded: " + maxLength + " bytes");
            byte[] ret = new byte[got];
            System.arraycopy(buffer, 0, ret, 0, got);
            return ret;
        } finally {
            BufferPool.returnBuffer(buffer);
        }
    }

    /**
     * Slurp a stream into a byte array without doing any copying, and return the number of bytes that
     * were read from stream.  The stream will be read until EOF or until the maximum specified number of bytes have
     * been read.  If you would like the array created for you with the exact size required, and don't mind
     * an extra array copy being involved, use {@link #slurpStream(java.io.InputStream, int)}.
     * <p/>
     * Like the other slurpStream methods, this method does NOT close the input stream.
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
     * <p/>
     * Like the other slurpStream methods, this method does NOT close the input stream.
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

    /**
     * Write all of the specified bytes out to the specified OutputStream.
     *
     * @param output  the bytes to write.  May be empty but never null.
     * @param stream  the stream to write them to.  Required.
     * @throws java.io.IOException if there is an IOException while writing the bytes to the stream.
     */
    public static void spewStream(byte[] output, OutputStream stream) throws IOException {
        copyStream(new ByteArrayInputStream(output), stream);
    }

    /**
     * Copy all of the in, right up to EOF, into out.  Does not flush or close either stream.
     *
     * @param in  the InputStream to read.  Must not be null.
     * @param out the OutputStream to write.  Must not be null.
     * @return the number bytes copied
     * @throws java.io.IOException if in could not be read, or out could not be written
     */
    public static long copyStream(InputStream in, OutputStream out) throws IOException {
        if (in == null || out == null) throw new NullPointerException("in and out must both be non-null");
        byte[] buf = BufferPool.getBuffer(16384);
        try {
            int got;
            long total = 0;
            while ((got = in.read(buf)) > 0) {
                out.write(buf, 0, got);
                total += got;
            }
            return total;
        } finally {
            BufferPool.returnBuffer(buf);
        }
    }

    /**
     * Compare two InputStreams for an exact match.  This method returns true if and only if both InputStreams
     * produce exactly the same bytes when read from the current position through EOF, and that both reach EOF
     * at the same time.  If so requested, either or both streams can be closed before this method returns.
     * Otherwise, if the comparison
     * succeeds, unclosed streams are left positioned at EOF; if the comparison fails due to a mismatched byte,
     * unclosed streams will be positioned somewhere after the mismatch; and, if the comparison fails due to one of the
     * streams reaching EOF early, the other stream will be left positioned somewhere after its counterpart
     * reached EOF.  The state of both streams is undefined if IOException is thrown.
     *
     * @param left          one of the InputStreams to compare
     * @param closeLeft     if true, left will be closed when the comparison finishes
     * @param right         the other InputStream to compare
     * @param closeRight    if true, right will be closed when the comparison finishes
     * @return              true if both streams produced the same byte stream and ended at the same time;
     *                      false if one of streams ended early or produced a mismatch.
     * @throws java.io.IOException  if there was an IOException reading or closing one of the streams.
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

            return match;
        } finally {
            BufferPool.returnBuffer(lb);
            BufferPool.returnBuffer(rb);

            if (closeLeft) ResourceUtils.closeQuietly(left);
            if (closeRight) ResourceUtils.closeQuietly(right);
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

    /**
     * Compress the specified bytes using gzip, and return the compressed bytes.
     *
     * @param bytes the bytes to compress.  May be empty but must not be null.
     * @return the gzip compressed representation of the input bytes.  Never null.
     * @throws java.io.IOException if there is a problem compressing the bytes
     */
    public static byte[] compressGzip(byte[] bytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream gzos = new GZIPOutputStream(baos);
        copyStream(new ByteArrayInputStream(bytes), gzos);
        gzos.flush();
        gzos.close();
        return baos.toByteArray();
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
}
