/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;


/**
 * Encapsulates settings to tell SecureSpanAgentFactory what kind of SecureSpanAgent to create for you.
 * @author mike
 * @version 1.0
 */
public class SecureSpanAgentOptions {
    private final String gatewayHostname;
    private final String username;
    private final char[] password;
    private int gatewayPort = 8080;
    private int gatewaySslPort = 8443;
    private int id = 0;
    private String keyStorePath = null;

    /**
     * Create an object to hold settings that can be used to customize the SecureSpanAgent delivered
     * by the SecureSpanAgentFactory.
     *
     * @param gatewayHostname
     * @param username
     * @param password
     */
    public SecureSpanAgentOptions(String gatewayHostname,
                                  String username,
                                  char[] password) {
        this.gatewayHostname = gatewayHostname;
        this.username = username;
        this.password = password;
    }

    /**
     * Get the hostname of the Gateway the created Agent will be pointed at.
     * @return the Gateway hostname, ie "gateway1.example.com"
     */
    public String getGatewayHostname() {
        return gatewayHostname;
    }

    /**
     * Get the username for the account the created Agent will use to talk to this Gateway.
     * @return the username, ie "mlyons"
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the password that will be used to perform password-based authentication, complete certificate
     * discovery, and unlock the client certificate private key.
     * @return the password, ie "s3cret999".toCharArray()
     */
    public char[] getPassword() {
        return password;
    }

    /**
     * Get the port that will be used to connect to the Gateway when SSL is not being used.
     * @return the Gateway HTTP port
     */
    public int getGatewayPort() {
        return gatewayPort;
    }

    /**
     * Set the port that will be used to connect to the Gateway when SSL is not being used.
     * @param gatewayPort the Gateway HTTP port
     */
    public void setGatewayPort(int gatewayPort) {
        this.gatewayPort = gatewayPort;
    }

    /**
     * Get the port that will be used to connect to the Gateway when SSL is being used.
     * @return the Gateway SSL port
     */
    public int getGatewaySslPort() {
        return gatewaySslPort;
    }

    /**
     * Set the port that will be used to connect to the Gateway when SSL is being used.
     * @param gatewaySslPort the Gateway SSL port
     */
    public void setGatewaySslPort(int gatewaySslPort) {
        this.gatewaySslPort = gatewaySslPort;
    }

    /**
     * Set the ID to use for the SecureSpanAgent instance.  The ID is used to generate the keystore filename,
     * if one is not set explicitly.  If no ID is set, the hashCode of the username will be used as the ID.
     * @param id The ID to use.  SecureSpanAgent instances created with different credentials must have different IDs.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Get the ID to use for the SecureSpanAgent instance.  The ID is used to generate the keystore filename,
     * if one is not set explicitly.  If no ID has been set, the hashCode of the username is used as the ID.
     * @return the ID that will be used by the newly-created SecureSpanAgent
     */
    public int getId() {
        if (id == 0)
            id = username.hashCode();
        return id;
    }

    /**
     * Get the pathname of the custom KeyStore file that will be used for this Agent.
     * If no custom KeyStore is requested, the Agent will use the default KeyStore location ~/.l7tech/keyStoreNNN
     * where ~ is the home directory of the user owning this process, and NNN is the ID of this SecureSpanAgent.
     * @return The path of the custom KeyStore file, or null if no custom KeyStore is being requested.
     */
    public String getKeyStorePath() {
        return keyStorePath;
    }

    /**
     * Set the pathname of the custom KeyStore file that will be used for this Agent.
     * If no custom KeyStore is requested, the Agent will use the default KeyStore location ~/.l7tech/keyStoreNNN
     * where ~ is the home directory of the user owning this process, and NNN is the ID of this SecureSpanAgent.
     * @param keyStorePath The path of the custom KeyStore file, or null if you wish to use the default path.
     */
    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }
}
