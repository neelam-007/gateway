package com.l7tech.server.log.syslog;

import java.io.Closeable;

/**
 * Interface for sending syslog messages.
 *
 * @author Steve Jones
 */
public interface Syslog extends Closeable {

    /**
     * Log a message to syslog.
     *
     * @param severity The severity part of the priority
     * @param process  The process to log
     * @param threadId The identifier for the log message
     * @param time     The time for the log message
     * @param message  The log message
     */
    void log(SyslogSeverity severity, String process, long threadId, long time, String message);
}
