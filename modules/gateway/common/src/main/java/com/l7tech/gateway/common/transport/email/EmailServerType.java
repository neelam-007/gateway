package com.l7tech.gateway.common.transport.email;

/**
 * An enumeration of email server types, along with their default ports.
 */
public enum EmailServerType {
    POP3("POP3", 110, 995),
    IMAP("IMAP", 143, 993);

    private String name;
    private int defaultClearPort;
    private int defaultSslPort;

    private EmailServerType(String name, int defaultClearPort, int defaultSslPort) {
        this.name = name;
        this.defaultClearPort = defaultClearPort;
        this.defaultSslPort = defaultSslPort;
    }

    public String getName() {
        return name;
    }
    
    public int getDefaultClearPort() {
        return defaultClearPort;
    }

    public int getDefaultSslPort() {
        return defaultSslPort;
    }
}
