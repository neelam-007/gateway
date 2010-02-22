package com.l7tech.server.tomcat;

import org.apache.tomcat.util.net.SSLImplementation;
import org.apache.tomcat.util.net.SSLSupport;
import org.apache.tomcat.util.net.ServerSocketFactory;

import javax.net.ssl.SSLSession;
import java.net.Socket;

/**
 * SSL implementation that uses the {@code SsgJSSESocketFactory}.
 *
 * @see SsgJSSESocketFactory
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
    @Override
    public String getImplementationName() {
        return "SecureSpanGatewayWrapperFor-" + delegate.getImplementationName();
    }

    /**
     * Get the ServerSocketFactory for this SSLImplementation.
     *
     * <p>This will create a new {@code SsgJSSESocketFactory}.</p>
     *
     * @return The wrapped SSL ServerSocketFactory
     * @see SsgJSSESocketFactory
     */
    @Override
    public ServerSocketFactory getServerSocketFactory() {
        return new SsgJSSESocketFactory();
    }

    /**
     * Invokes delegate
     */
    @Override
    public SSLSupport getSSLSupport(Socket socket) {
        return delegate.getSSLSupport(socket);
    }

    @SuppressWarnings({"deprecation"})
    @Deprecated
    @Override
    public SSLSupport getSSLSupport(SSLSession session) {
        return delegate.getSSLSupport(session);
    }

    //- PRIVATE

    private final SSLImplementation delegate;
}
