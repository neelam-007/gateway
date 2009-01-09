package com.l7tech.skunkworks.http;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ChunkServer {
    private static final Logger logger = Logger.getLogger(ChunkServer.class.getName());

    private static void usage() {
        System.err.println("Usage: ChunkServer portnum");
        System.exit(1);
    }

    private static int port(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            usage();
            return 0; /* NOTREACHED */
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) usage();
        int port = port(args[0]);

        ServerSocket ss = new ServerSocket(port);

        logger.info("Listening on port " + port + " for HTTP requests; will ignore the requests and return a hardcoded chunked response.");

        for (;;) {
            Socket sock = ss.accept();
            try {
                logger.info("Answering request");
                handleReq(sock);
            } catch (IOException e) {
                logger.log(Level.WARNING, "IOException: " + e.getMessage(), e);
            } finally {
                sock.close();
            }
        }
    }

    private static void handleReq(Socket sock) throws IOException {
        discardRequest(sock);
        PrintStream out = new PrintStream(sock.getOutputStream());
        out.print(
                "HTTP/1.1 200 OK\015\012" +
                "Content-Type: text/plain\015\012" +
                "Connection: close\015\012" +
                "Transfer-Encoding: chunked\015\012" +
                "\015\012" +
                "23\015\012" +
                "This is the data in the first chunk\015\012" +
                "1A\015\012" +
                "and this is the second one\015\012" +
                "0\015\012" +
                "\015\012");
        out.flush();
    }

    private static void discardRequest(Socket sock) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        String line;
        do {
            line = in.readLine();
        } while (line != null && line.trim().length() > 0);
    }
}
