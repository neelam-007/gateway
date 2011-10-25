package com.l7tech.gateway.common.log;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Metadata for a log sink contents.
 *
 * @author Steve Jones
 */
public class LogSinkData implements Serializable {

    //- PUBLIC

    public LogSinkData(@NotNull final byte[] data,
                       final long nextReadPosition,
                       boolean isRotated,
                       long timeRead) {
        this.nextReadPosition = nextReadPosition;
        this.data = data;
        this.isRotated = isRotated;
        this.timeRead = timeRead;
    }

    /**
     * Get the log data.
     *
     * @return The data
     */
    @NotNull
    public byte[] getData() {
        return data;
    }

    /**
     * Get the offset for the next read.
     *
     * <p>A.K.A. The total number of bytes read from the file.</p>
     *
     * @return The next offset or -1 if there is no more data.
     */
    public long getNextReadPosition() {
        return nextReadPosition;
    }

    /**
     * Get if the file has been rotated and reading started from the beginning
     * @return true if the file has been rotated and read from the beginning
     */
    public boolean isRotated() {
        return isRotated;
    }

    /**
     * The time when the chunk of data is read by the server
     * @return
     */
    public long timeRead() {
        return timeRead;
    }
    //- PRIVATE

    private final byte[] data;
    private final long nextReadPosition;
    private final boolean isRotated;
    private final long timeRead;

}
