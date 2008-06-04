package com.l7tech.common.security.socket;

import com.l7tech.common.io.InetAddressUtil;
import com.l7tech.common.util.ExceptionUtils;

import java.net.InetAddress;
import java.security.AccessControlException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * Takes care of finding the peer identity on a Windows system.
 */
class Win32LocalTcpPeerIdentifier extends LocalTcpPeerIdentifier {
    private static final Logger logger = Logger.getLogger(Win32LocalTcpPeerIdentifier.class.getName());

    @SuppressWarnings({"UnusedDeclaration"})
    private int pid;             // Set by native method

    @SuppressWarnings({"UnusedDeclaration"})
    private int sid;             // Set by native method

    @SuppressWarnings({"UnusedDeclaration"})
    private String username;     // Set by native method

    @SuppressWarnings({"UnusedDeclaration"})
    private String domain;       // Set by native method

    @SuppressWarnings({"UnusedDeclaration"})
    private String program;      // Set by native method


    Win32LocalTcpPeerIdentifier() {
    }

    /**
     * {@inheritDoc}
     *
     * @throws AccessControlException If this process lacks the PROCESS_QUERY_INFORMATION right on the target process,
     *                                preventing us from looking up its terminal session.
     */
    @Override
    public boolean identifyTcpPeer(InetAddress sockAddr, boolean includeAllLoopback, int sockPort, InetAddress peerAddr, int peerPort) throws UnsatisfiedLinkError, AccessControlException, IOException {
        if (!haveNativeLib)
            throw new UnsatisfiedLinkError("Win32LocalTcpPeerIdentifier is not available on this system");

        final long longsock = sockAddr == null ? 0 : InetAddressUtil.toLong(sockAddr);
        if (!nativeIdentifyTcpPeer(longsock, includeAllLoopback, sockPort, InetAddressUtil.toLong(peerAddr), peerPort))
            return false;

        putIdentifier(IDENTIFIER_PID, String.valueOf(pid));
        putIdentifier(IDENTIFIER_SID, String.valueOf(sid));
        putIdentifier(IDENTIFIER_USERNAME, username);
        putIdentifier(IDENTIFIER_NAMESPACE, domain);
        putIdentifier(IDENTIFIER_PROGRAM, program);
        return true;
    }


    /**
     * Check if Win32PeerIdentFinder services are available on this system.
     *
     * @return <b>true</b> if the service is available.  Callers can create instances of this class and can expect
     *         {@link #identifyTcpPeer} to work correctly.
     *          <p/>
     *         <b>false</b> if the service is unavailable.  Instances of this class can be created but
     *         {@link #identifyTcpPeer} will always throw UnsatisfiedLinkError.
     */
    public static boolean isAvailable() {
        return haveNativeLib;
    }


    /**
     * Finds a matching row in the current system extended TCP table, and populates the pid, username, domain,
     * and program fields with the relevant information.
     * <p/>
     * This method will scan the TCP table for a row representing a TCP connection in the ESTABLISHED state
     * that matches the given parameters.
     * Since the goal is to locate the <em>peer</em>/s process, rather than our own process, this method will
     * match a row where the <em>local</em> side of the connection matches peerAddr and peerPort, and where
     * the <em>remote</em> side of the connection matches sockAddr and sockPort.
     * <p/>
     * IP addresses should be encoded as long integers in network (big-endian) byte order:
     * <pre>(dotted decimal) AA.BB.CC.DD =  (hexadecimal) 00000000aabbccdd</pre>
     * where the lest significant byte is the final octet of the dotted decimal IP address,
     * and the four highest order bytes are zero.
     * <p/>
     * Typically both IP addresses will be 2130706433 (that is, 127.0.0.1 encoded as a long) but callers are
     * free to filter by other addresses instead if they have reason to believe such a search may be fruitful.
     * <p/>
     * If no matching entry in the TCP table is found, this method will return false.
     * Otherwise, it will attempt to look up the owning process's terminal server session, and then get
     * the session's username and domain.  It will then set the {@link #pid}, {@link #program},
     * {@link #domain}, and {@link #username}
     * fields and return true.
     * <p/>
     * If the current process does not have permission to look up the TCP table or the matching connection's
     * owning process's terminal session, this method throws AccessControlException.
     *
     * @param sockAddr  The IP address of the local (typically server) side of the socket connection, encoded as a long
     *                  per the above instructions.  May be 0 if includeAllLoopback is true.
     * @param includeAllLoopback If true, all loopback addresses -- defined as any address with a leading octet of 127 --
     *                           will be matched for the sockAddr side, as well as sockAddr itself (if specified)
     * @param sockPort  The TCP port of the local (typically server) side of the socket connection, expressed in
     *                  decimal (that is, port 8080 would be represented by the int value 8080).
     * @param peerAddr  The IP address of the peer (typically client) side of the socket connection, encoded as a long
     *                  per the above instructions.
     * @param peerPort  The TCP port of the peer (typically client) side of the socket connection, expressed in
     *                  decimal (that is, port 43212 would be represented by the int value 43212).
     * @return <b>true</b> if a row was found and fields were populated successfully; <b>false</b> if no matching row was found.
     *         If this method returns false, no fields of the instance will have been changed.
     * @throws AccessControlException If this process lacks the PROCESS_QUERY_INFORMATION right on the target process,
     *                                preventing us from looking up its terminal session.
     * @throws IOException if a Windows system call fails for some other reason 
     */
    private native boolean nativeIdentifyTcpPeer(long sockAddr, boolean includeAllLoopback, int sockPort, long peerAddr, int peerPort) throws AccessControlException, IOException;


    private static final boolean haveNativeLib;
    static {
        boolean worked = false;
        try {
            System.loadLibrary("peerident");
            worked = true;
        } catch (Throwable t) {
            logger.log(Level.INFO, "Win32 peer ident services unavailable: Unable to load and initialize native library: " +
                    ExceptionUtils.getMessage(t), t);
        }
        haveNativeLib = worked;
    }
}
