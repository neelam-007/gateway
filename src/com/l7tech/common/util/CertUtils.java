/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import com.l7tech.common.security.CertificateExpiry;

import javax.security.auth.x500.X500Principal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.*;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.DERObjectIdentifier;

/**
 *
 * @author mike
 * @version 1.0
 */
public class CertUtils {
    /**
     * Checks the validity period of the specified certificate.
     * @return a {@link CertificateExpiry} indicating how many days remain before the certificate will expire
     * @throws CertificateNotYetValidException if the certificate's "not-before" is after the current time
     * @throws CertificateExpiredException if the certificate's "not-after" was before the current time
     */
    public static CertificateExpiry checkValidity( X509Certificate certificate )
            throws CertificateNotYetValidException, CertificateExpiredException
    {
        certificate.checkValidity();
        int days = (int)(.5f + ((System.currentTimeMillis() - certificate.getNotAfter().getTime()) * 1000 * 86400));
        return new CertificateExpiry(days);
    }

    /**
     * Constructs a {@link Map} based on an X.500 distinguished name.
     *
     * The keys in the map are are upper-case X.500 attribute names (e.g. CN, DC, UID, etc.)
     * where possible, otherwise a String containing an OID.'
     *
     * The values are a {@link List} of values for that attribute (they are frequently multivalued)
     * 
     * @param dn the X.500 DN to parse
     * @return a Map of attribute names to value lists
     */
    public static Map dnToAttributeMap(String dn) {
        X509Name x509name = new X509Name(dn);
        Map map = new HashMap();
        for (int i = 0; i < x509name.getOIDs().size(); i++ ) {
            final DERObjectIdentifier oid = (DERObjectIdentifier)x509name.getOIDs().get(i);

            String name = (String)X509Name.DefaultSymbols.get(oid);
            if (name == null) name = (String)X509Name.RFC2253Symbols.get(oid);
            if (name == null) name = oid.getId();

            List values = (List) map.get(name);
            if ( values == null ) {
                values = new ArrayList();
                map.put(name, values);
            }
            String value = (String)x509name.getValues().get(i);
            values.add(value);
        }
        return map;
    }

    /**
     * Tests whether the provided DN matches the provided pattern.
     * <p>
     * If the pattern has "*" for any
     * attribute value, the DN will match if it has any value for the attribute with the same name.
     * </p><p>
     * The DN matches if and only if: <ul compact>
     * <li>Every attribute in the pattern is also present in the DN,
     *     <b>even if the pattern's value is "*"</b>;</li>
     * <li>Every attribute in the pattern whose value isn't "*" is present
     *     <b>with the same value</b> in the DN;</li>
     * </ul>
     * </p><p>
     * Note that the DN can have additional attributes that are not present
     * in the pattern and can still be considered to match if the rules are met.</p>
     * @param dn the dn to be matched
     * @param pattern the pattern to match against
     * @return true if the dn matches the pattern, false otherwise.
     */
    public static boolean dnMatchesPattern(String dn, String pattern) {
        Map dnMap = dnToAttributeMap(dn);
        Map patternMap = dnToAttributeMap(pattern);

        boolean matches = true;
        for ( Iterator i = patternMap.keySet().iterator(); i.hasNext(); ) {
            String oid = (String)i.next();
            List patternValues = (List)patternMap.get(oid);
            List dnValues = (List)dnMap.get(oid);

            if ( dnValues == null ) {
                matches = false;
                break;
            }

            for ( Iterator j = patternValues.iterator(); j.hasNext(); ) {
                String patternValue = (String) j.next();
                if ( !dnValues.contains(patternValue) ) {
                    if ( !("*".equals(patternValue)) ) {
                        matches = false;
                        break;
                    }
                }
            }
        }

        return matches;
    }

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

        l.add(new String[]{"Key usage", keyUsageToString(cert.getKeyUsage())});

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

    private static final String[] KEY_USAGES = {
        "digitalSignature",
        "nonRepudiation",
        "keyEncipherment",
        "dataEncipherment",
        "keyAgreement",
        "keyCertSign",
        "cRLSign",
        "encipherOnly",
        "decipherOnly",
    };

    /**
     * @return A string such as "KeyEncipherment, caCert" that describes enabled key usages for a cert, or
     * "<None premitted>" if no usage bits are enabled, or
     * "<Not present>" if there is no key usage extension.
     */
    private static String keyUsageToString(boolean[] ku) {
        if (ku == null) return "<Not present>";
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (int i = 0; i < ku.length; i++) {
            if (ku[i]) {
                String keyUsage = i < KEY_USAGES.length ? KEY_USAGES[i] : "<bit " + i + ">";
                if (!first) sb.append(", ");
                sb.append(keyUsage);
                first = false;
            }
        }

        String kus = sb.toString();
        return kus.length() < 1 ? "<None permitted>" : kus;
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

            if (cert.getIssuerDN().toString().equals(trustedDN.toString())) {
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
        if (certNameString == null)
            throw new IllegalArgumentException("Cert subject DN is NULL");

        String cn = "";
        int index1 = certNameString.indexOf("cn=");
        int index2 = certNameString.indexOf("CN=");
        int startIndex = -1;
        int endIndex = -1;

        if (index1 >= 0) {
            startIndex = index1 + 3;
        } else if (index2 >= 0) {
            startIndex = index2 + 3;
        } else {
            throw new IllegalArgumentException("Cert subject DN is not in the format CN=username");
        }

        if (startIndex >= 0) {
            endIndex = certNameString.indexOf(",", startIndex);
            if (endIndex > 0) {
                cn = certNameString.substring(startIndex, endIndex);
            } else {
                cn = certNameString.substring(startIndex, certNameString.length());
            }
        }

        return cn;
    }
}
