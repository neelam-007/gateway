package com.l7tech.skunkworks.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public class RandomXmlGenerator {
    enum Action {
        GENERATE,
        CHECK
    }

    private final RandomXmlInputStream stream;
    private final Action action;


    public RandomXmlGenerator(Action action, long randomSeed, long maxStanzas, long maxNesting) {
        this.stream = new RandomXmlInputStream(randomSeed, maxNesting, maxStanzas);
        this.action = action;
    }

    public static void main(String[] args) {
        final RandomXmlGenerator gen;
        try {
            gen = make(args);
            if (gen.action == Action.GENERATE) {
                gen.sendTo(System.out);
            } else {
                gen.checkAgainst(System.in);
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.err.println("\n\nUsage: RandomXmlGenerator <GENERATE|CHECK> <randomSeed> <maxStanzas> <maxNesting>");
            System.exit(1);
        }

    }

    private void checkAgainst(InputStream in) throws IOException {
        if (!compareInputStreams(in, false, stream, false)) {
            System.err.println("Streams do not match");
            System.exit(1);
        }
    }

    private void sendTo(OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        for (;;) {
            int got = stream.read(buf);
            if (got < 1)
                return;
            out.write(buf, 0, got);
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
     * @throws IOException  if there was an IOException reading or closing one of the streams.
     *                      the state of the streams is undefined if this method throws.
     */
    public static boolean compareInputStreams(InputStream left, boolean closeLeft,
                                              InputStream right, boolean closeRight) throws IOException
    {
        byte[] lb = new byte[4096];
        byte[] rb = new byte[4096];
        boolean match = true;

        for (;;) {
            int gotleft = readFullBlock(left, lb);
            int gotright = readFullBlock(right, rb);
            if (gotleft != gotright) {
                match = false;
                break;
            } else if (gotleft < 1)
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

    private static RandomXmlGenerator make(String[] args) {
        String actionStr = args[0];
        long randomSeed = Long.parseLong(args[1]);
        long maxStanzas = Long.parseLong(args[2]);
        long maxNesting = Long.parseLong(args[3]);

        final Action action;
        if ("generate".equalsIgnoreCase(actionStr))
            action = Action.GENERATE;
        else if ("check".equalsIgnoreCase(actionStr))
            action = Action.CHECK;
        else
            throw new IllegalArgumentException("Unrecognized action: " + actionStr);

        return new RandomXmlGenerator(action, randomSeed, maxStanzas, maxNesting);
    }
}
