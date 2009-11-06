package com.l7tech.uddi;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Bean for holding enough info to create a UDDIClient
 * @author darmstrong
 */
public class UDDIClientConfig {

    //- PUBLIC

    public UDDIClientConfig( final String inquiryUrl,
                             final String publishUrl,
                             final String subscriptionUrl,
                             final String securityUrl,
                             final String login,
                             final String password,
                             final UDDIClientTLSConfig tlsConfig ) {
        this.inquiryUrl = inquiryUrl;
        this.publishUrl = publishUrl;
        this.subscriptionUrl = subscriptionUrl;
        this.securityUrl = securityUrl;
        this.login = login;
        this.password = password;
        this.tlsConfig = tlsConfig;
    }

    public String getInquiryUrl() {
        return inquiryUrl;
    }

    public String getPublishUrl() {
        return publishUrl;
    }

    public String getSubscriptionUrl() {
        return subscriptionUrl;
    }

    public String getSecurityUrl() {
        return securityUrl;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public UDDIClientTLSConfig getTlsConfig() {
        return tlsConfig;
    }

    //- PRIVATE

    private final String inquiryUrl;
    private final String publishUrl;
    private final String subscriptionUrl;
    private final String securityUrl;
    private final String login;
    private final String password;
    private final UDDIClientTLSConfig tlsConfig;
}
