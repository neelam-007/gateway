package com.l7tech.server.log.syslog.impl;

/**
 * Syslog message data
 *
 * @author Steve Jones
 */
class SyslogMessage {

    //- PUBLIC

    /**
     * Get the facility value for this message.
     *
     * @return The facility
     */
    public int getFacility() {
        return facility;
    }

    /**
     * Get the severity value for this message.
     *
     * @return The severity
     */
    public int getSeverity() {
        return severity;
    }

    /**
     * Get the priority value for this message.
     *
     * <p>The priority is calculated from the facility and severity as follows:</p>
     *
     * <code> PRIORITY = ( 8 * FACILITY ) + SEVERITY </code>
     *
     * @return The priority
     */
    public int getPriority() {
        return ((8*getFacility()) + getSeverity());
    }

    /**
     * Get the host (FQDN) for this log message.
     *
     * @return The host
     */
    public String getHost() {
        return host;
    }

    /**
     * Get the message part of the syslog message.
     *
     * @return The message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the "process" name
     *
     * @return The process
     */
    public String getProcess() {
        return process;
    }

    /**
     * Get thread identifier
     *
     * @return The thread id
     * @see Thread#getId()
     */
    public long getThreadId() {
        return threadId;
    }

    /**
     * Get the time for this log message.
     *
     * @return The time in millis
     */
    public long getTime() {
        return time;
    }

    //- PACKAGE

    /**
     * Create a new syslog message with the given values.
     * 
     * @param facility The facility (part of priority)
     * @param severity The severity (part of priority)
     * @param host The fully qualified host name
     * @param process The process name
     * @param threadId The thread identifier
     * @param time The time for the log message
     * @param message The
     */
    SyslogMessage(final int facility,
                  final int severity,
                  final String host,
                  final String process,
                  final long threadId,
                  final long time,
                  final String message) {
        this.facility = facility;
        this.severity = severity;
        this.host = host;
        this.process = process;
        this.threadId = threadId;
        this.time = time;
        this.message = message;
    }

    //- PRIVATE

    private final int facility;
    private final int severity;
    private final String host;
    private final String process;
    private final long threadId;
    private final long time;
    private final String message;
}
