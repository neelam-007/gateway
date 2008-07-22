package com.l7tech.security.socket;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.test.BenchmarkRunner;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test server for PeerIdent feature.
 * This test must be run on a Windows system.
 */
public class TestLocalTcpPeerIdentifier {
    private static final Logger logger = Logger.getLogger(TestLocalTcpPeerIdentifier.class.getName());

    public static void main(String[] args) throws Exception {
        if (!Win32LocalTcpPeerIdentifier.isAvailable())
            throw new UnsatisfiedLinkError("No local TCP peer identifier is available on this system");

        ServerSocket serv = new ServerSocket(8989, 5, InetAddress.getByName("127.0.0.1"));
        for (;;) {
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

    private static void handleClientConnection(final Socket clientSocket) throws Exception {
        StringBuilder msg = new StringBuilder();

        try {
            LocalTcpPeerIdentifier ident = LocalTcpPeerIdentifierFactory.createIdentifier();
            boolean result = ident.identifyTcpPeer(clientSocket);

            if (!result) {
                msg.append("Unable to identify you.");
            } else {
                Set<String> idents = ident.getIdentifiers();
                for (String name : idents)
                    msg.append(name).append(": ").append(ident.getIdentifier(name)).append("\r\n");
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "Exception while identifying connection: " + ExceptionUtils.getMessage(e), e);
            msg.append("Unable to identify you: ").append(ExceptionUtils.getMessage(e));
        }

        final OutputStream os = clientSocket.getOutputStream();
        msg.append("\r\n");
        os.write(msg.toString().getBytes());
        os.flush();

        // Do a quick benchmark
        new BenchmarkRunner(new Runnable() {
            public void run() {
                for (int i = 0; i < 10000; ++i) {
                    LocalTcpPeerIdentifier ident = LocalTcpPeerIdentifierFactory.createIdentifier();
                    try {
                        ident.identifyTcpPeer(clientSocket);
                    } catch (IOException e) {
                        throw new RuntimeException("Ident failed", e);
                    }
                }
            }
        }, 4).run();
    }
}