package com.l7tech.common.security.socket;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.AccessControlException;
import java.io.IOException;

/**
 * Represents a strategy for identifying the peer of a local (within the same machine) TCP connection
 * on a particular operating system family.
 */
public abstract class LocalTcpPeerIdentifier {
    /**
     * Get the unique user identifier that was found in the last call to {@link #identifyTcpPeer(java.net.Socket)}.
     * <p/>
     * The returned identifier will be unique for each user account under which processes can run on the
     * local machine.  Other than that, there is no guarantee about the format, although it will typically
     * be either a username or a user ID, possibly concatenated with additional information (such as a Windows domain).
     *
     * @return the unique user identifier, if {@link #identifyTcpPeer} has been called and succeeded; otherwise null.
     */
    public abstract String getUserIdentifier();

    /**
     * Attempt to identify the user that owns the process that owns the other side of the specified Socket,
     * which is expected to represent an already established TCP connection.
     *
     * @param tcpSocket a Socket representing an established TCP connection.  Required.
     * @return false if no user could be identified.
     *         true if a user was identified; call {@link #getUserIdentifier()} to get the unique user identifier that was found.
     * @throws ClassCastException if tcpSocket is not an established TCP connection
     * @throws UnsatisfiedLinkError if needed native code is not available
     * @throws AccessControlException if this process does not have sufficient system-level privileges to obtain
     *                                the requested information
     * @throws IOException if there is an unexpected error while attempting to read needed information
     */
    public boolean identifyTcpPeer(Socket tcpSocket) throws UnsatisfiedLinkError, AccessControlException, IOException {
        int localPort = tcpSocket.getLocalPort();
        InetSocketAddress peername = (InetSocketAddress)tcpSocket.getRemoteSocketAddress();
        InetAddress peerAddress = peername.getAddress();
        int peerPort = peername.getPort();
        return identifyTcpPeer(tcpSocket.getLocalAddress(), true, localPort, peerAddress, peerPort);
    }

    /**
     * Attempt to find identity information about a locally-connected TCP peer matching the specified information.
     * <p/>
     * This method always throws UnsupportedOperationException.
     *
     * @param sockAddr The IP addresses of the local process's side of the TCP connection.  May be null if includeAllLoopback is set.
     * @param includeAllLoopback If true, all loopback addresses -- defined as any address with a leading octet of 127 --
     *                           will be matched for the sockAddr side, as well as sockAddr itself (if specified)
     * @param sockPort The TCP port of the local process's side of the TCP connection.
     * @param peerAddr The IP address of the remote side of the TCP connection.
     *                 Typically must also be 127.0.0.1 for this method to have any chance of succeeding.
     * @param peerPort The TCP port of the remote side of the TCP connection.
     * @return <b>true</b> if a matching local process was identified as owning the other side of this TCP connection,
     *              and its identifying pid, username, and Windows domain name have been saved in our instance fields.
     *           <p/>
     *         <b>false</b> if no matching local process could be identified as owning the other side of this TCP connection.
     * @throws UnsatisfiedLinkError if needed native code is not available
     * @throws AccessControlException If this process does not have sufficient system-level privileges to obtain
     *                                the requested information
     * @throws IOException if there is an unexpected error while attempting to read needed information
     */
    protected boolean identifyTcpPeer(InetAddress sockAddr, boolean includeAllLoopback, int sockPort, InetAddress peerAddr, int peerPort) throws UnsatisfiedLinkError, AccessControlException, IOException {
        throw new UnsupportedOperationException();
    }
}
