/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans.trust;

import com.l7tech.common.io.PermissiveHostnameVerifier;
import com.l7tech.common.io.PermissiveSSLSocketFactory;
import com.l7tech.config.client.ConfigurationException;
import com.l7tech.gateway.config.client.beans.UrlConfigurableBean;
import com.l7tech.gateway.config.client.beans.ConfigResult;
import com.l7tech.gateway.config.client.beans.ConfigurationContext;
import com.l7tech.util.SyspropUtil;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

/** @author alex */
public class TrustedCertUrl extends UrlConfigurableBean {
    private volatile X509Certificate cert;

    TrustedCertUrl() {
        super("host.controller.remoteNodeManagement.tempTrustedCertUrl", "HTTPS URL", null, "https");
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
        return ConfigResult.chain(new ConfirmTrustedCert(cert));
    }

    @Override
    protected String processUserInput( final String userInput ) {
        String value = userInput;

        if ( !userInput.startsWith("https://") && (
                !Pattern.matches("^[a-zA-Z]{1,10}:.*", userInput) ||
                userInput.startsWith("localhost")
        )) {
            if ( value.indexOf(':') < 0 ) {
                value += ":8182";
            }
            value = "https://" + value;
        }

        return value;
    }
}
