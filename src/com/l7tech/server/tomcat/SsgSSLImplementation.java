package com.l7tech.server.tomcat;

import java.net.Socket;

import org.apache.tomcat.util.net.SSLImplementation;
import org.apache.tomcat.util.net.SSLSupport;
import org.apache.tomcat.util.net.ServerSocketFactory;

/**
 * SSL implementation that wraps the default SSL factory in an SsgServerSocketFactory.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class SsgSSLImplementation extends SSLImplementation {

    //- PUBLIC

    /**
     * Create an SsgSSLImplementation that delegates to the default instance.
     *
     * @see SSLImplementation#getInstance
     */
    public SsgSSLImplementation() {
        try {
            delegate = getInstance();
        }
        catch(Exception e) {
            throw (IllegalStateException) new IllegalStateException("Cannot create default SSLImplementation").initCause(e);
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
        ServerSocketFactory ssf = delegate.getServerSocketFactory();
        return new SsgServerSocketFactory(ssf);
    }

    /**
     * Invokes delegate
     */
    public SSLSupport getSSLSupport(Socket socket) {
        return delegate.getSSLSupport(socket);
    }

    //- PRIVATE

    private final SSLImplementation delegate;
}
