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
                       final long nextReadPosition ) {
        this.nextReadPosition = nextReadPosition;
        this.data = data;
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

    //- PRIVATE

    private final byte[] data;
    private final long nextReadPosition;
}
