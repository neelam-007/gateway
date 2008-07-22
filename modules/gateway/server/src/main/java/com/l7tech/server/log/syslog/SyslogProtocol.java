package com.l7tech.server.log.syslog;

/**
 * Protocols for syslog.
 *
 * @author Steve Jones
 */
public enum SyslogProtocol {

    /**
     * TCP protocol
     */
    TCP,

    /**
     * UDP protocol
     */
    UDP,

    /**
     * VM protocol (for test use only)
     */
    VM;
}
