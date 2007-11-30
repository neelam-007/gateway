package com.l7tech.server.log.syslog;

import java.net.SocketAddress;

/**
 * Listener for connection events for Syslog.
 *
 * <p>When using TCP connections, this allows a user to be informed on whether
 * their syslog messages are going anywhere.</p>
 *
 * @author Steve Jones
 */
public interface SyslogConnectionListener {

    /**
     * Notification of a successful connection to a syslog server.
     *
     * @param address The connection address
     */
    void notifyConnected(SocketAddress address);

    /**
     * Notification of a failed connection or disconnection from a syslog server.
     *
     * @param address The connection address
     */
    void notifyDisconnected(SocketAddress address);
}
