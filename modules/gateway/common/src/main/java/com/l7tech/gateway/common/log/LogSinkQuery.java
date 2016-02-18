package com.l7tech.gateway.common.log;

import java.io.Serializable;

/**
 * Data for querying the logs
 *
 * Used by LogAccessAdmin and LogSinkAdmin
 *
 * WARNING
 * WARNING
 * WARNING If you change this class you MUST update "cluster-servlet.xml" to
 * WARNING permit its field instance classes (node to node whitelist)
 * WARNING
 * WARNING If you refactor this class to extend a superclass (NOT ADVISABLE BTW)
 * WARNING you MUST update "cluster-servlet.xml" to permit the superclass(es)and their fields.
 * WARNING
 * WARNING You must ensure that permitted classes ARE NOT from untrusted sources
 * WARNING and CANNOT be used to execute arbitrary Java code e.g. java.lang.System and java.lang.Runtime MUSTN'T be permitted.
 * WARNING In addition Apache InvokerTransformer is known to have remote code execution vulnerability and MUST NOT be permitted.
 * WARNING
 * WARNING
 *
 * User: wlui
 */


public final class LogSinkQuery implements Serializable {
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
