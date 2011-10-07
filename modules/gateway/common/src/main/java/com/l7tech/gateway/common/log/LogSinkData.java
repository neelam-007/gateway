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
                       final long lastReadByteLocation) {
        this.lastReadByteLocation = lastReadByteLocation;
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public long getLastReadByteLocation() {
        return lastReadByteLocation;
    }

    //- PRIVATE

    private final byte[] data;
    private final long lastReadByteLocation;
}
