package com.l7tech.server.transport.jms.prov.fiorano;

import fiorano.jms.runtime.IFMQSecurityManager;
import fiorano.jms.common.FioranoException;

import java.net.Socket;
import java.security.SecureRandom;
import java.security.KeyManagementException;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.l7tech.server.transport.jms.JmsConfigException;
import com.l7tech.server.transport.jms.JmsSslCustomizerSupport;
import com.sun.net.ssl.*;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

/**
 * Loads the digital certificates.  The digital certificates are retrieved by specifying the keystoreId and alias
 * which will be used to retrieve from the database.  Then it will initialize the SSL parameters that will be used
 * in SSL connection.
 *
 * User: dlee
 * Date: Jun 2, 2008
 */
@SuppressWarnings({"deprecation"})
public class FioranoSecurityManager implements IFMQSecurityManager {
    private static final Logger logger = Logger.getLogger(FioranoSecurityManager.class.getName());
    private SSLContext context;

    /**
     * After doing some reverse engineering on the Fiorano side of code it will actually accept a hashtable contain
     * the environment variables that we have passed.  This environment will be used so that I can create the proper
     * SSLContext with the appropriate certificate that will be retrieved from the database
     *
     * @param environment
     */
    public FioranoSecurityManager(Hashtable environment) throws JmsConfigException {
        // fiorano doesn't support client-auth, so nothing interesting to do here
        context = new SunSSLContextWrapper(JmsSslCustomizerSupport.getSSLContext(null, null));
    }

    public Object getSecurityContext() {
        logger.log(Level.FINEST, "FMQ Security Manager getSecurityContext() called.");
        return context;
    }

    public void checkExecute(Socket socket) throws FioranoException {
        //todo: maybe check the state of the established socket, e.g. was client-cert auth actually used, if configured?
    }

    private static class SunSSLContextWrapper extends SSLContext {
        public SunSSLContextWrapper(final javax.net.ssl.SSLContext otherType) {
            super(new SSLContextSpi() {
                protected void engineInit(KeyManager[] keyManagers, TrustManager[] trustManagers, SecureRandom secureRandom)
                    throws KeyManagementException {
                    //otherType.init(keyManagers, trustManagers, secureRandom); // not needed, otherType already initialized
                }

                protected SSLSocketFactory engineGetSocketFactory() {
                    return otherType.getSocketFactory();
                }

                protected SSLServerSocketFactory engineGetServerSocketFactory() {
                    return otherType.getServerSocketFactory();
                }
            },
            otherType.getProvider(),
            otherType.getProtocol());
        }
    }
}
