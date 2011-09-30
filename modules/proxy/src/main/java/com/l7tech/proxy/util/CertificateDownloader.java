package com.l7tech.proxy.util;

import com.l7tech.common.http.*;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.util.Charsets;
import com.l7tech.util.ConfigFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Download a certificate from the SSG and check it for validity, if possible.
 */
public class CertificateDownloader {
    public static final String GATEWAY_CERT_DISCOVERY_ERROR = "Gateway certificate discovery service returned an unexpected error: ";

    private static final Logger logger = Logger.getLogger(CertificateDownloader.class.getName());
    private static final boolean ENABLE_PRE60 = ConfigFactory.getBooleanProperty( "com.l7tech.proxy.util.enablePre60CertDisco", true );

    private final SimpleHttpClient httpClient;
    private final URL ssgUrl;
    private final String username;
    private final char[] password;

    private byte[] certBytes = null;
    private List<Pre60CertificateCheckInfo> pre60CheckInfos = Collections.emptyList();
    private List<CertificateCheck2Info> check2Infos = Collections.emptyList();
    private String nonce = "";
    private boolean sawNoPass = false;
    private static final SecureRandom rand = new SecureRandom();

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
        nonce = getClientNonce();
        String uri = SecureSpanConstants.CERT_PATH + "?" + "getcert=1&nonce=" + nonce;
        if (username != null)
            uri += "&username=" + URLEncoder.encode(username, "UTF-8");

        URL remote = new URL(ssgUrl.getProtocol(), ssgUrl.getHost(), ssgUrl.getPort(), uri);

        GenericHttpRequestParams params = new GenericHttpRequestParams(remote);
        SimpleHttpClient.SimpleHttpResponse result = httpClient.get(params);

        certBytes = result.getBytes();
        final ContentTypeHeader ctype = result.getContentType();
        logger.fine("Gateway certificate discovery service returned status " + result.getStatus() + " " + (ctype == null ? "(no Content-Type)" : ctype.toString()));

        if (result.getStatus() != 200) {
            String msg = new String(certBytes, 0, Math.min(certBytes.length, 900), ctype == null ? Charsets.UTF8 : ctype.getEncoding());
            throw new IOException(GATEWAY_CERT_DISCOVERY_ERROR + msg);
        }
        X509Certificate cert = CertUtils.decodeCert(certBytes);

        HttpHeaders headerHolder = result.getHeaders();
        HttpHeader[] headers = headerHolder.toArray();

        this.pre60CheckInfos = new ArrayList<Pre60CertificateCheckInfo>();
        boolean sawPre60NoPass = false;
        if (ENABLE_PRE60) for (HttpHeader header : headers) {
            Pre60CertificateCheckInfo pre60CheckInfo = Pre60CertificateCheckInfo.parseHttpHeader(header);
            if (pre60CheckInfo != null) {
                if (pre60CheckInfo.isNoPass()) {
                    sawPre60NoPass = true;
                } else {
                    this.pre60CheckInfos.add(pre60CheckInfo);
                }
            }
        }

        this.check2Infos = new ArrayList<CertificateCheck2Info>();
        boolean sawAnyCheck2Header = false;
        boolean sawCheck2NoPass = false;
        for (HttpHeader header : headers) {
            CertificateCheck2Info check2Info = CertificateCheck2Info.parseHttpHeader(header);
            if (check2Info != null) {
                sawAnyCheck2Header = true;
                if (check2Info.isNoPass()) {
                    sawCheck2NoPass = true;
                } else {
                    this.check2Infos.add(check2Info);
                }
            }
        }

        this.sawNoPass = sawAnyCheck2Header ? sawCheck2NoPass : sawPre60NoPass;

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

        byte[] nonceBytes = nonce.getBytes(Charsets.UTF8);
        for (CertificateCheck2Info check2Info : check2Infos) {
            if (check2Info.checkCert(certBytes, password, nonceBytes))
                return true;
        }

        for (Pre60CertificateCheckInfo pre60CheckInfo : pre60CheckInfos) {
            if (pre60CheckInfo.checkCert(certBytes, username, password, nonce))
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

        return sawNoPass || password == null || (pre60CheckInfos.size() < 1 && check2Infos.size() < 1);
    }

    /**
     * @return the next nonce value.  Never null.
     */
    protected String getClientNonce() {
        return String.valueOf(Math.abs(rand.nextLong()));
    }
}
