package com.l7tech.common.security.socket;

import com.l7tech.common.util.ExceptionUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test server for PeerIdent feature.
 * This test must be run on a Windows system.
 */
public class LocalTcpPeerIdentifierTest {
    private static final Logger logger = Logger.getLogger(LocalTcpPeerIdentifierTest.class.getName());

    public static void main(String[] args) throws Exception {
        if (!Win32LocalTcpPeerIdentifier.isAvailable())
            throw new UnsatisfiedLinkError("No Win32 local TCP peer identifier is available -- missing dll? not windows?");

        ServerSocket serv = new ServerSocket(8989, 5, InetAddress.getByName("127.0.0.1"));
        while (true) {
            Socket clientSocket = serv.accept();
            try {
                handleClientConnection(clientSocket);
            } finally {
                closeQuietly(clientSocket);
            }
        }
    }

    private static void closeQuietly(Socket clientSocket) {
        try {
            if (clientSocket != null)
                clientSocket.close();
        } catch (IOException e) {
            logger.log(Level.INFO, "IO error while closing socket: " + ExceptionUtils.getMessage(e), e);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Exception while closing socket: " + ExceptionUtils.getMessage(t), t);
        }
    }

    private static void handleClientConnection(Socket clientSocket) throws IOException {
        StringBuilder msg = new StringBuilder();

        try {
            Win32LocalTcpPeerIdentifier ident = new Win32LocalTcpPeerIdentifier();
            boolean result = ident.identifyTcpPeer(clientSocket);

            if (!result) {
                msg.append("Unable to identify you.");
            } else {
                msg.append("pid: ").append(ident.getProcessId()).append("\r\n");
                msg.append("session id: ").append(ident.getSessionId()).append("\r\n");
                msg.append("username: ").append(ident.getUsername()).append("\r\n");
                msg.append("domain: ").append(ident.getDomain()).append("\r\n");
                msg.append("program: ").append(ident.getProgram()).append("\r\n");
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "Exception while identifying connection: " + ExceptionUtils.getMessage(e), e);
            msg.append("Unable to identify you: ").append(ExceptionUtils.getMessage(e));
        }

        final OutputStream os = clientSocket.getOutputStream();
        msg.append("\r\n");
        os.write(msg.toString().getBytes());
        os.flush();
    }
}