package com.l7tech.server.transport.email;

import com.l7tech.policy.assertion.alert.EmailAlertAssertion;

/**
 * Bean to hold email configuration property informaiton.
 *
 * User: dlee
 * Date: Nov 18, 2008
 */
public class EmailConfig {
    private final static long DEFAULT_CONNECTION_TIMEOUT = 60000;
    private final static long DEFAULT_READ_TIMEOUT = 60000;
    
    private String host;
    private int port;
    private EmailAlertAssertion.Protocol protocol;
    private boolean authenticate;
    private String authUsername;
    private String authPassword;
    private long connectionTimeout;
    private long readTimeout;

    public EmailConfig(boolean authenticate, String authUsername, String authPassword, String host, int port,
                       EmailAlertAssertion.Protocol protocol) {
        this.authenticate = authenticate;
        this.authPassword = authPassword;
        this.authUsername = authUsername;
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        this.readTimeout = DEFAULT_READ_TIMEOUT;
    }

    public EmailConfig(boolean authenticate, String authUsername, String authPassword, String host, int port,
                       EmailAlertAssertion.Protocol protocol, long connectionTimeout, long readTimeout) {
        this.authenticate = authenticate;
        this.authPassword = authPassword;
        this.authUsername = authUsername;
        this.connectionTimeout = connectionTimeout;
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.readTimeout = readTimeout;
    }

    public boolean isAuthenticate() {
        return authenticate;
    }

    public void setAuthenticate(boolean authenticate) {
        this.authenticate = authenticate;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public EmailAlertAssertion.Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(EmailAlertAssertion.Protocol protocol) {
        this.protocol = protocol;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public long getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
    }
}
