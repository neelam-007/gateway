package com.l7tech.remote.rmi;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author emil
 * @version 22-Dec-2004
 */
public class NamingURL {
    public static String DEFAULT_SCHEME = "rmi";
    private static final int DEFAULT_PORT = 2124;

    private String scheme;
    private String host;
    private int port;
    private String name;


    /**
     * Parse Naming URL strings to obtain referenced host, port and
     * object name.
     *
     * @return an <code>NamingURL</code> object which contains each of the above
     *         components.
     * @throws java.net.MalformedURLException if given url string is malformed
     */
    public static NamingURL parse(String str)
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
            if (scheme == null) {
                scheme = DEFAULT_SCHEME;
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
                port = DEFAULT_PORT;
            }
            return new NamingURL(host, port, name);

        } catch (URISyntaxException ex) {
            throw (MalformedURLException)new MalformedURLException("invalid URL string: " + str).initCause(ex);
        }
    }


    public NamingURL(String host, int port, String name) throws MalformedURLException {
        this(DEFAULT_SCHEME, host, port, name);
    }

    public NamingURL(String scheme, String host, int port, String name) throws MalformedURLException {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.name = name;
    }

    /**
     * @return the scheme component
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * @return the host component
     */
    public String getHost() {
        return host;
    }

    /**
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the object name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a string representation of this naming URL.
     */
    public String toString() {
        try {
            String absolutePath = name;
            if (absolutePath !=null) {
                if (!absolutePath.startsWith("/")) {
                    absolutePath = "/"+ absolutePath;
                }
            }
            return new URI(scheme, null, host, port, absolutePath, null, null).toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


}
