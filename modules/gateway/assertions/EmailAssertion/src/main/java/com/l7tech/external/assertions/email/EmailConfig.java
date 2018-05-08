package com.l7tech.external.assertions.email;

import java.io.Serializable;
import java.util.Objects;

/**
 * Bean to hold email connection properties information.
 *
 * User: dlee
 * Date: Nov 18, 2008
 */
public class EmailConfig implements Serializable {

    public final static long DEFAULT_CONNECTION_TIMEOUT = 30000;
    public final static long DEFAULT_READ_TIMEOUT = 30000;
    
    private final String host;
    private final int port;
    private final EmailProtocol protocol;
    private final boolean authenticate;
    private final String authUsername;
    private final String authPassword;
    private final long connectionTimeout;
    private final long readTimeout;

    public EmailConfig(boolean authenticate, String authUsername, String authPassword, String host, int port,
                       EmailProtocol protocol) {
        this(authenticate, authUsername, authPassword, host, port, protocol, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public EmailConfig(boolean authenticate, String authUsername, String authPassword, String host, int port,
                       EmailProtocol protocol, long connectionTimeout, long readTimeout) {
        this.authenticate = authenticate;
        this.authPassword = authPassword;
        this.authUsername = authUsername;
        this.connectionTimeout = connectionTimeout;
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.readTimeout = readTimeout;
    }

    public EmailConfig(final EmailAssertion assertion) {
        this(assertion.isAuthenticate(), assertion.getAuthUsername(), assertion.getAuthPassword(),
                assertion.getSmtpHost(), Integer.parseInt(assertion.getSmtpPort()), assertion.getProtocol());
    }

    public boolean isAuthenticate() {
        return authenticate;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public EmailProtocol getProtocol() {
        return protocol;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EmailConfig that = (EmailConfig) o;
        return port == that.port &&
                authenticate == that.authenticate &&
                connectionTimeout == that.connectionTimeout &&
                readTimeout == that.readTimeout &&
                Objects.equals(host, that.host) &&
                protocol == that.protocol &&
                Objects.equals(authUsername, that.authUsername) &&
                Objects.equals(authPassword, that.authPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port, protocol, authenticate, authUsername, authPassword, connectionTimeout, readTimeout);
    }
}
