/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import com.l7tech.common.protocol.SecureSpanConstants;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Download a certificate from the SSG, check it for validity, and import it.
 *
 * Typical usage:
 * <code>
 * CertificateDownloader cd = new CertificateDownloader();
 * cd.setSsgUrl("http://wherever:8080");
 * cd.setUsername("alice");
 * cd.setPassword("sekrit".toCharArray());
 * try {
 *   boolean result = cd.downloadCertificate();
 *   Certificate cert = cd.getCertificate();
 *   if (result) {
 *     // Certificate signature was validated with the given username and password
 *   } else {
 *     // A certificate was obtained, but it could not be validated
 *   }
 *   if (cd.isUserUnknown()) { }
 * } catch (Exception e) {
 *   // certificate download was unsuccessful.  cd.getCertificate() will probably return null.
 * }
 *
 * if (!cd.isValidCert()) {
 *   cd.setPassword("otherPass");  // You can try other passwords on the downloaded cert.
 *                                 // To try a different username though you'll need to downloadCertificate() again.
 *   if (!cd.isValidCert()) {
 *     // oh well
 *   }
 * }
 * </code>
 *
 * User: mike
 * Date: Jul 15, 2003
 * Time: 1:40:08 PM
 */
public class CertificateDownloader {
    private static final int CHECK_PREFIX_LENGTH = SecureSpanConstants.HttpHeaders.CERT_CHECK_PREFIX.length();

    private String username = null;
    private char[] password = null;
    private URL ssgUrl = null;
    private X509Certificate cert = null;
    private byte[] certBytes = null;
    private List checks = Collections.EMPTY_LIST;
    private String nonce = "";
    private boolean sawNoPass = false;

    public CertificateDownloader() {
    }

    public CertificateDownloader(URL ssgUrl, String username, char[] password) {
        setSsgUrl(ssgUrl);
        setUsername(username);
        setPassword(password);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    private String getHa1(String realm) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        }
        md5.reset();
        md5.update((username == null ? "" : username).getBytes());
        md5.update(":".getBytes());
        md5.update((realm == null ? "" : realm).getBytes());
        md5.update(":".getBytes());
        md5.update((password == null ? "" : new String(password)).getBytes());
        return HexUtils.encodeMd5Digest(md5.digest());
    }

    public void setSsgUrl(URL ssgUrl) {
        try {
            this.ssgUrl = new URL(ssgUrl.getProtocol(), ssgUrl.getHost(), ssgUrl.getPort(), SecureSpanConstants.CERT_PATH);
        } catch (MalformedURLException e) {
            // can't happen
        }
    }

    public X509Certificate getCertificate() {
        return cert;
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
            MessageDigest md5;
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e); // can't happen
            }
            byte[] ha1 = getHa1(realm).getBytes();
            md5.reset();
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
     * and validating the result with the specified password.
     *
     * @return true if the downloaded certificate checked out OK.  Use getCertificate() to get it either way.
     * @throws IOException in case of network trouble
     * @throws CertificateException if the returned certificate can't be parsed
     */
    public boolean downloadCertificate() throws IOException, CertificateException {
        if (ssgUrl == null)
            throw new IllegalStateException("No Gateway url is set");
        nonce = String.valueOf(Math.abs(new SecureRandom().nextLong()));
        String uri = ssgUrl.getPath() + "?" + "getcert=1&nonce=" + nonce;
        if (username != null)
            uri += "&username=" + URLEncoder.encode(username, "UTF-8");

        URL remote = null;
        remote = new URL(ssgUrl.getProtocol(), ssgUrl.getHost(), ssgUrl.getPort(), uri);
        HexUtils.Slurpage result = HexUtils.slurpUrl(remote);
        certBytes = result.bytes;
        Map headers = result.headers;
        cert = CertUtils.decodeCert(certBytes);
        this.checks = new ArrayList();

        sawNoPass = false;
        for (Iterator i = headers.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            List list = (List) headers.get(key);
            String value = (String) list.get(0);
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
        return isValidCert();
    }

    /**
     * Check whether the currently-downloaded cert validates given the current password, given the username
     * used to download it.
     *
     * This checks the password against every Cert-Check-NNN: header present when the cert was downloaded.
     *
     * @return true if the cert checks out.
     */
    public boolean isValidCert() {
        for (Iterator i = checks.iterator(); i.hasNext();) {
            CheckInfo checkInfo = (CheckInfo) i.next();
            if (checkInfo.checkCert())
                return true;
        }

        // downloaded certificate was not trusted
        return false;
    }

    /**
     * Check whether the specified username was known to the SSG.
     * @return false if the SSG recognized the username; otherwise true
     */
    public boolean isUserUnknown() {
        return checks.size() < 1 && !sawNoPass;
    }

    /**
     * Check whether an account with this username exists but whose password isn't available to the SSG.
     * @return true if at least one Cert-Check-NNN: header contatining "NOPASS" was in the response, or no password
     *         was provided when this CertificateDownloader was instantiated, or no Cert-Check headers at all
     *         were in the response.
     */
    public boolean isUncheckablePassword() {
        return sawNoPass || password == null || checks.size() < 1;
    }
}
