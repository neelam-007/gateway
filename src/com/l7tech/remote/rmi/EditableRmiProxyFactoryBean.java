package com.l7tech.remote.rmi;

import org.springframework.remoting.rmi.RmiProxyFactoryBean;
import org.springframework.remoting.RemoteAccessException;

import java.rmi.Remote;
import java.rmi.Naming;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.RMIClientSocketFactory;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * The {@link RmiProxyFactoryBean} subclass that supports property
 * editing an stub reset.
 *
 * @author emil
 * @version 9-Dec-2004
 */
public class EditableRmiProxyFactoryBean extends RmiProxyFactoryBean {
    private boolean resetStubRequested = false;
    private RMIClientSocketFactory registryClientSocketFactory;

    public void setRegistryClientSocketFactory(RMIClientSocketFactory csf) {
        this.registryClientSocketFactory = csf;
    }

    /**
     * Reset the stub
     */
    public void resetStub() {
        try {
            resetStubRequested = true;
            getStub();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            resetStubRequested = false;
        }
    }

    /**
     * Overriden to support setting service url during runtime
     *
     * @throws Exception
     */
    public void afterPropertiesSet() throws Exception {
        String serviceUrl = getServiceUrl();
        try {
            if (serviceUrl == null) {
                setServiceUrl("");
            }
            super.afterPropertiesSet();
        } finally {
            setServiceUrl(serviceUrl);
        }
    }

    /**
     * overriden to support programmatic stub reset
     */
    protected Remote lookupStub() throws Exception {
        final String serviceUrl = getServiceUrl();
        if (resetStubRequested) {
            logger.trace("reset stub service " + serviceUrl);
            return null;
        }
        if (serviceUrl == null) {
            throw new RemoteAccessException("Service URL cannot be null " + getServiceInterface());
        }
        Remote stub;
        if (registryClientSocketFactory == null) {
            stub = Naming.lookup(serviceUrl);
        } else {
            ParsedNamingURL parsedUrl = parseURL(serviceUrl);
            stub = LocateRegistry.getRegistry(parsedUrl.host, parsedUrl.port, registryClientSocketFactory).lookup(parsedUrl.name);
        }
        if (logger.isInfoEnabled()) {
            logger.info("Located object with RMI URL [" + serviceUrl + "]: value=[" + stub + "]");
        }
        return stub;

    }


    /**
     * Dissect Naming URL strings to obtain referenced host, port and
     * object name.
     *
     * @return an object which contains each of the above
     *         components.
     * @throws java.net.MalformedURLException if given url string is malformed
     */
    private static ParsedNamingURL parseURL(String str)
      throws MalformedURLException {
        try {
            URI uri = new URI(str);
            if (uri.getFragment() != null) {
                throw new MalformedURLException("invalid character, '#', in URL name: " + str);
            } else if (uri.getQuery() != null) {
                throw new MalformedURLException("invalid character, '?', in URL name: " + str);
            } else if (uri.getUserInfo() != null) {
                throw new MalformedURLException("invalid character, '@', in URL host: " + str);
            }
            String scheme = uri.getScheme();
            if (scheme != null && !scheme.equals("rmi")) {
                throw new MalformedURLException("invalid URL scheme: " + str);
            }

            String name = uri.getPath();
            if (name != null) {
                if (name.startsWith("/")) {
                    name = name.substring(1);
                }
                if (name.length() == 0) {
                    name = null;
                }
            }

            String host = uri.getHost();
            if (host == null) {
                host = "";
                if (uri.getPort() == -1) {
                    /* handle URIs with explicit port but no host
                     * (e.g., "//:1098/foo"); although they do not strictly
                     * conform to RFC 2396, Naming's javadoc explicitly allows
                     * them.
                     */
                    String authority = uri.getAuthority();
                    if (authority != null && authority.startsWith(":")) {
                        authority = "localhost" + authority;
                        uri = new URI(null, authority, null, null, null);
                    }
                }
            }
            int port = uri.getPort();
            if (port == -1) {
                port = Registry.REGISTRY_PORT;
            }
            return new ParsedNamingURL(host, port, name);

        } catch (URISyntaxException ex) {
            throw (MalformedURLException)new MalformedURLException("invalid URL string: " + str).initCause(ex);
        }
    }

    /**
     * Simple class to enable multiple URL return values.
     */
    private static class ParsedNamingURL {
        String host;
        int port;
        String name;

        ParsedNamingURL(String host, int port, String name) {
            this.host = host;
            this.port = port;
            this.name = name;
        }
    }
}
