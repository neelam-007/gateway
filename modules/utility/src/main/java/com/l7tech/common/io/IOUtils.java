package com.l7tech.common.io;

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

            if (closeLeft) left.close();
            if (closeRight) right.close();

            return match;
        } finally {
            BufferPool.returnBuffer(lb);
            BufferPool.returnBuffer(rb);
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
