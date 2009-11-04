package com.l7tech.uddi;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Bean for holding enough info to create a UDDIClient
 * @author darmstrong
 */
public class UDDIClientConfig {

    //for testing
    public UDDIClientConfig() {
    }

    public UDDIClientConfig(String inquiryUrl,
                            String publishUrl,
                            String subscriptionUrl,
                            String securityUrl,
                            String login,
                            String password) {
        this.inquiryUrl = inquiryUrl;
        this.publishUrl = publishUrl;
        this.subscriptionUrl = subscriptionUrl;
        this.securityUrl = securityUrl;
        this.login = login;
        this.password = password;
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

    private String inquiryUrl;
    private String publishUrl;
    private String subscriptionUrl;
    private String securityUrl;
    private String login;
    private String password;
}
