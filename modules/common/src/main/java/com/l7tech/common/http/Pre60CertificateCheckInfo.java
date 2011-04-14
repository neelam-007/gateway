/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.util.HexUtils;

import java.io.Serializable;

/**
 * <b>Note: obsolete as of SecureSpan 6.0, but preserved in XVC (for now) for backward compatibility with pre-6.0 Gateways.</b>
 *
 * Represents a digest that can be used to check if a certificate is valid given a particular username, password, and nonce.
 */
public class Pre60CertificateCheckInfo implements Serializable {
    private static final long serialVersionUID = -2918236748329938127L;
    private static final int CHECK_PREFIX_LENGTH = SecureSpanConstants.HttpHeaders.LEGACY_CERT_CHECK_PREFIX.length();

    private final String oid;       // identity provider OID.
    private final String digest;    // digest of the certificate computed by the originator of this CertificateCheckInfo.
    private final String realm;     // realm.

    /**
     * Create a new CertificateCheckInfo using the specified parameters.
     *
     * @param oid       the Identity Provider OID to hash it with.  Must not be null.
     * @param realm     the realm to hash it with.  Must not be null.
     * @param digest    the computed digest.  Must not be null.
     */
    public Pre60CertificateCheckInfo(String oid, String digest, String realm) {
        this.oid = oid;
        this.digest = digest;
        this.realm = realm;
    }

    /**
     * Create a new CertificateCheckInfo for the specified certificate bytes, using the specified parameters to
     * compute a new digest for it.
     *
     * @param certBytes the DER encoded bytes of the certificate to be hashed.  Must not be null.
     * @param ha1       the hex-encoded MD5 of "username:realm:password", ie "fe6f8a9879c251ba28928ba865192816".
     * @param nonce     the client-chosen nonce to hash it with.  Must not be null.
     * @param oid       the Identity Provider OID to hash it with.  Must not be null.
     * @param realm     the realm to hash it with.  Must not be null.
     */
    public Pre60CertificateCheckInfo(byte[] certBytes, String ha1, String nonce, String oid, String realm) {
        this.oid = oid;
        this.realm = realm;
        digest = ha1 == null ? SecureSpanConstants.NOPASS : computeHash(ha1, nonce, certBytes);
    }

    /**
     * Examines the specified HTTP header to see if a valid CertificateCheckInfo can be created out of it and, if so,
     * creates one.
     *
     * @param checkInfoHeader the HTTP header to examine.  Must be non-null, and have a header name starting with
     *                        {@link com.l7tech.common.protocol.SecureSpanConstants.HttpHeaders#LEGACY_CERT_CHECK_PREFIX}.
     * @return a new CertificateCheckInfo, or null if a valid CertificteCheckInfo could not be created
     *         from the specified header.
     */
    public static Pre60CertificateCheckInfo parseHttpHeader(HttpHeader checkInfoHeader) {
        String headerName = checkInfoHeader.getName();
        String headerValue = checkInfoHeader.getFullValue();
        if (headerName == null || headerName.length() <= CHECK_PREFIX_LENGTH ||
                !headerName.substring(0, CHECK_PREFIX_LENGTH).equalsIgnoreCase(
                        SecureSpanConstants.HttpHeaders.LEGACY_CERT_CHECK_PREFIX))
            return null;
        String oid = headerName.substring(CHECK_PREFIX_LENGTH);
        int semiPos = headerValue.indexOf(';');
        if (semiPos < 0) {
            // Check header was badly formatted -- ignore it
            return null;
        }
        String hash = headerValue.substring(0, semiPos);
        String realm = headerValue.substring(semiPos + 1);
        if (realm.substring(0, 1).equals(" "))
            realm = realm.substring(1);

        return new Pre60CertificateCheckInfo(oid, hash, realm);
    }

    /**
     * @param username username.  required.
     * @param password password.  required.
     * @return the H(A1) of the specified username and password, in the current realm.
     */
    private String computeHa1(String username, char[] password) {
        byte[][] toDigest = {
            (username == null ? "" : username).getBytes(),
            ":".getBytes(),
            (realm == null ? "" : realm).getBytes(),
            ":".getBytes(),
            (password == null ? "" : new String(password)).getBytes()
        };
        return HexUtils.encodeMd5Digest(HexUtils.getMd5Digest(toDigest));
    }

    /**
     * Compute the check digest using the current oid and realm and using the specified parameters.
     * Use this version if you have the password.  H(A1) will be computed for you.
     *
     * @param username   username.  required.
     * @param password   password.  required.
     * @param nonce      client nonce.  required.
     * @param certBytes  encoded certificate bytes.  required.
     * @return the computed digest for the specified certBytes, using the current oid and realm and the specified parameters.
     */
    private String computeHash(String username, char[] password, String nonce, byte[] certBytes) {
        String ha1 = computeHa1(username, password);
        return computeHash(ha1, nonce, certBytes);
    }

    /**
     * Compute the check digest using the current oid and realm and using the specified parameters.
     * Use this version if you don't have the password, but you do already possess H(A1).
     *
     * @param ha1       the hex-encoded MD5 of "username:realm:password", ie "fe6f8a9879c251ba28928ba865192816".
     *                  Must not be null.
     * @param nonce     the nonce string provided by the client.  Must not be null.
     * @param certBytes the DER bytes of the certificate that are being checked.  Must not be null.
     * @return the digest as a hex-encoded string, or "NOPASS" if ha1 was null.
     */
    private String computeHash(String ha1, String nonce, byte[] certBytes) {
        byte[][] toDigest = {
            ha1.getBytes(),
            nonce.getBytes(),
            oid.getBytes(),
            certBytes,
            ha1.getBytes()
        };
        return HexUtils.encodeMd5Digest(HexUtils.getMd5Digest(toDigest));
    }

    /**
     * Check the specified certificate against this CertificateCheckInfo.
     *
     * @param certBytes  encoded certificate bytes.  required.
     * @param username   username.  required.
     * @param password   password.  required.
     * @param nonce      client nonce.  required.
     * @return true if the specified certificate bytes hashed with the current oid and realm and with the
     *              specified username, password and nonce results in a digest identical to the current digest.
     */
    public boolean checkCert(byte[] certBytes, String username, char[] password, String nonce) {
        if (isNoPass())
            return false; // can't check "NOPASS" check headers.
        String desiredValue = computeHash(username, password, nonce, certBytes);
        return desiredValue.equals(digest);
    }

    /**
     * Encode this CertificateCheckInfo as an HTTP header.
     *
     * @return this CertificateCheckInfo encoded as an HTTP header.
     */
    public HttpHeader asHttpHeader() {
        final String name = SecureSpanConstants.HttpHeaders.LEGACY_CERT_CHECK_PREFIX + oid;
        final String value = digest + "; " + realm;
        return new GenericHttpHeader(name, value);
    }

    /**
     * Check if this check header's digest could not be computed by the originator because they did not have access
     * to the password.
     *
     * @return true if the current digest is NOPASS.
     */
    public boolean isNoPass() {
        return SecureSpanConstants.NOPASS.equals(digest);
    }

    /** @return the current identity provider OID. */
    public String getOid() {
        return oid;
    }

    /** @return the current realm. */
    public String getRealm() {
        return realm;
    }
}
