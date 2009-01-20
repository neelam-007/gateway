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
     * SSL protocol
     */
    SSL,

    /**
     * UDP protocol
     */
    UDP,

    /**
     * VM protocol (for test use only)
     */
    VM;
}
