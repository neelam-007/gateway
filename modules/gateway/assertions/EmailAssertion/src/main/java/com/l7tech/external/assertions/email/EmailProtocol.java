package com.l7tech.external.assertions.email;

/**
 * Represents supported Email Protocols.
 */
public enum EmailProtocol {

    PLAIN ("Plain SMTP", 25),
    SSL ("SMTP over SSL", 465),
    STARTTLS ("SMTP with STARTTLS", 587);

    private final String description;
    private final int defaultSmtpPort;

    private EmailProtocol(final String description, final int defaultSmtpPort) {
        this.description = description;
        this.defaultSmtpPort = defaultSmtpPort;
    }

    public String getDescription() {
        return description;
    }

    public int getDefaultSmtpPort() {
        return defaultSmtpPort;
    }

    @Override
    public String toString() {
        return description;
    }
}
