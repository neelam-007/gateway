package com.l7tech.server.tomcat;

import com.l7tech.common.util.ResourceUtils;
import org.apache.tomcat.util.net.SSLImplementation;
import org.apache.tomcat.util.net.SSLSupport;
import org.apache.tomcat.util.net.ServerSocketFactory;

import javax.net.ssl.SSLSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.Provider;
import java.security.Security;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            String providerClass = "com.l7tech.server.tomcat.ClientTrustingProvider";
            Security.addProvider((Provider) Class.forName(providerClass).newInstance());
        }
        catch(Exception e) {
            // log as fine since the provider is optional.
            logger.log(Level.FINE, "Could not configured ClientTrustingProvider", e);
        }
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
        ServerSocketFactory ssf = new SsgServerSocketFactory(delegate.getServerSocketFactory());

        File base = new File(System.getProperty("catalina.base", "/ssg"));
        File conf = new File(base, "etc/conf");
        File trustStore = new File(conf, "truststore.jks");

        ssf.setAttribute("truststorePass", "changeit");
        ssf.setAttribute("truststoreType", "JKS");
        ssf.setAttribute("truststoreFile", trustStore.getAbsolutePath());
        ssf.setAttribute("truststoreAlgorithm", "AXPK");

        File propFile = new File(base, "conf/SsgSSLImplementation.properties");
        if (propFile.exists()) {
            logger.config("Loading connector properties from '"+propFile.getAbsolutePath()+"'.");
            Properties props = new Properties();
            InputStream in = null;
            try {
                in = new FileInputStream(propFile);
                props.load(in);
                for (Map.Entry<Object, Object> entry : props.entrySet()) {
                    ssf.setAttribute((String)entry.getKey(), entry.getValue());
                }
            }
            catch(IOException ioe) {
                logger.log(Level.WARNING, "Error reading connector properties.", ioe);
            }
            finally {
                ResourceUtils.closeQuietly(in);
            }
        }
        else {
            logger.fine("No SsgSSLImplementation properties found ["+propFile.getAbsolutePath()+"]; using default properties");
        }

        return ssf;
    }

    /**
     * Invokes delegate
     */
    public SSLSupport getSSLSupport(Socket socket) {
        return delegate.getSSLSupport(socket);
    }

    public SSLSupport getSSLSupport(SSLSession session) {
        return delegate.getSSLSupport(session);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SsgSSLImplementation.class.getName());

    private final SSLImplementation delegate;
}
