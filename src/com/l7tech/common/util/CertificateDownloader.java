/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import com.l7tech.common.http.*;
import com.l7tech.common.protocol.SecureSpanConstants;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Download a certificate from the SSG and check it for validity, if possible.
 */
public class CertificateDownloader {
    private final SimpleHttpClient httpClient;
    private final URL ssgUrl;
    private final String username;
    private final char[] password;

    private byte[] certBytes = null;
    private List checks = Collections.EMPTY_LIST;
    private String nonce = "";
    private boolean sawNoPass = false;

    /**
     * Create a new CertificateDownloader using the specified HTTP client, target URL, and credentials.
     *
     * @param httpClient  the HTTP client to use.  Must not be null.
     * @param ssgUrl      the URL of the certificate discovery service.  Must not be null.
     * @param username    username to use to check the downloaded cert, or null to disable this feature.
     * @param password    password to use to check the downloaded cert, or null to disable this feature.
     */
    public CertificateDownloader(SimpleHttpClient httpClient, URL ssgUrl, String username, char[] password) {
        if (httpClient == null || ssgUrl == null) throw new NullPointerException();
        this.httpClient = httpClient;
        this.ssgUrl = ssgUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * Download a certificate from the currently-configured ssgUrl, providing the specified username
     * and preparing to validate the result with the specified password.
     *
     * @return the downloaded certificate.  Never null.
     * @throws IOException in case of network trouble
     * @throws CertificateException if the returned certificate can't be parsed
     */
    public X509Certificate downloadCertificate() throws IOException, CertificateException {
        if (ssgUrl == null)
            throw new IllegalStateException("No Gateway url is set");
        certBytes = null;
        nonce = String.valueOf(Math.abs(new SecureRandom().nextLong()));
        String uri = SecureSpanConstants.CERT_PATH + "?" + "getcert=1&nonce=" + nonce;
        if (username != null)
            uri += "&username=" + URLEncoder.encode(username, "UTF-8");

        URL remote = new URL(ssgUrl.getProtocol(), ssgUrl.getHost(), ssgUrl.getPort(), uri);

        GenericHttpRequestParams params = new GenericHttpRequestParamsImpl(remote);
        SimpleHttpClient.SimpleHttpResponse result = httpClient.get(params);

        certBytes = result.getBytes();
        X509Certificate cert = CertUtils.decodeCert(certBytes);

        HttpHeaders headerHolder = result.getHeaders();
        HttpHeader[] headers = headerHolder.toArray();
        this.checks = new ArrayList();

        sawNoPass = false;
        for (int i = 0; i < headers.length; i++) {
            CertificateCheckInfo checkInfo = CertificateCheckInfo.parseHttpHeader(headers[i]);
            if (checkInfo != null) {
                if (checkInfo.isNoPass()) {
                    sawNoPass = true;
                } else {
                    checks.add(checkInfo);
                }
            }
        }
        return cert;
    }

    /**
     * Check whether the currently-downloaded cert validates given the current password, given the username
     * used to download it.
     *
     * This checks the password against every Cert-Check-NNN: header present when the cert was downloaded.
     *
     * @return true if the cert checks out.
     * @throws IllegalStateException if {@link #downloadCertificate()} has not successfully returned a certificate.
     */
    public boolean isValidCert() {
        if (certBytes == null) throw new IllegalStateException();

        for (Iterator i = checks.iterator(); i.hasNext();) {
            CertificateCheckInfo certificateCheckInfo = (CertificateCheckInfo) i.next();
            if (certificateCheckInfo.checkCert(certBytes, username, password, nonce))
                return true;
        }

        // downloaded certificate was not trusted
        return false;
    }

    /**
     * Check whether an account with this username exists but whose password isn't available to the SSG.
     * @return true if at least one Cert-Check-NNN: header contatining "NOPASS" was in the response, or no password
     *         was provided when this CertificateDownloader was instantiated, or no Cert-Check headers at all
     *         were in the response.
     * @throws IllegalStateException if {@link #downloadCertificate()} has not successfully returned a certificate.
     */
    public boolean isUncheckablePassword() {
        if (certBytes == null) throw new IllegalStateException();

        return sawNoPass || password == null || checks.size() < 1;
    }
}
