/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.common.io.PermissiveHostnameVerifier;
import com.l7tech.common.io.PermissiveSSLSocketFactory;
import com.l7tech.gateway.config.client.ConfigurationException;
import com.l7tech.gateway.config.client.beans.UrlConfigurableBean;
import com.l7tech.gateway.config.client.beans.ConfigResult;
import com.l7tech.gateway.config.client.beans.ConfigurationContext;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/** @author alex */
public class TrustedCertUrl extends UrlConfigurableBean {
    private volatile X509Certificate cert;

    TrustedCertUrl() {
        super("host.controller.remoteNodeManagement.tempTrustedCertUrl", "SSL URL", null, "https");
    }

    public void validate(URL value) throws ConfigurationException {
        URLConnection conn;
        try {
            conn = value.openConnection();
            if (!(conn instanceof HttpsURLConnection)) throw new ConfigurationException("Provided URL did not result in an HTTPS connection");

            HttpsURLConnection httpsURLConnection = (HttpsURLConnection)conn;
            httpsURLConnection.setSSLSocketFactory(new PermissiveSSLSocketFactory());
            httpsURLConnection.setHostnameVerifier(new PermissiveHostnameVerifier());
            httpsURLConnection.connect();
            Certificate[] certs = httpsURLConnection.getServerCertificates();
            if (certs == null || certs.length < 1) throw new ConfigurationException("Server presented no certificates");
            if (!(certs[0] instanceof X509Certificate)) throw new ConfigurationException("Server certificate wasn't X.509");
            cert = (X509Certificate)certs[0];
        } catch (IOException e) {
            throw new ConfigurationException("Unable to connect to provided URL", e);
        }
    }

    @Override
    public ConfigResult onConfiguration(URL value, ConfigurationContext context) {
        if (cert == null) throw new IllegalStateException("cert is null");
        return ConfigResult.chain(new ConfirmTrustedCert(cert));
    }
}
