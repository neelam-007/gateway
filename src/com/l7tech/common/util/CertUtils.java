/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import javax.security.auth.x500.X500Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.MessageDigest;
import java.security.Principal;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.DSAParams;

/**
 *
 * @author mike
 * @version 1.0
 */
public class CertUtils {
    public static final String X509_OID_SUBJECTKEYID = "2.5.29.14";

    /**
     * Display structured information about a certificate.
     *
     * @param cert The certificate to analyze
     * @return a single multi-line string that can be printed out
     * @throws CertificateEncodingException if the cert could not be decoded
     * @throws NoSuchAlgorithmException if the cert required an algorithm that is not available
     */
    public static String toString(X509Certificate cert) throws CertificateEncodingException, NoSuchAlgorithmException {
        StringBuffer sb = new StringBuffer();
        List p = getCertProperties(cert);
        for (Iterator i = p.iterator(); i.hasNext();) {
            String[] s = (String[]) i.next();
            String label = s[0];
            String value = s[1];
            sb.append(label + ": " + value + "\n");
        }
        return sb.toString();
    }

    /**
     * Obtain structured information about a certificate in an easy-to-display tabular format.
     *
     * @param cert The certificate to analyze
     * @return a list of String[] tuples, where each is of the form {"Label", "Value"}
     * @throws CertificateEncodingException if the cert could not be decoded
     * @throws NoSuchAlgorithmException if the cert required an algorithm that is not available
     */
    public static ArrayList getCertProperties(X509Certificate cert)
      throws CertificateEncodingException, NoSuchAlgorithmException {
        ArrayList l = new ArrayList();
        if (cert == null) return l;

        // l.add(new String[]{"Revocation date", new Date().toString()});
        l.add(new String[]{"Creation date", nullNa(cert.getNotBefore())});
        l.add(new String[]{"Expiry date", nullNa(cert.getNotAfter())});
        l.add(new String[]{"Issued to", nullNa(cert.getSubjectDN())});
        l.add(new String[]{"Serial number", nullNa(cert.getSerialNumber())});
        l.add(new String[]{"Issuer", nullNa(cert.getIssuerDN())});

        l.add(new String[]{"SHA-1 fingerprint", getCertificateFingerprint(cert, "SHA1")});
        l.add(new String[]{"MD5 fingerprint", getCertificateFingerprint(cert, "MD5")});

        PublicKey publicKey = cert.getPublicKey();
        l.add(new String[]{"Key type", nullNa(publicKey.getAlgorithm())});

        if (publicKey != null && publicKey instanceof RSAPublicKey) {
            RSAPublicKey rsaKey = (RSAPublicKey) publicKey;
            String modulus = rsaKey.getModulus().toString(16);
            l.add(new String[]{"RSA strength", (modulus.length() * 4) + " bits"});
            //l.add(new String[]{"RSA modulus", modulus});
            l.add(new String[]{"RSA public exponent", rsaKey.getPublicExponent().toString(16)});
        } else if (publicKey != null && publicKey instanceof DSAPublicKey) {
            DSAPublicKey dsaKey = (DSAPublicKey) publicKey;
            DSAParams params = dsaKey.getParams();
            l.add(new String[]{"DSA prime (P)", params.getP().toString(16)});
            l.add(new String[]{"DSA subprime (P)", params.getQ().toString(16)});
            l.add(new String[]{"DSA base (P)", params.getG().toString(16)});
        }

        return l;
    }

    /**
     * The method creates the fingerprint and returns it in a
     * String to the caller.
     *
     * @param cert      the certificate
     * @param algorithm the alghorithm (MD5 or SHA1)
     * @return the certificate fingerprint as a String
     * @exception NoSuchAlgorithmException
     *                      if the algorithm is not available.
     * @exception CertificateEncodingException
     *                      thrown whenever an error occurs while attempting to
     *                      encode a certificate.
     */
    public static String getCertificateFingerprint(X509Certificate cert, String algorithm)
      throws NoSuchAlgorithmException, CertificateEncodingException {
        if (cert == null) {
            throw new NullPointerException("cert");
        }
        StringBuffer buff = new StringBuffer();
        byte[] fingers = cert.getEncoded();

        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(fingers);
        byte[] digest = md.digest();
        // the algorithm
        buff.append(algorithm + ":");

        for (int i = 0; i < digest.length; i++) {
            if (i != 0) buff.append(":");
            int b = digest[i] & 0xff;
            String hex = Integer.toHexString(b);
            if (hex.length() == 1) buff.append("0");
            buff.append(hex.toUpperCase());
        }
        return buff.toString();
    }

    /** Convert a null object into "N/A", otherwise toString */
    private static String nullNa(Object o) {
        return o == null ? "N/A" : o.toString();
    }

    public static class CertificateUntrustedException extends Exception {
        private CertificateUntrustedException(String message) {
            super(message);
        }
    }

    /**
     * Verifies that each cert in the specified certificate chain is signed by the next certificate
     * in the chain, and that at least one is <em>signed by or identical to</em> trustedCert.
     *
     * @param chain An array of one or more {@link X509Certificate}s to check
     * @param trustedCert A trusted {@link X509Certificate} to check the chain against
     * @param maxDepth How many levels deep to search for trust.  A recommended value for this is 1, otherwise
     *                 certain attacks may become possible.
     * @throws CertUtils.CertificateUntrustedException if the chain could not be validated with the specified
     *                                                       trusted certificate, but the chain otherwise appears to
     *                                                       be internally consistent and might validate later if a
     *                                                       different trusted certificate is used.
     * @throws CertificateExpiredException if one of the certs in the chain has expired
     * @throws CertificateException if the chain is seriously invalid and cannot be trusted
     */
    public static void verifyCertificateChain( X509Certificate[] chain,
                                               X509Certificate trustedCert,
                                               int maxDepth )
            throws CertificateException, CertificateExpiredException, CertificateUntrustedException
    {

        Principal trustedDN = trustedCert.getSubjectDN();

        for (int i = 0; i < maxDepth; i++) {
            X509Certificate cert = chain[i];
            cert.checkValidity(); // will abort if this throws
            if (i + 1 < chain.length) {
                try {
                    cert.verify(chain[i + 1].getPublicKey());
                } catch (Exception e) {
                    // This is a serious problem with the cert chain presented by the peer.  Do a full abort.
                    throw new CertificateException("Unable to verify signature in peer certificate chain: " + e);
                }
            }

            if (cert.getIssuerDN().equals(trustedDN)) {
                try {
                    cert.verify(trustedCert.getPublicKey());
                    return; // success
                } catch (Exception e) {
                    // Server SSL cert might have changed.  Attempt to reimport it
                    throw new CertificateUntrustedException("Unable to verify peer certificate with trusted cert: " + e);
                }
            } else if (cert.getSubjectDN().equals(trustedDN)) {
                if (cert.equals(trustedCert)) {
                    return; // success
                }
            }
        }

        // We probably just havne't talked to this Ssg before.  Trigger a reimport of the certificate.
        throw new CertificateUntrustedException("Couldn't find trusted certificate in peer's certificate chain");
    }

    /**
     * Extract the username from the specified client certificate.  The certificate is expected to contain
     * a distinguished name in the format "CN=username".  Other formats are not supported.
     * @param cert the certificate to examine
     * @return the username from the certificate.  Might be empty string, but won't be null.
     * @throws IllegalArgumentException if the certificate does not contain DN of "CN=username"
     * TODO: The certificate format restrictions will need to be relaxed to allow arbitrary client certificates
     */
    public static String extractUsernameFromClientCertificate(X509Certificate cert) throws IllegalArgumentException {
        Principal principal = cert.getSubjectDN();
        if (principal == null)
            throw new IllegalArgumentException("Cert contains no user subject DN");
        return extractCommonName(principal);
    }

    /**
     * Extract the username from the specified client certificate.  The certificate is expected to contain
     * a distinguished name in the format "CN=username".  Other formats are not supported.
     * @param cert the certificate to examine
     * @return the username from the certificate.  Might be empty string, but won't be null.
     * @throws IllegalArgumentException if the certificate does not contain DN of "CN=username"
     * TODO: The certificate format restrictions will need to be relaxed to allow arbitrary client certificates
     */
    public static String extractIssuerNameFromClientCertificate (X509Certificate cert) throws IllegalArgumentException {
        Principal principal = cert.getIssuerDN();
        if (principal == null)
            throw new IllegalArgumentException("Cert contains no issuer subject DN");
        return extractCommonName(principal);
    }

    /**
     * Extract the value of the CN attribute from the DN in the Principal.
     * @param principal
     * @return String  The value of CN attribute in the DN
     */
    private static String extractCommonName(Principal principal) {
        X500Principal certName = new X500Principal(principal.toString());
        String certNameString = certName.getName(X500Principal.RFC2253);
        if (certNameString == null || !certNameString.substring(0, 3).equalsIgnoreCase("cn="))
            throw new IllegalArgumentException("Cert subject DN is not in the format CN=username");

        String username = "";
        int endIndex = certNameString.indexOf(",");
        if (endIndex > 0) {
            username = certNameString.substring(3, endIndex);
        } else {
            username = certNameString.substring(3, certNameString.length());
        }

        return username;
    }
}
