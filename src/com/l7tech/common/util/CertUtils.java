/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import java.util.ArrayList;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.DSAParams;

/**
 *
 * @author mike
 * @version 1.0
 */
public class CertUtils {
    /** Returns a properties instance filled out with info about the certificate. */
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
}
