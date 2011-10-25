package com.l7tech.gateway.common.log;

import java.io.Serializable;

/**
 * Data for querying the logs
 * User: wlui
 */


public class LogSinkQuery implements Serializable {
    private final long startPosition;
    private final boolean fromEnd;
    private final long lastRead;

    /**
     *
     * @param fromEnd   start reading from end of file
     * @param lastRead  last query time
     * @param startPosition position to start reading from
     */
    public LogSinkQuery(boolean fromEnd, long lastRead, long startPosition) {
        this.fromEnd = fromEnd;
        this.lastRead = lastRead;
        this.startPosition = startPosition;
    }

    public boolean isFromEnd() {
        return fromEnd;
    }

    public long getLastRead() {
        return lastRead;
    }

    public long getStartPosition() {
        return startPosition;
    }
}
