package com.l7tech.util;

import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.util.Properties;
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
        PoolByteArrayOutputStream out = new PoolByteArrayOutputStream(4096);
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
        GZIPInputStream gzipIn = null;
        try {
            return slurpStream(gzipIn = new GZIPInputStream(new ByteArrayInputStream(bytes)));
        } finally {
            ResourceUtils.closeQuietly( gzipIn );
        }
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

    public static long copyStream( final Reader in, final Writer out ) throws IOException {
        return copyStream(in, out, null);
    }

    /**
     * Copy all of the in, right up to EOF, into out.  Does not flush or close either stream.
     *
     * @param in  the Reader to read.  Must not be null.
     * @param out the Writer to write.  Must not be null.
     * @return the number characters copied
     * @throws java.io.IOException if in could not be read, or out could not be written
     */
    public static long copyStream( final Reader in, final Writer out, @Nullable final Functions.UnaryVoidThrows<Long, IOException> limitCallback ) throws IOException {
        if (in == null || out == null) throw new NullPointerException("in and out must both be non-null");
        char[] buf = new char[1024];
        int got;
        long total = 0;
        while ((got = in.read(buf)) > 0) {
            out.write(buf, 0, got);
            total += got;
            if (limitCallback != null) {
                limitCallback.call(total);
            }

        }
        return total;
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
        return compressGzip( new ByteArrayInputStream(bytes) );
    }

    /**
     * Compress the given data using gzip, and return the compressed bytes.
     *
     * <p>This method will not close the given input stream.</p>
     *
     * @param in the stream to compress.  Must not be null.
     * @return the gzip compressed representation of the input bytes.  Never null.
     * @throws java.io.IOException if there is a problem compressing the bytes
     */
    public static byte[] compressGzip( final InputStream in ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream gzos = new GZIPOutputStream(baos);
        copyStream(in, gzos);
        gzos.flush();
        gzos.close();
        return baos.toByteArray();
    }

    /**
     * Load properties from a file.
     *
     * @param propsFile The file to load (required)
     * @return The properties
     * @throws FileNotFoundException If the file does not exist or cannot be opened for reading
     * @throws IOException If an error occurs
     */
    public static Properties loadProperties( final File propsFile ) throws IOException {
        final Properties properties;
        InputStream inputStream = null;
        try {
            properties = new Properties();
            properties.load( inputStream = new FileInputStream( propsFile ) );
        } finally {
            ResourceUtils.closeQuietly(inputStream);
        }
        return properties;
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

    /**
     * Create an input stream for the given data that filters through the callback output stream.
     *
     * <p>This method can be used to convert a filtering output stream to a
     * filtering input stream in cases where the output stream can be flushed
     * with partial data.</p>
     *
     * <p>WARNING: This should not be used if the filtering output stream
     * expands the content to 400% of the input size or more or if the stream
     * cannot be flushed with partial data.</p>
     *
     * <p>The output stream passed to the callback is assumed to be closed by
     * closing the returned stream. If this is not the case then the callback
     * should otherwise handle closing the stream.</p>
     *
     * @param data The data to be read
     * @param offset The offset for the data
     * @param length The length for the data
     * @param outputBuilder Callback for construction of the filtering output stream.
     * @return The filtered input stream.
     */
    public static InputStream toInputStream( final byte[] data,
                                             final int offset,
                                             final int length,
                                             final Functions.Unary<OutputStream,OutputStream> outputBuilder ) {
        return new InputStream(){
            private final int[] out = new int[400];
            private int outReadIndex = 0;
            private int outWriteIndex = 0;
            private int dataIndex = offset;
            private final int dataEnd = offset + length;
            private final OutputStream encoderOut = outputBuilder.call( new OutputStream(){
                @Override
                public void write( final int b ) throws IOException {
                    out[outWriteIndex] = ((byte)b) & 0xFF; // this should not be necessary
                    outWriteIndex = ++outWriteIndex % 400;
                }
            } );

            @Override
            public int read() throws IOException {
                if ( outReadIndex == outWriteIndex ) {
                    if ( (dataIndex + 100) < dataEnd ) {
                        encoderOut.write( data, dataIndex, 100 );
                        encoderOut.flush();
                        dataIndex += 100;
                    } else if ( dataIndex < dataEnd ) {
                        encoderOut.write( data, dataIndex, dataEnd - dataIndex );
                        encoderOut.flush();
                        encoderOut.close();
                        dataIndex = dataEnd;
                    }
                }

                if ( outReadIndex == outWriteIndex ) {
                    return -1;
                } else {
                    int read = out[outReadIndex];
                    outReadIndex = (outReadIndex+1) % 400;
                    return read;
                }
            }

            @Override
            public void close() throws IOException {
                ResourceUtils.closeQuietly( encoderOut );
            }
        };
    }
}
