package com.l7tech.server.log.syslog;

/**
 * Syslog severities.
 *
 * <p>From RFC 3164 (http://www.faqs.org/rfcs/rfc3164.html section 4.1.1)</p>
 *
 * @author Steve Jones
 */
public enum SyslogSeverity {

    // Note that the ordinal is misused as the severity, so don't re-order these ...

    /**
     * 0 - Emergency: system is unusable
     */
    EMERGENCY,

    /**
     * 1 - Alert: action must be taken immediately
     */
    ALERT,

    /**
     * 2 - Critical: critical conditions
     */
    CRITICAL,

    /**
     * 3 - Error: error conditions
     */
    ERROR,

    /**
     * 4 - Warning: warning conditions
     */
    WARNING,

    /**
     * 5 - Notice: normal but significant condition
     */
    NOTICE,

    /**
     * 6 - Informational: informational messages
     */
    INFORMATIONAL,

    /**
     * 7 - Debug: debug-level messages    
     */
    DEBUG;

    /**
     * Get the severity value.
     *
     * @return The severity
     */
    public int getSeverity() {
        return ordinal();
    }
}
