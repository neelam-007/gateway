package com.l7tech.remote.jini.l7code;

import java.net.URLStreamHandler;
import java.net.URLConnection;
import java.net.URL;
import java.io.IOException;

/**
 * @author emil
 * @version 24-Mar-2004
 */
public class Handler extends URLStreamHandler {
    protected URLConnection openConnection(URL u) throws IOException {
        return new CodeURLConnection(u);
    }
}
