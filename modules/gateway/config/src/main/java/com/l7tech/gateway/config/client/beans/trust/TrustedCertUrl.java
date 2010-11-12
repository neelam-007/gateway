/**
 * Copyright (C) 2008-2009 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.util.InetAddressUtil;
import com.l7tech.common.io.PermissiveHostnameVerifier;
import com.l7tech.common.io.PermissiveSSLSocketFactory;
import com.l7tech.config.client.ConfigurationException;
import com.l7tech.gateway.config.client.beans.ConfigResult;
import com.l7tech.gateway.config.client.beans.ConfigurationContext;
import com.l7tech.gateway.config.client.beans.UrlConfigurableBean;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/** @author alex */
public class TrustedCertUrl extends UrlConfigurableBean {
    private volatile X509Certificate cert;
    private final NewTrustedCertFactory factory;
    private static final int DEFAULT_TRUSTED_CERT_PORT = 8182;

    TrustedCertUrl(NewTrustedCertFactory factory) {
        super("host.controller.remoteNodeManagement.tempTrustedCertUrl", "HTTPS URL", null, "https");
        this.factory = factory;
    }

    @Override
    public void validate(URL value) throws ConfigurationException {
        URLConnection conn;
        try {
            conn = value.openConnection();
            if (!(conn instanceof HttpsURLConnection)) throw new ConfigurationException("Provided URL did not result in an HTTPS connection");

            HttpsURLConnection httpsURLConnection = (HttpsURLConnection)conn;
            httpsURLConnection.setSSLSocketFactory(new PermissiveSSLSocketFactory());
            httpsURLConnection.setHostnameVerifier(new PermissiveHostnameVerifier());
            httpsURLConnection.setReadTimeout( SyspropUtil.getInteger(TrustedCertUrl.class.getName() + ".readTimeout", 30000) );
            httpsURLConnection.setConnectTimeout( SyspropUtil.getInteger(TrustedCertUrl.class.getName() + ".connectTimeout", 30000) );
            httpsURLConnection.connect();
            Certificate[] certs = httpsURLConnection.getServerCertificates();
            if (certs == null || certs.length < 1) throw new ConfigurationException("Server presented no certificates");
            if (!(certs[0] instanceof X509Certificate)) throw new ConfigurationException("Server certificate wasn't X.509");
            cert = (X509Certificate)certs[0];
        } catch (IOException e) {
            throw new ConfigurationException("Unable to connect to the provided URL, please check that you have\nentered the correct host and port and that the server is running.", e);
        }
    }

    @Override
    public ConfigResult onConfiguration(URL value, ConfigurationContext context) {
        if (cert == null) throw new IllegalStateException("cert is null");
        return ConfigResult.chain(new ConfirmTrustedCert(cert, factory));
    }

    @Override
    protected String processUserInput( final String userInput ) {
        if ( ! startsWithScheme(userInput)) {
            Pair<String, String> hostAndPort = InetAddressUtil.getHostAndPort(userInput, Integer.toString(DEFAULT_TRUSTED_CERT_PORT));
            return "https://" + hostAndPort.left + ":" + hostAndPort.right;
        } else {
            return userInput;
        }
    }

    private boolean startsWithScheme(String userInput) {
        return userInput != null && userInput.indexOf(':') > -1 && userInput.charAt(0) != '[' && ! InetAddressUtil.isValidIpv6Address(userInput);
    }
}
