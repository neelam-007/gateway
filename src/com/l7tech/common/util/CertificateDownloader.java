/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.GenericHttpRequestParamsImpl;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.common.protocol.SecureSpanConstants;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
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
    private static final int CHECK_PREFIX_LENGTH = SecureSpanConstants.HttpHeaders.CERT_CHECK_PREFIX.length();

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

    private String getHa1(String realm) {
        MessageDigest md5 = HexUtils.getMd5();
        md5.update((username == null ? "" : username).getBytes());
        md5.update(":".getBytes());
        md5.update((realm == null ? "" : realm).getBytes());
        md5.update(":".getBytes());
        md5.update((password == null ? "" : new String(password)).getBytes());
        return HexUtils.encodeMd5Digest(md5.digest());
    }

    private class CheckInfo {
        public String oid;
        public String digest;
        public String realm;

        public CheckInfo(String oid, String digest, String realm) {
            this.oid = oid;
            this.digest = digest;
            this.realm = realm;
        }

        public boolean checkCert() {
            MessageDigest md5 = HexUtils.getMd5();
            byte[] ha1 = getHa1(realm).getBytes();
            md5.update(ha1);
            md5.update(nonce.getBytes());
            md5.update(oid.getBytes());
            md5.update(certBytes);
            md5.update(ha1);
            String desiredValue = HexUtils.encodeMd5Digest(md5.digest());
            return desiredValue.equals(digest);
        }
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

        HttpHeader[] headers = result.getHeaders();
        this.checks = new ArrayList();

        sawNoPass = false;
        for (int i = 0; i < headers.length; i++) {
            HttpHeader header = headers[i];
            String key = header.getName();
            String value = header.getFullValue();
            if (key == null || key.length() <= CHECK_PREFIX_LENGTH ||
                    !key.substring(0, CHECK_PREFIX_LENGTH).equalsIgnoreCase(
                            SecureSpanConstants.HttpHeaders.CERT_CHECK_PREFIX))
                continue;
            String idp = key.substring(CHECK_PREFIX_LENGTH);
            int semiPos = value.indexOf(';');
            if (semiPos < 0) {
                // Check header was badly formatted -- continuing
                continue;
            }
            String hash = value.substring(0, semiPos);
            String realm = value.substring(semiPos + 1);
            if (realm.substring(0, 1).equals(" "))
                realm = realm.substring(1);

            if (SecureSpanConstants.NOPASS.equals(hash))
                sawNoPass = true;
            else
                checks.add(new CheckInfo(idp, hash, realm));
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
            CheckInfo checkInfo = (CheckInfo) i.next();
            if (checkInfo.checkCert())
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
