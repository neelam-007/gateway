/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import org.apache.log4j.Category;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    public static final Category log = Category.getInstance(CertificateDownloader.class);
    public static final String CERT_PATH = "/ssg/policy/disco.modulator";

    private String username = null;
    private char[] password = null;
    private URL ssgUrl = null;
    private Certificate cert = null;
    private List checks = Collections.EMPTY_LIST;
    private MessageDigest md5 = null;
    private String nonce = "";

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

    private String getHa1() {
        MessageDigest md5 = getMd5();
        md5.reset();
        md5.update((username == null ? "" : username).getBytes());
        md5.update(":".getBytes());
        md5.update((password == null ? "" : new String(password)).getBytes());
        return HexUtils.encodeMd5Digest(md5.digest());
    }

    public void setSsgUrl(URL ssgUrl) {
        try {
            this.ssgUrl = new URL(ssgUrl.getProtocol(), ssgUrl.getHost(), ssgUrl.getPort(), CERT_PATH);
        } catch (MalformedURLException e) {
            // can't happen
        }
    }

    public Certificate getCertificate() {
        return cert;
    }

    private class CheckInfo {
        public String oid;
        public String digest;

        public CheckInfo(String oid, String digest) {
            this.oid = oid;
            this.digest = digest;
            log.info("CheckInfo: oid=" + oid + "  digest=" + digest);
        }

        public boolean checkCert(String nonce, String ha1) {
            MessageDigest md5 = getMd5();
            md5.reset();
            md5.update(nonce.getBytes());
            md5.update(String.valueOf(oid).getBytes());
            try {
                md5.update(cert.getEncoded());
            } catch (CertificateEncodingException e) {
                throw new RuntimeException(e); // can't happen
            }
            md5.update(ha1.getBytes());
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
            throw new IllegalStateException("No SSG url is set");

        try {
            nonce = String.valueOf(Math.abs(SecureRandom.getInstance("SHA1PRNG").nextLong()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        }
        String uri = ssgUrl.getPath() + "?" + "getcert=1&nonce=" + nonce;
        if (username != null)
            uri += "&username=" + URLEncoder.encode(username, "UTF-8");

        URL remote = null;
        URLConnection conn = null;
        InputStream connStream = null;
        try {
            remote = new URL(ssgUrl.getProtocol(), ssgUrl.getHost(), ssgUrl.getPort(), uri);
            log.info("Connecting to " + remote);
            conn = remote.openConnection();
            conn.setAllowUserInteraction(false);
            conn.connect();
            connStream = conn.getInputStream();
            Map headers = conn.getHeaderFields();
            //log.info("Got headers: " + headers);
            byte[] certBytes = HexUtils.slurpStream(connStream, 16384);
            ByteArrayInputStream bais = new ByteArrayInputStream(certBytes);
            Certificate cert = CertificateFactory.getInstance("X.509").generateCertificate(bais);
            this.cert = cert;
            this.checks = new ArrayList();

            for (Iterator i = headers.keySet().iterator(); i.hasNext();) {
                String key = (String) i.next();
                List list = (List) headers.get(key);
                if (key == null || key.length() < 12 || !key.substring(0, 11).equals("Cert-Check-"))
                    continue;
                String idp = key.substring(11);
                String value = (String) list.get(0);
                checks.add(new CheckInfo(idp, value));
            }
            log.info("Got back " + checks.size() + " Cert-Check: headers.");
        } catch (MalformedURLException e) {
            // can't happen
        } finally {
            if (connStream != null)
                connStream.close();
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
        String ha1 = getHa1();
        for (Iterator i = checks.iterator(); i.hasNext();) {
            CheckInfo checkInfo = (CheckInfo) i.next();
            if (checkInfo.checkCert(nonce, ha1))
                return true;
        }
        return false;
    }

    /**
     * Check whether the specified username was known to the SSG.
     * Returns true, unless at least one Cert-Check-NNN: header was returned.
     * @return
     */
    public boolean isUserUnknown() {
        return checks.size() < 1;
    }

    private MessageDigest getMd5() {
        if (md5 == null) {
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
        return md5;
    }
}
