package com.l7tech.skunkworks.transport;

import com.l7tech.util.Background;
import com.l7tech.util.ResourceUtils;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.TimerTask;

/**
 * A TCP listener that accepts an incoming connection and then ignores it, closing it after 5 min without reading any octets from it.
 */
public class DeafListener {
    private static final int PORT = 15333;
    private static final long CLOSE_DELAY = 300000L;

    public static void main(String[] args) throws Exception {
        ServerSocket ss = new ServerSocket(PORT);
        System.out.println("Listening for incoming connections on port " + PORT);
        //noinspection InfiniteLoopStatement
        for (;;) {
            final Socket s = ss.accept();
            System.out.println("Connection accepted; ignoring it for " + (CLOSE_DELAY/1000L) + " seconds");
            Background.scheduleOneShot(new TimerTask() {
                @Override
                public void run() {
                    ResourceUtils.closeQuietly(s);
                }
            }, CLOSE_DELAY);
        }
    }
}
