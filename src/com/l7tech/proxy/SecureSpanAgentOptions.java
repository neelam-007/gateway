/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;


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
    private String trustStorePath = null;
    private Boolean useSslByDefault = null;
    private SecureSpanAgentFactory.SecureSpanAgentImpl trustedGateway = null;
    private GatewayCertificateTrustManager gatewayCertificateTrustManager = null;

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
     * If no custom KeyStore path is requested, the Agent will use the default KeyStore location ~/.l7tech/keyNNN.p12
     * where ~ is the home directory of the user owning this process, and NNN is the ID of this SecureSpanAgent.
     * <p>
     * The KeyStore is in PKCS#12 format, and is only needed if you have a client certificate.  It holds the signed
     * client certificate along with its corresponding private key.  The KeyStore is encrypted on disk with Triple-DES,
     * using a key derived from the Password.
     * @return The path of the custom KeyStore file, or null if no custom KeyStore is being requested.
     */
    public String getKeyStorePath() {
        return keyStorePath;
    }

    /**
     * Set the pathname of the custom KeyStore file that will be used for this Agent.
     * If no custom KeyStore path is requested, the Agent will use the default KeyStore location ~/.l7tech/keyNNN.p12
     * where ~ is the home directory of the user owning this process, and NNN is the ID of this SecureSpanAgent.
     * <p>
     * The KeyStore is in PKCS#12 format, and is only needed if you have a client certificate.  It holds the signed
     * client certificate along with its corresponding private key.  The KeyStore is encrypted on disk with Triple-DES,
     * using a key derived from the Password.
     * @param keyStorePath The path of the custom KeyStore file, or null if you wish to use the default path.
     */
    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    /**
     * Get the pathname of the custom CertStore file that will be used for this Agent.
     * if no custom CertStore path is requested, the Agent will use the default CertStore
     * location ~/.l7tech/certsNNN.p12 where ~ is the home directory of the user owning this process, and NNN is the
     * ID of this SecureSpanAgent.
     * <p>
     * The CertStore is in PKCS#12 format, and holds only public information including the Gateway's server
     * certificate and a publicly readable copy of the signed client certificate (if any).  It is obscured on disk
     * with Triple-DES using a constant key.
     * @return The path of the custom CertStore file, or null if no custom CertStore path is being requested.
     */
    public String getCertStorePath() {
        return trustStorePath;
    }

    /**
     * Set the pathname of the custom CertStore file that will be used for this Agent.
     * if no custom CertStore path is requested, the Agent will use the default CertStore
     * location ~/.l7tech/certsNNN.p12 where ~ is the home directory of the user owning this process, and NNN is the
     * ID of this SecureSpanAgent.
     * <p>
     * The CertStore is in PKCS#12 format, and holds only public information including the Gateway's server
     * certificate and a publicly readable copy of the signed client certificate (if any).  It is obscured on disk
     * with Triple-DES using a constant key.
     * @param certStorePath The path of the custom CertStore file, or null if you wish to use the default path.
     */
    public void setCertStorePath(String certStorePath) {
        this.trustStorePath = certStorePath;
    }

    /**
     * Check whether the created Agent will use SSL when sending messages to the Gateway for which
     * no policy is (yet) known.
     * @return useSslByDefault If True, Agent will use SSL whenever no policy is known for a request.
     *                        If False, Agent will not use SSL whenever no policy is known for a request.
     *                        If null, Agent will use it's default for this setting (currently True).
     */
    public Boolean getUseSslByDefault() {
        return useSslByDefault;
    }

    /**
     * Specify whether the created Agent should use SSL when sending messages to the Gateway for which
     * no policy is (yet) known.
     * @param useSslByDefault If True, Agent will use SSL whenever no policy is known for a request.
     *                        If False, Agent will not use SSL whenever no policy is known for a request.
     *                        If null, Agent will use it's default for this setting (currently True).
     */
    public void setUseSslByDefault(Boolean useSslByDefault) {
        this.useSslByDefault = useSslByDefault;
    }

    /**
     * Check whether the created Agent will be configured to use a Federated Gateway, and if so, which
     * Trusted Gateway will be providing the credentials.
     * @return The SecureSpanAgent pointing at the Trusted Gateway if the new Agent will be configured to
     *         work with a Federated Gateway; or null, if the new Agent will be working with a Trusted Gateway directly.
     */
    public SecureSpanAgent getTrustedGateway() {
        return trustedGateway;
    }

    /**
     * Specify whether the created Agent will be configured to use a Federated Gateway, and if so, which
     * Trusted Gateway will be providing the credentials.
     * @param trustedGateway A SecureSpanAgent pointing at the Trusted Gateway if the new Agent will be a
     *                       Federated Gateway; or null, if the new Agent will be a Trusted Gateway.  The
     *                       trustedGateway must be a SecureSpanAgent instance obtained from the
     *                       SecureSpanAgentFactory.
     */
    public void setTrustedGateway(SecureSpanAgent trustedGateway) {
        if (!(trustedGateway instanceof SecureSpanAgentFactory.SecureSpanAgentImpl))
            throw new IllegalArgumentException("trustedGateway must be a SecureSpanAgent instance " +
                                               "obtained from the SecureSpanAgentFactory.");
        this.trustedGateway = (SecureSpanAgentFactory.SecureSpanAgentImpl)trustedGateway;
    }

    /** Interface implemented by API users who wish to trust new Gateway certificates at runtime. */
    public static interface GatewayCertificateTrustManager {
        /**
         *
         * @param gatewayCertificateChain The certificate chain obtained during Gateway server certificate discovery.
         *                                This may or may not be the full chain, but is must contain
         *                                at least one certificate, assumed to be the Gateway's SSL certificate.
         * @return true iff. this certificate can be trusted for the current Gateway; false if trust cannot be
         *         conclusively established.
         * @throws CertificateException if there is a problem with the gatewayCertificateChain.
         */
        boolean isGatewayCertificateTrusted(X509Certificate[] gatewayCertificateChain) throws CertificateException;
    }

    /**
     * Check the custom trust manager for dynamically determining whether an about-to-be-imported Gateway SSL
     * certificate should be trusted.
     * @return the user callback that will determine certificate trust at runtime.
     */
    public GatewayCertificateTrustManager getGatewayCertificateTrustManager() {
        return gatewayCertificateTrustManager;
    }

    /**
     * Set a custom trust manager for dynamically determining whether an about-to-be-imported Gateway SSL certificate
     * should be trusted.  This trust manager will only be consulted if the Agent is unable to safely determine
     * certificate trust automatically (perhaps because the password is unavailable to the Agent or Gateway, or the
     * peer is a Federated Gateway).
     * @param gatewayCertificateTrustManager a user callback that will determine certificate trust at runtime.
     */
    public void setGatewayCertificateTrustManager(GatewayCertificateTrustManager gatewayCertificateTrustManager) {
        this.gatewayCertificateTrustManager = gatewayCertificateTrustManager;
    }
}
