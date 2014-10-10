package com.l7tech.external.assertions.bufferdata.server;

import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for buffers that can support appendAndMaybeExtract().
 */
public interface Buffer {
    /**
     * Append a byte array to the buffer, possibly first extracting the current buffer contents if it would thereby
     * become too full, or if the oldest buffered data is currently too old.
     *
     * @param dataToAppend data to append to buffer.  The buffer takes ownership of this byte array,
     *                     and the caller must not retain a reference to it after donating it to the buffer.
     *                     If this block is larger than the maximum buffer size this method will fail
     *                     with an IllegalArgumentException.
     * @param maxSizeBytes maximum size the buffer may grow to.  If the current size plus the dataToAppend size
     *                     exceeds maxSizeBytes, an extract will occur before the append.
     * @param maxAgeMillis maximum age of oldest buffered data.  If the oldest currently-buffered data is older than
     *                     this age, an extract will occur before the append.
     * @return buffer status at the end of the operation, along with extracted buffer contents (if any) or null.
     */
    @NotNull
    Pair<BufferStatus,byte[]> appendAndMaybeExtract( @NotNull byte[] dataToAppend, long maxSizeBytes, long maxAgeMillis );

    /**
     * Discard buffer contents.  Call when the buffer will not be used again.
     * Not safe if combined concurrently with other calls.
     */
    void discard();

    /**
     * Used to return the status of a buffer after an appendAndMaybeExtract.
     */
    public static class BufferStatus {
        private final long size;
        private final long age;
        private final boolean wasFull;
        private final boolean wasExtracted;

        public BufferStatus( long size, long age, boolean wasFull, boolean wasExtracted ) {
            this.size = size;
            this.age = age;
            this.wasFull = wasFull;
            this.wasExtracted = wasExtracted;
        }

        public long getSize() {
            return size;
        }

        public long getAge() {
            return age;
        }

        public boolean isWasFull() {
            return wasFull;
        }

        public boolean isWasExtracted() {
            return wasExtracted;
        }
    }
}
