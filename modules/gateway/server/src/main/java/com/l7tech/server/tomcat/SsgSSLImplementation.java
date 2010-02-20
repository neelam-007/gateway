package com.l7tech.server.tomcat;

import org.apache.tomcat.util.net.SSLImplementation;
import org.apache.tomcat.util.net.SSLSupport;
import org.apache.tomcat.util.net.ServerSocketFactory;

import javax.net.ssl.SSLSession;
import java.net.Socket;

/**
 * SSL implementation that wraps the default SSL factory in an SsgServerSocketFactory.
 *
 * <p>If the conf directory contains a properties file (SsgSSLImplementation.properties)
 * with any attributes then these are set on any created socket factories.</p>
 *
 * <p>This will also register our crypto provider if it is available.</p>
 *
 * @version $Revision$
 */
public class SsgSSLImplementation extends SSLImplementation {

    //- PUBLIC

    /**
     * Create an SsgSSLImplementation that delegates to the default instance.
     *
     * @see SSLImplementation#getInstance()
     */
    public SsgSSLImplementation() {
        try {
              delegate = getInstance();
        }
        catch(Exception e) {
            throw new IllegalStateException("Cannot create default SSLImplementation", e);
        }
    }

    /**
     * Get the name for this SSLImplementation.
     *
     * @return The name
     */
    public String getImplementationName() {
        return "SecureSpanGatewayWrapperFor-" + delegate.getImplementationName();
    }

    /**
     * Get the ServerSocketFactory for this SSLImplementation.
     *
     * <p>This will wrap the delegates ServerSocketFactory in an
     * SsgServerSocketFactory.</p>
     *
     * @return The wrapped SSL ServerSocketFactory
     */
    public ServerSocketFactory getServerSocketFactory() {
        return new SsgJSSESocketFactory();
    }

    /**
     * Invokes delegate
     */
    public SSLSupport getSSLSupport(Socket socket) {
        return delegate.getSSLSupport(socket);
    }

    @SuppressWarnings({"deprecation"})
    public SSLSupport getSSLSupport(SSLSession session) {
        return delegate.getSSLSupport(session);
    }

    //- PRIVATE

    private final SSLImplementation delegate;
}
