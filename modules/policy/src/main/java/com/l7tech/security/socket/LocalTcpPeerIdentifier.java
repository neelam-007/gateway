package com.l7tech.security.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.AccessControlException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a strategy for identifying the peer of a local (within the same machine) TCP connection
 * on a particular operating system family.
 * <p/>
 * This class is not thread safe.
 */
public abstract class LocalTcpPeerIdentifier {
    /** The process ID of the client process, or null if not known or not relevant.  Format not specified.  Not necessarily a number. */
    public static final String IDENTIFIER_PID = "pid";

    /** The session ID of the client process, or null if not known or not relevant.  Format not specified.  Not necessarily a number. */
    public static final String IDENTIFIER_SID = "sid";

    /** The user ID of the user owning the client process, or null if not known or relevant.  Format not specified.  Not necessarily a number. */
    public static final String IDENTIFIER_UID = "uid";

    /** The name of the user owning the client process, or null if not known or not relevant.  Format not specified. */
    public static final String IDENTIFIER_USERNAME = "username";

    /** The namespace in which the username is meaningful -- e.g., a Windows domain -- or null if not known or not relevant. */
    public static final String IDENTIFIER_NAMESPACE = "namespace";

    /** The name of the program that was executed to create the client process, or null or empty if not known or not relevant. */
    public static final String IDENTIFIER_PROGRAM = "program";

    /**
     * Check if all resources needed for this implementation to work are available.
     *
     * @return true iff. this implementation's {@link #identifyTcpPeer} could be expected to work in this environment.
     */
    public boolean isAvailable() {
        return true;
    }
    
    /**
     * Attempt to identify the user that owns the process that owns the other side of the specified Socket,
     * which is expected to represent an already established TCP connection.
     *
     * @param tcpSocket a Socket representing an established TCP connection.  Required.
     * @return false if no user could be identified.
     *         true if a user was identified; call {@link #getIdentifier} to query the information that was found.
     * @throws ClassCastException if tcpSocket is not an established TCP connection
     * @throws UnsatisfiedLinkError if needed native code is not available
     * @throws AccessControlException if this process does not have sufficient system-level privileges to obtain
     *                                the requested information
     * @throws IOException if there is an unexpected error while attempting to read needed information
     */
    public boolean identifyTcpPeer(Socket tcpSocket) throws UnsatisfiedLinkError, AccessControlException, IOException {
        clearIdentifiers();
        int localPort = tcpSocket.getLocalPort();
        InetSocketAddress peername = (InetSocketAddress)tcpSocket.getRemoteSocketAddress();
        InetAddress peerAddress = peername.getAddress();
        int peerPort = peername.getPort();
        return identifyTcpPeer(tcpSocket.getLocalAddress(), true, localPort, peerAddress, peerPort);
    }


    /**
     * Get all the identifiers collected by a previous call to {@link #identifyTcpPeer}.
     *
     * @return the Set of identifiers.  Never null.  Will be empty unless call to {@link #identifyTcpPeer} has succeeded.
     */
    public Set<String> getIdentifiers() {
        return Collections.unmodifiableSet(identifiers.keySet());
    }

    /**
     * Get the identifiers as a Map.
     *
     * @return the identifier map.  Never null, but will be empty unless a call to {@link #identifyTcpPeer} has succeeded.
     */
    public Map<String, String> getIdentifierMap() {
        return Collections.unmodifiableMap(identifiers);
    }
    
    /**
     * Get the unique user identifier that was found in the last call to {@link #identifyTcpPeer(java.net.Socket)}.
     * <p/>
     * The returned identifier will be unique for each user account under which processes can run on the
     * local machine.  Other than that, there is no guarantee about the format, although it will typically
     * be either a username or a user ID, possibly concatenated with additional information (such as a Windows domain).
     *
     * @param key  the name of the identifier to get.  Required.
     * @return the specified identifier, if {@link #identifyTcpPeer} has been called and succeeded, and if
     *         the requested identifier was available; otherwise null.
     */
    public String getIdentifier(String key) {
        return identifiers.get(key);
    }


    /**
     * Attempt to find identity information about a locally-connected TCP peer matching the specified information,
     * if the actual socket is not available.
     *
     * @param sockAddr The IP addresses of the local process's side of the TCP connection.  May be null if includeAllLoopback is set.
     * @param includeAllLoopback If true, all loopback addresses -- defined as any address with a leading octet of 127 --
     *                           will be matched for the sockAddr side, as well as sockAddr itself (if specified)
     * @param sockPort The TCP port of the local process's side of the TCP connection.
     * @param peerAddr The IP address of the remote side of the TCP connection.
     *                 Typically must also be 127.0.0.1 for this method to have any chance of succeeding.
     * @param peerPort The TCP port of the remote side of the TCP connection.
     * @return <b>true</b> if a matching local process was identified as owning the other side of this TCP connection,
     *              and its identifying information has been saved in this instance.
     *           <p/>
     *         <b>false</b> if no matching local process could be identified as owning the other side of this TCP connection.
     * @throws UnsatisfiedLinkError if needed native code is not available
     * @throws AccessControlException If this process does not have sufficient system-level privileges to obtain
     *                                the requested information
     * @throws IOException if there is an unexpected error while attempting to read needed information
     * @throws UnsupportedOperationException if this LocalTcpPeerIdentifier requires access to the low level socket
     */
    protected boolean identifyTcpPeer(InetAddress sockAddr, boolean includeAllLoopback, int sockPort, InetAddress peerAddr, int peerPort)
            throws UnsatisfiedLinkError, AccessControlException, IOException, UnsupportedOperationException
    {
        throw new UnsupportedOperationException();
    }


    /**
     * Save the specified identifier information into this instance.
     *
     * @param key   the name of the identifier to get.  Required.
     * @param value the value to set for this identifier.  Required.
     *              Use {@link #removeIdentifier} to delete an identifier.
     * @return the previous value for this identifier, or null if there was no previous value.
     */
    protected String putIdentifier(String key, String value) {
        if (key == null || value == null) throw new IllegalArgumentException();
        return identifiers.put(key, value);
    }


    /**
     * Remove the specified identifier information from this instance.
     *
     * @param key  the name of the identifier to remove.
     * @return the previous value for this identifier, or null if there was no previous value.
     */
    protected String removeIdentifier(String key) {
        return identifiers.remove(key);
    }


    /**
     * Remove all identifier information from this instance.
     */
    protected void clearIdentifiers() {
        identifiers.clear();
    }


    private final Map<String, String> identifiers = new HashMap<String, String>();
}
