package com.l7tech.skunkworks.http;

import java.io.InputStream;
import java.net.URL;

/**
 *
 */
public class JavaUrlClient {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Requires URL as first argument");
            System.exit(1);
        }
            
        String url = args[0];

        InputStream responseStream = new URL(url).openStream();
        byte[] buf = new byte[1024];
        int got;
        do {
            got = responseStream.read(buf);
        } while (got > 0);

        System.out.println("URL content was read successfully.");
    }
}
