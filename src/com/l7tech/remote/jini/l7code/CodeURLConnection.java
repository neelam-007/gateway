package com.l7tech.remote.jini.l7code;

import com.l7tech.common.util.Locator;
import com.l7tech.remote.jini.lookup.ServiceLookup;

import java.net.URLConnection;
import java.net.URL;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

/**
 * @author emil
 * @version 24-Mar-2004
 */
public class CodeURLConnection extends URLConnection {
    protected boolean connected = false;
    private CodeServer codeServer;

    public CodeURLConnection(URL url) {
        super(url);
    }

    public void connect() throws IOException {
        if (connected) return;
        ServiceLookup serviceLookup = (ServiceLookup)Locator.getDefault().lookup(ServiceLookup.class);
        if (serviceLookup == null) {
            throw new IOException("Unable to obtain service lookup");
        }
        codeServer = (CodeServer)serviceLookup.lookup(CodeServer.class, null);
        if (codeServer == null) {
             throw new IOException("Unable to obtain code server service");
         }

        connected = true;
    }

    public InputStream getInputStream() throws IOException {
        connect();
        return new ByteArrayInputStream(codeServer.getResource(url.getFile()));
    }
}
