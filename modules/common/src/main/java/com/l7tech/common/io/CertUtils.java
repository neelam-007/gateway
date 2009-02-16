/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.io;

import com.l7tech.util.*;
import com.whirlycott.cache.Cache;
import org.apache.harmony.security.asn1.ASN1Integer;
import org.apache.harmony.security.asn1.ASN1Sequence;
import org.apache.harmony.security.asn1.ASN1Type;

import javax.security.auth.x500.X500Principal;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.naming.InvalidNameException;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.*;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mike
 * @version 1.0
 */
public class CertUtils {
    public static final String PEM_CERT_BEGIN_MARKER = "-----BEGIN CERTIFICATE-----";
    public static final String PEM_CERT_END_MARKER = "-----END CERTIFICATE-----";
    public static final String PEM_RSAKEY_BEGIN_MARKER = "-----BEGIN RSA PRIVATE KEY-----";
    public static final String PEM_RSAKEY_END_MARKER = "-----END RSA PRIVATE KEY-----";
    public static final String PEM_DSAKEY_BEGIN_MARKER = "-----BEGIN DSA PRIVATE KEY-----";
    public static final String PEM_DSAKEY_END_MARKER = "-----END DSA PRIVATE KEY-----";
    public static final String PEM_CRL_BEGIN_MARKER = "-----BEGIN X509 CRL-----";
    public static final String PEM_CRL_END_MARKER = "-----END X509 CRL-----";
    private static final String PEM_CSR_BEGIN_MARKER = "-----BEGIN CERTIFICATE REQUEST-----";
    private static final String PEM_CSR_END_MARKER = "-----END CERTIFICATE REQUEST-----";
    private static final String PEM_CSR_BEGIN_NEW_MARKER = "-----BEGIN NEW CERTIFICATE REQUEST-----";
    private static final String PEM_CSR_END_NEW_MARKER = "-----END NEW CERTIFICATE REQUEST-----";

    private static final Logger logger = Logger.getLogger(CertUtils.class.getName());
    private static final String PROPBASE = CertUtils.class.getName();
    private static final int CERT_VERIFY_CACHE_MAX = SyspropUtil.getInteger(PROPBASE + ".certVerifyCacheSize", 500);
    private static CertificateFactory certFactory;

    public static final String ALG_MD5 = "MD5";
    public static final String ALG_SHA1 = "SHA1";
    public static final String FINGERPRINT_HEX = "hex";
    public static final String FINGERPRINT_RAW_HEX = "rawhex";
    public static final String FINGERPRINT_BASE64 = "b64";

    public static final String X509_OID_CRL_DISTRIBUTION_POINTS = "2.5.29.31";
    public static final String X509_OID_NETSCAPE_CRL_URL = "2.16.840.1.113730.1.4";
    public static final String X509_OID_SUBJECTKEYID = "2.5.29.14";
    public static final String X509_OID_AUTHORITYKEYID = "2.5.29.35";
    public static final String X509_OID_AUTHORITY_INFORMATION_ACCESS = "1.3.6.1.5.5.7.1.1";

    private static final Map<String,String> DN_MAP;
    static {
        Map<String,String> map = new HashMap<String,String>();
        map.put("2.5.4.12", "T");
        map.put("1.3.6.1.4.1.42.2.11.2.1", "IP");
        map.put("2.5.4.46", "DNQ");
        map.put("2.5.4.4", "SURNAME");
        map.put("2.5.4.42", "GIVENNAME");
        map.put("2.5.4.43", "INITIALS");
        map.put("2.5.4.44", "GENERATION");
        map.put("1.2.840.113549.1.9.1", "EMAILADDRESS");
        map.put("2.5.4.5", "SERIALNUMBER");
        DN_MAP = Collections.unmodifiableMap(map);
    }

    private static final String REGEX_BASE64 = "\\s*([a-zA-Z0-9+/\\s]+=*)\\s*";
    private static final Pattern PATTERN_BASE64 = Pattern.compile(REGEX_BASE64);
    private static final Pattern PATTERN_CRL = Pattern.compile(PEM_CSR_BEGIN_MARKER + REGEX_BASE64 + PEM_CSR_END_MARKER);
    private static final Pattern PATTERN_NEW_CRL = Pattern.compile(PEM_CSR_BEGIN_NEW_MARKER + REGEX_BASE64 + PEM_CSR_END_NEW_MARKER);

    public static boolean isCertCaCapable(X509Certificate cert) {
        if (cert == null) return false;
        boolean[] usages = cert.getKeyUsage();
        return usages != null && usages[KeyUsage.keyCertSign] && cert.getBasicConstraints() > 0;
    }

    /** Exception thrown if there is more than one CN in a cert DN. */
    public static class MultipleCnValuesException extends Exception {}


    // Map of VerifiedCert => Boolean.TRUE
    private static final Cache certVerifyCache =
            WhirlycacheFactory.createCache("certCache",
                                           CERT_VERIFY_CACHE_MAX,
                                           127, WhirlycacheFactory.POLICY_LRU
            );

    private static class VerifiedCert {
        final byte[] certBytes;
        final byte[] publicKeyBytes;
        final int hashCode;

        public VerifiedCert(X509Certificate cert, PublicKey key) throws CertificateEncodingException {
            this(cert.getEncoded(), key.getEncoded());
        }

        public VerifiedCert(byte[] certBytes, byte[] publicKeyBytes) {
            this.certBytes = certBytes;
            this.publicKeyBytes = publicKeyBytes;
            this.hashCode = makeHashCode();
        }

        /** @noinspection RedundantIfStatement*/
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final VerifiedCert that = (VerifiedCert)o;

            if (!Arrays.equals(certBytes, that.certBytes)) return false;
            if (!Arrays.equals(publicKeyBytes, that.publicKeyBytes)) return false;

            return true;
        }

        private int makeHashCode() {
            int c = 7;
            c += 17 * Arrays.hashCode(certBytes);
            c += 29 * Arrays.hashCode(publicKeyBytes);
            return c;
        }

        public int hashCode() {
            return hashCode;
        }

        /** @return true if this cert has already been verified with this public key. */
        public boolean isVerified() {
            final Object got;
            got = certVerifyCache.retrieve(this);
            return got instanceof Boolean && (Boolean) got;
        }

        /** Report that this cert was successfully verified with its public key. */
        public void onVerified() {
            certVerifyCache.store(this, Boolean.TRUE);
        }
    }

    /** Same behavior as X509Certificate.verify(publicKey), but memoizes the result. */
    public static void cachedVerify(X509Certificate cert, PublicKey publicKey) throws NoSuchProviderException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException {
        VerifiedCert vc = new VerifiedCert(cert, publicKey);
        if (vc.isVerified()) {
            if (logger.isLoggable(Level.FINER)) logger.finer("Verified cert signature (cached): " + cert.getSubjectDN().toString());
            return; // cache hit
        }

        cert.verify(publicKey);
        vc.onVerified();
    }

    /**
     * Test if the specified certificate is verifiable with the specified public key, without throwing
     * any checked exceptions.
     * <p/>
     * This makes use of the CertUtils certificate verification cache.
     *
     * @param cert  the certificate to check.  Required.
     * @param publicKey  the public key to verify with.  Required.
     * @return  true iff. the specified cert verifies successfully with the specified public key
     */
    public static boolean isVerified(X509Certificate cert, PublicKey publicKey) {
        try {
            cachedVerify(cert, publicKey);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Same behavior as X509Certificate.verify(publicKey, sigProvider), but memoizes the result. */
    public static void cachedVerify(X509Certificate cert, PublicKey publicKey, String sigProvider) throws NoSuchProviderException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException {
        VerifiedCert vc = new VerifiedCert(cert, publicKey);
        if (vc.isVerified()) {
            if (logger.isLoggable(Level.FINER)) logger.finer("Verified cert signature (cached): " + cert.getSubjectDN().toString());
            return; // cache hit
        }

        cert.verify(publicKey, sigProvider);
        vc.onVerified();
    }

    /** Decode the specified cert bytes, which may be either PEM or DER but which must be exactly 1 certzzz. */
    public static X509Certificate decodeCert(byte[] bytes) throws CertificateException {
        // Detect PEM early, since the Sun cert parser is piss-poor unreliable at doing so on its own
        if (looksLikePem(bytes)) try {
            return decodeFromPEM(new String(bytes, "ISO-8859-1"));
        } catch (IOException e) {
            throw new CertificateException("Invalid PEM-format certificate", e);
        }

        return (X509Certificate)getFactory().generateCertificate(new ByteArrayInputStream(bytes));
    }

    /**
     * Examines the first couple hundred bytes of the specified byte array for a PEM start marker.
     *
     * @param bytes the bytes to examine for a PEM start marker.  Must not be null.
     * @return true if bytes contains a PEM start marker within the first couple hundred bytes; otherwise false.
     */
    public static boolean looksLikePem(byte[] bytes) {
        try {
            String prefix = new String(bytes, 0, Math.min(200,bytes.length), "ISO-8859-1");
            return prefix.indexOf(PEM_CERT_BEGIN_MARKER) >= 0;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e); // can't happen
        }
    }

    /**
     * Convert a PEM CSR to binary (but without attempting to parse the ASN.1).
     * The surrounding BEGIN and END markers are optional.
     * If PEM decoding cannot be done, this method will return the original bytes unchanged.
     *
     * @param contents a base64 value with optional begin and end markers.  Required.
     * @return the decoded binary for this CSR.
     * @throws IOException if the base64 cannot be decoded.
     */
    public static byte[] csrPemToBinary(byte[] contents) throws IOException {
        String str = new String(contents, "UTF-8").trim();
        String b64 = null;
        Matcher matcher = PATTERN_NEW_CRL.matcher(str);
        if (matcher.find()) {
            b64 = matcher.group(1);
        } else {
            matcher = PATTERN_CRL.matcher(str);
            if (matcher.matches()) {
                b64 = matcher.group(1);
            } else {
                matcher = PATTERN_BASE64.matcher(str);
                if (matcher.matches()) {
                    b64 = matcher.group(1);
                }
            }
        }
        if (b64 == null)
            throw new IOException("CSR does not appear to be PEM encoded");
        return HexUtils.decodeBase64(b64, true);
    }

    /**
     * Safely check if two certificates are equal, working around the fact that .equals() between
     * different X509Certificate implementations is problematic.
     * <p/>
     * This method checks the implementation classes.  If both are the same, it just calls
     * cert1.equals(cert2).  Otherwise, it gets the canonical encoded forms of both certificates
     * and compares them for byte-by-byte equality.
     *
     * @param cert1  a certificate to examine.  Required.
     * @param cert2  a certificate to examine.  If null, this method always returns false.
     * @return true iff. both X509Certificate instances represent the same X.509 certificate.
     */
    public static boolean certsAreEqual(X509Certificate cert1, X509Certificate cert2) {
        if (cert2 == null) return false;
        if (cert1.getClass() == cert2.getClass()) return cert1.equals(cert2);
        try {
            return Arrays.equals(cert1.getEncoded(), cert2.getEncoded() );
        } catch ( CertificateEncodingException e ) {
            return false;
        }
    }

    /**
     * Check the specified cert and key and throw an exception if we can tell that the key does not
     * go with the cert.
     * <p/>
     * A successful return does NOT mean the key is guaranteed to match the cert.
     * This implementation is best-effort, intended only to catch bugs, and <b>MUST NOT</b> be used for any kind of security enforcement.
     * <p/>
     * It will currently only detect problems with RSA certs, and only when the modulus is
     * available for both the key from the cert and the passed-in key.  (The modulus may not be
     * available in some cases where the Key is backed by an object stored in a PKCS#11 hardware module.)
     * <p/>
     * In the future this method may support DSA certs as well, so it is encouraged to call this method
     * even if you know it's not an RSA cert.
     *
     * @param cert a cert to examine.  Required.
     * @param key a key that is believed either to be the RSA public key from the cert, or the corresponding RSA private key.
     * @throws java.security.cert.CertificateException if we can tell that the key in question definitely does not go with the specified cert.
     */
    public static void checkForMismatchingKey(X509Certificate cert, Key key) throws CertificateException {
        PublicKey certPublic = cert.getPublicKey();

        boolean certIsRsa = certPublic instanceof RSAKey || "RSA".equalsIgnoreCase(certPublic.getAlgorithm());
        boolean keyIsRsa = key instanceof RSAKey || "RSA".equalsIgnoreCase(key.getAlgorithm());

        if (certIsRsa != keyIsRsa)
            throw new CertificateException("The specified key does not belong to the specified certificate.");

        if (!(certPublic instanceof RSAKey) || !(key instanceof RSAKey))
            return; // can't compare modulus, so can't help you further

        BigInteger rsaKeyMod = ((RSAKey)key).getModulus();
        BigInteger certKeyMod = ((RSAKey)certPublic).getModulus();

        if (rsaKeyMod == null || certKeyMod == null)
            return; // we can't help you.  (Plus, normally can't happen.)

        if (!certKeyMod.equals(rsaKeyMod))
            throw new CertificateException("The specified key's RSA modulus differs from that of the public key in the certificate.");
    }

    public static X509Certificate[] parsePemChain(String[] pemChain) throws CertificateException {
        if (pemChain == null || pemChain.length < 1)
            throw new IllegalArgumentException("PEM chain must contain at least one certificate");

        X509Certificate[] safeChain = new X509Certificate[pemChain.length];
        try {
            for (int i = 0; i < pemChain.length; i++) {
                String pem = pemChain[i];
                safeChain[i] = CertUtils.decodeFromPEM(pem);
            }
        } catch (IOException e) {
            throw new CertificateException("error setting new cert", e);
        }
        return safeChain;
    }

    /**
     * Produce an X509Certificate array from a Certificate array that contains nothing but X509Certificates.
     *
     * @param genericChain a Certificate[] array that contains only X509Certificate instances
     * @return an X509Certificate[] instance.
     * @throws CertificateException if there are any non-X.509 certificate in the input array
     */
    public static X509Certificate[] asX509CertificateArray(Certificate[] genericChain) throws CertificateException {
        X509Certificate[] ret = new X509Certificate[genericChain.length];
        for (int i = 0; i < genericChain.length; ++i) {
            Certificate cert = genericChain[i];
            if (!(cert instanceof X509Certificate))
                throw new CertificateException("Certificate chain contains a non-X.509 certificate");
            X509Certificate x509Cert = (X509Certificate)cert;
            ret[i] = x509Cert;
        }
        return ret;
    }

    public static X509Certificate[] decodeCertChain(byte[] bytes) throws CertificateException {
        Collection<? extends Certificate> list = getFactory().generateCertificates(new ByteArrayInputStream(bytes));
        List<X509Certificate> certs = new ArrayList<X509Certificate>(list.size());
        for (Certificate certificate : list) {
            if (certificate instanceof X509Certificate)
                certs.add((X509Certificate)certificate);
            else
                throw new IllegalArgumentException("Certificate in chain was not X.509");
        }
        return certs.toArray(new X509Certificate[certs.size()]);
    }

    public synchronized static CertificateFactory getFactory() {
        try {
            if (certFactory == null)
                certFactory = CertificateFactory.getInstance(FACTORY_ALGORITHM);
            return certFactory;
        } catch ( CertificateException e ) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the PEM (aka base64) encoded X.509 certificate.
     * @param cert the certificate to encode
     * @return  the PEM encoded certificate as a byte array
     * @throws java.security.cert.CertificateEncodingException if there is a problem encoding the cert
     * @throws java.io.IOException if there is a problem encoding the encoding of the cert
     */
    public static String encodeAsPEM(X509Certificate cert) throws IOException, CertificateEncodingException {
        return encodeAsPEM(cert.getEncoded());
    }

    /**
     * Get the PEM (aka base64) encoded X.509 certificate from the byte array
     * containing the X.509 certificate encoded as ASN.1 DER.
     * @param cert the byte array with the certificate encoded as ASN.1 and DER
     * @return  the PEM encoded certificate as a byte array
     * @throws java.io.IOException if there is a problem encoding the cert
     */
    public static String encodeAsPEM(byte[] cert) throws IOException {
        BufferPoolByteArrayOutputStream bos = new BufferPoolByteArrayOutputStream();
        try {
            String encoding = "UTF-8";
            bos.write(PEM_CERT_BEGIN_MARKER.getBytes(encoding));
            bos.write("\n".getBytes(encoding));
            bos.write( HexUtils.encodeBase64(cert).getBytes(encoding));
            bos.write("\n".getBytes(encoding));
            bos.write(PEM_CERT_END_MARKER.getBytes(encoding));
            bos.write("\n".getBytes(encoding));
            return bos.toString(encoding);
        } finally {
            bos.close();
        }
    }

    /**
     * Get the X509Certifcate that is (Base64) encoded in the given text.  PEM header/footers, if present, are ignored.
     *
     * @param certificateText The base64 encoded certficate data, possibly with PEM header/footer
     * @return the X509Certificate certificate
     * @throws IOException if the text is not a PEM/Base64 data
     * @throws CertificateException if the certificate decoding fails
     */
    public static X509Certificate decodeFromPEM(String certificateText) throws IOException, CertificateException {
        int startIndex = certificateText.indexOf(PEM_CERT_BEGIN_MARKER);
        int endIndex = certificateText.indexOf(PEM_CERT_END_MARKER);

        final String base64Certificate;
        if (startIndex < 0) {
            if (endIndex >= 0) throw new IOException("Begin PEM marker present, but end marker missing");
            base64Certificate = certificateText;
        } else {
            base64Certificate = certificateText.substring(
                    startIndex+PEM_CERT_BEGIN_MARKER.length(),
                    endIndex);
        }

        byte[] certificateBytes = HexUtils.decodeBase64(base64Certificate, true);

        return decodeCert(certificateBytes);
    }

    /**
     * Decode a private key from a PEM encoded file
     *
     * @param keyText the PEM encoded key data
     * @return the private key
     */
    public static PrivateKey decodeKeyFromPEM(String keyText) throws IOException {
        int startIndex = keyText.indexOf(PEM_RSAKEY_BEGIN_MARKER);
        int endIndex = keyText.indexOf(PEM_RSAKEY_END_MARKER);
        int markerLength = PEM_RSAKEY_BEGIN_MARKER.length();
        boolean rsa = true;

        if (startIndex == -1 && endIndex == -1 ) {
            startIndex = keyText.indexOf(PEM_DSAKEY_BEGIN_MARKER);
            endIndex = keyText.indexOf(PEM_DSAKEY_END_MARKER);
            markerLength = PEM_DSAKEY_BEGIN_MARKER.length();
            rsa = false;
        }

        if (startIndex <0 || endIndex <= startIndex) {
            throw new CausedIOException("Key data not found (missing begin or end marker)");
        }

        String keyHeadersAndData = keyText.substring(
                startIndex+markerLength,
                endIndex);

        String encryptionHeader = "DEK-Info"; // encryption not supported
        int saltTextIndex = keyHeadersAndData.indexOf(encryptionHeader);
        if (saltTextIndex >= 0) {
            throw new CausedIOException("Key data not valid (encryption not supported)");
        }

        byte[] keyData = HexUtils.decodeBase64(keyHeadersAndData, true);
        final PrivateKey privateKey;
        try {
            if (rsa) {
                KeyFactory kf = KeyFactory.getInstance("RSA");
                BigInteger[] keyModulusAndExponent = decodePKCS1RSA(keyData);
                privateKey = kf.generatePrivate(new RSAPrivateKeySpec(keyModulusAndExponent[0], keyModulusAndExponent[1]));
            } else {
                KeyFactory kf = KeyFactory.getInstance("DSA");
                privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(keyData));
            }
        }
        catch(NoSuchAlgorithmException nsae) {
            throw new CausedIOException("Key data not valid (unsupported key type)");
        }
        catch(InvalidKeySpecException ikse) {
            throw new CausedIOException("Key data not valid (invalid key spec)");
        }

        return privateKey;
    }

    private static final String[] KEY_USAGES = {
        "Digital Signature",
        "Non-Repudiation",
        "Key Encipherment",
        "Data Encipherment",
        "Key Agreement",
        "Certificate Signing",
        "CRL Signing",
        "Encipher Only",
        "Decipher Only",
    };

    public static String getThumbprintSHA1(X509Certificate cert) throws CertificateEncodingException
    {
        try {
            return getCertificateFingerprint(cert, ALG_SHA1, FINGERPRINT_BASE64);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Misconfigured VM: SHA-1 not available: " + e.getMessage(), e); // can't happen
        }
    }

    public static String getSki(X509Certificate cert) {
        byte[] skiBytes = getSKIBytesFromCert(cert);
        if (skiBytes == null) return null;
        return HexUtils.encodeBase64(skiBytes);
    }

    public static final class KeyUsage {
        public static final int digitalSignature = 0;
        public static final int nonRepudiation = 1;
        public static final int keyEncipherment = 2;
        public static final int dataEncipherment = 3;
        public static final int keyAgreement = 4;
        public static final int keyCertSign = 5;
        public static final int cRLSign = 6;
        public static final int encipherOnly = 7;
        public static final int decipherOnly = 8;
    }

    /**
     * Checks the validity period of the specified certificate.
     *
     * @param certificate the certificate to check. Required.
     * @return a {@link CertificateExpiry} indicating how many days remain before the certificate will expire
     * @throws CertificateNotYetValidException if the certificate's "not-before" is after the current time
     * @throws CertificateExpiredException if the certificate's "not-after" was before the current time
     */
    public static CertificateExpiry checkValidity( X509Certificate certificate )
            throws CertificateNotYetValidException, CertificateExpiredException
    {
        certificate.checkValidity();
        return getCertificateExpiry(certificate);
    }

    /**
     * Compute the number of days until the specified cert expires and return a CertificateExpiry instance
     * that can be used to evaluate the severity of the cert's expiry condition.
     *
     * @param certificate the certificate to evaluate.  Required.
     * @return a {@link CertificateExpiry} indicating how many days remain before the certificate will expire
     */
    public static CertificateExpiry getCertificateExpiry(X509Certificate certificate) {
        final long now = System.currentTimeMillis();
        final long expires = certificate.getNotAfter().getTime();
        // fla, bugfix 1791 (what kind of math is this?!)
        // int days = (int)(.5f + ((expires - now) * 1000 * 86400));
        int days = (int)((expires - now) / (1000*86400));
        return new CertificateExpiry(days);
    }

    /**
     * Check if the specified certificate is within its validity period.
     * This method returns true iff. the current system time is within the range
     * defined by the specified certificate's NotBefore and NotAfter fields.
     *
     * @param cert the certificate to check
     * @return true iff. this certificate has become valid and has not yet expired.
     */
    public static boolean isValid(X509Certificate cert) {
        try {
            cert.checkValidity();
            return true;
        } catch (CertificateExpiredException e) {
            return false;
        } catch (CertificateNotYetValidException e) {
            return false;
        }
    }

    /**
     * Check that the DN contains only known attribute names.
     *
     * @param dn The DN to check
     * @return True if valid
     */
    public static boolean isValidDN( final String dn ) {
        boolean valid = false;

        try {
            X500Principal princ = new X500Principal(dn);
            String formattedDn = princ.getName(X500Principal.RFC2253);

            Set<String> rawDNSet = dnToAttributeMap(dn).keySet();
            Set<String> formattedDNSet = new HashSet<String>(dnToAttributeMap(formattedDn).keySet());
            for ( Map.Entry<String,String> entry : DN_MAP.entrySet() ) {
                if ( formattedDNSet.contains(entry.getKey()) ) {
                    formattedDNSet.remove(entry.getKey());
                    formattedDNSet.add(entry.getValue());
                }                
            }

            valid = rawDNSet.equals(formattedDNSet);
        } catch (IllegalArgumentException iae) {
            // invalid
        }

        return valid;
    }

    /**
     * Get the validation message if the DN does not contain only known attribute names.
     *
     * @param dn The DN to check
     * @return The message or null if valid
     */
    public static String getDNValidationMessage( final String dn ) {
        String message = null;

        try {
            X500Principal princ = new X500Principal(dn);
            String formattedDn = princ.getName(X500Principal.RFC2253);

            Set<String> rawDNSet = dnToAttributeMap(dn).keySet();
            Set<String> formattedDNSet = new HashSet<String>(dnToAttributeMap(formattedDn).keySet());
            for ( Map.Entry<String,String> entry : DN_MAP.entrySet() ) {
                if ( formattedDNSet.contains(entry.getKey()) ) {
                    formattedDNSet.remove(entry.getKey());
                    formattedDNSet.add(entry.getValue());
                }
            }

            rawDNSet.removeAll(formattedDNSet);
            if ( !rawDNSet.isEmpty() ) {
                message = "Unrecognized DN components " + rawDNSet; 
            }
        } catch (IllegalArgumentException iae) {
            message = "Illegal DN: '" + ExceptionUtils.getMessage(iae) + "'";
        }

        return message;
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
    public static Map<String, List<String>> dnToAttributeMap(String dn) {
        final LdapName name;
        try {
            name = new LdapName(dn);
        } catch (InvalidNameException e) {
            throw new IllegalArgumentException("Invalid DN", e);
        }

        Map<String, List<String>> map = new HashMap<String, List<String>>();
        List<Rdn> rdns = name.getRdns();
        for (Rdn rdn : rdns) {
            String type = rdn.getType().toUpperCase();

            List<String> values = map.get(type);
            if (values == null) {
                values = new ArrayList<String>();
                map.put(type, values);
            }

            values.add(rdn.getValue().toString());
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
     * @param dn the dn to be matched.  If this is invalid, this method will return false.
     * @param pattern the pattern to match against.  Must be a valid DN.
     * @return true if the dn matches the pattern, false otherwise.
     * @throws IllegalArgumentException if the pattern is not a valid DN.
     */
    public static boolean dnMatchesPattern(String dn, String pattern) {
        Map<String, List<String>> dnMap = dnToAttributeMap(dn);
        Map<String, List<String>> patternMap = dnToAttributeMap(pattern);

        boolean matches = true;
        for (String oid : patternMap.keySet()) {
            List<String> patternValues = patternMap.get(oid);
            List<String> dnValues = dnMap.get(oid);

            if (dnValues == null) {
                matches = false;
                break;
            }

            for (String patternValue : patternValues) {
                if (!dnValues.contains(patternValue)) {
                    if (!("*".equals(patternValue))) {
                        matches = false;
                        break;
                    }
                }
            }
        }

        return matches;
    }


    /**
     * Display structured information about a certificate.
     *
     * @param cert The certificate to analyze
     * @return a single multi-line string that can be printed out
     * @throws CertificateEncodingException if the cert could not be decoded
     */
    public static String toString(X509Certificate cert) throws CertificateEncodingException {
        StringBuilder sb = new StringBuilder();
        List<Pair<String, String>> p = getCertProperties(cert);
        for (Pair<String, String> prop : p) {
            String label = prop.left;
            String value = prop.right;
            sb.append(label).append(": ").append(value).append("\n");
        }
        return sb.toString();
    }

    /**
     * Obtain structured information about a certificate in an easy-to-display tabular format.
     *
     * @param cert The certificate to analyze
     * @return a List of Pairs of {"Label", "Value"}
     * @throws CertificateEncodingException if the cert could not be decoded
     */
    public static List<Pair<String, String>> getCertProperties(X509Certificate cert)
      throws CertificateEncodingException {
        List<Pair<String, String>> l = new ArrayList<Pair<String, String>>();
        if (cert == null) return l;

        l.add(new Pair<String, String>("Creation date", nullNa(cert.getNotBefore())));
        l.add(new Pair<String, String>("Expiry date", nullNa(cert.getNotAfter())));
        l.add(new Pair<String, String>("Issued to", nullNa(cert.getSubjectDN())));
        l.add(new Pair<String, String>("Serial number", nullNa(cert.getSerialNumber())));
        l.add(new Pair<String, String>("Issuer", nullNa(cert.getIssuerDN())));

        try {
            l.add(new Pair<String, String>("SHA-1 fingerprint", getCertificateFingerprint(cert, "SHA1")));
            l.add(new Pair<String, String>("MD5 fingerprint", getCertificateFingerprint(cert, "MD5")));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // misconfigured VM
        }

        l.add(new Pair<String, String>("Basic constraints", basicConstraintsToString(cert.getBasicConstraints())));
        l.add(new Pair<String, String>("Key usage", keyUsageToString(cert.getKeyUsage())));

        PublicKey publicKey = cert.getPublicKey();
        if (publicKey != null) {
            l.add(new Pair<String, String>("Key type", nullNa(publicKey.getAlgorithm())));

            if (publicKey instanceof RSAPublicKey) {
                RSAPublicKey rsaKey = (RSAPublicKey) publicKey;
                String modulus = rsaKey.getModulus().toString(16);
                l.add(new Pair<String, String>("RSA strength", (modulus.length() * 4) + " bits"));
                //l.add(new Pair<String, String>("RSA modulus", modulus});
                l.add(new Pair<String, String>("RSA public exponent", rsaKey.getPublicExponent().toString(16)));
            } else if (publicKey instanceof DSAPublicKey) {
                DSAPublicKey dsaKey = (DSAPublicKey) publicKey;
                DSAParams params = dsaKey.getParams();
                l.add(new Pair<String, String>("DSA prime (P)", params.getP().toString(16)));
                l.add(new Pair<String, String>("DSA subprime (P)", params.getQ().toString(16)));
                l.add(new Pair<String, String>("DSA base (P)", params.getG().toString(16)));
            }
        }

        return l;
    }

    private static String basicConstraintsToString(int basicConstraints) {
        if (basicConstraints == -1) return "<Not present>";
        if (basicConstraints == Integer.MAX_VALUE) return "CA capable; unlimited path length";
        return "CA capable; maximum path length=" + basicConstraints;        
    }

    /**
     * @return A string such as "KeyEncipherment, caCert" that describes enabled key usages for a cert, or
     * "<None premitted>" if no usage bits are enabled, or
     * "<Not present>" if there is no key usage extension.
     */
    private static String keyUsageToString(boolean[] ku) {
        if (ku == null) return "<Not present>";
        StringBuilder sb = new StringBuilder();
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

    public static String getCertificateFingerprint(X509Certificate cert, String algorithm)
            throws CertificateEncodingException, NoSuchAlgorithmException
    {
        return getCertificateFingerprint(cert, algorithm, FINGERPRINT_HEX);
    }

    /**
     * The method creates the fingerprint and returns it in a
     * String to the caller.
     *
     * @param cert      the certificate
     * @param algorithm the alghorithm (MD5 or SHA1)
     * @param format    the format to return, either hex ("SHA1:00:22:ff:et:ce:te:ra"), b64 ("abndwlaksj=="),
     *                  or rawhex ("0022ffetcetera").
     * @return the certificate fingerprint as a String
     * @exception CertificateEncodingException
     *                      thrown whenever an error occurs while attempting to
     *                      encode a certificate.
     */
    public static String getCertificateFingerprint(X509Certificate cert, String algorithm, String format)
            throws CertificateEncodingException, NoSuchAlgorithmException
    {
        if (cert == null) {
            throw new NullPointerException("cert");
        }
        StringBuilder buff = new StringBuilder();
        byte[] fingers = cert.getEncoded();

        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] digest = md.digest(fingers);

        boolean raw = false;
        if (FINGERPRINT_BASE64.equals(format))
            return HexUtils.encodeBase64(digest, true);
        else if (FINGERPRINT_RAW_HEX.equals(format))
            raw = true;
        else if (!(FINGERPRINT_HEX.equals(format)))
            throw new IllegalArgumentException("Unknown cert fingerprint format: " + format);

        // the algorithm
        if (!raw)
            buff.append(algorithm).append(":");

        for (int i = 0; i < digest.length; i++) {
            if (!raw && i != 0) buff.append(":");
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
            throws CertificateException, CertificateUntrustedException
    {

        Principal trustedDN = trustedCert.getSubjectDN();

        for (int i = 0; i < maxDepth; i++) {
            X509Certificate cert = chain[i];
            cert.checkValidity(); // will abort if this throws
            if (i + 1 < chain.length) {
                try {
                    cachedVerify(cert, chain[i + 1].getPublicKey());
                } catch (Exception e) {
                    // This is a serious problem with the cert chain presented by the peer.  Do a full abort.
                    throw new CertificateException("Unable to verify signature in peer certificate chain: " + e);
                }
            }

            if (cert.getIssuerDN().toString().equals(trustedDN.toString())) {
                try {
                    cachedVerify(cert, trustedCert.getPublicKey());
                    return; // success
                } catch (Exception e) {
                    // Server SSL cert might have changed.  Attempt to reimport it
                    throw new CertificateUntrustedException("Unable to verify peer certificate with trusted cert: " + e);
                }
            } else if (cert.getSubjectDN().equals(trustedDN)) {
                if (certsAreEqual(cert, trustedCert)) {
                    return; // success
                }
            }
        }

        // We probably just havne't talked to this Ssg before.  Trigger a reimport of the certificate.
        throw new CertificateUntrustedException("Couldn't find trusted certificate in peer's certificate chain");
    }

    /**
     * Extract the subject common name from the specified certificate.
     *
     * @param cert the certificate to examine
     * @return the username from the certificate.  Might be empty string, but won't be null.
     * @throws IllegalArgumentException if the certificate does not contain a subject DN.
     * @throws com.l7tech.common.io.CertUtils.MultipleCnValuesException if the subject DN contains more than one CN attribute.
     *         This is apparently permitted by X.509.
     */
    public static String extractSingleCommonNameFromCertificate(X509Certificate cert) throws IllegalArgumentException, MultipleCnValuesException {
        Principal principal = cert.getSubjectDN();
        if (principal == null)
            throw new IllegalArgumentException("Cert contains no subject DN");
        String ret = extractSingleCommonName(principal);
        return ret == null ? "" : ret;
    }

    /**
     * Extract a single subject common name attribute value from a certificate.
     * If multiple CN values are present, this will find and return only one of them.
     *
     * @param cert the certificate to examine.  Required.
     * @return One of the CN attribute values, or null if there aren't any.
     */
    public static String extractFirstCommonNameFromCertificate(X509Certificate cert) {
        Principal principal = cert.getSubjectDN();
        if (principal == null)
            throw new IllegalArgumentException("Cert contains no subject DN");
        Map<String, List<String>> dnMap = dnToAttributeMap(principal.getName());
        List<String> cnValues = dnMap.get("CN");
        String login = null;
        if (cnValues != null && cnValues.size() >= 1) {
            login = cnValues.get(0);
        }
        return login;
    }

    /**
     * Extract all subject common names from the specified certificate.
     *
     * @param cert the certificate to examine.  Required.
     * @return a List of the CN attribute values for this certificate's subject.  May be empty but never null.
     * @throws IllegalArgumentException if the certificate does not contain a subject DN.
     */
    public static List<String> extractCommonNamesFromCertificate(X509Certificate cert) throws IllegalArgumentException {
        Principal principal = cert.getSubjectDN();
        if (principal == null)
            throw new IllegalArgumentException("Cert contains no subject DN");
        List<String> vals = dnToAttributeMap(principal.getName()).get("CN");
        return vals == null ? Collections.<String>emptyList() : vals;
    }

    /**
     * Extract a single issuer common name from the specified certificate, failing if there is more than one CN.
     *
     * @param cert the certificate to examine
     * @return the issuer common name from the certificate.  Might be empty string, but won't be null.
     * @throws IllegalArgumentException if the certificate does not contain an issuer DN.
     * @throws com.l7tech.common.io.CertUtils.MultipleCnValuesException if the issuer DN contains more than one CN attribute.
     *         This is apparently permitted by X.509.
     */
    public static String extractSingleIssuerNameFromCertificate(X509Certificate cert) throws IllegalArgumentException, MultipleCnValuesException {
        Principal principal = cert.getIssuerDN();
        if (principal == null)
            throw new IllegalArgumentException("Cert contains no issuer DN");
        String ret = extractSingleCommonName(principal);
        if (ret == null) ret = principal.getName();
        return ret == null ? "" : ret;
    }

    /**
     * Extract an issuer CN value from the specified certificate.
     * If the issuer DN has multiple CN values, this will find and return one of them.
     *
     * @param cert  the certificate to examine.  Required.
     * @return an issuer CN value, or null if there weren't any.
     * @throws IllegalArgumentException if the certificate does not contain an issuer DN.
     */
    public static String extractFirstIssuerNameFromCertificate(X509Certificate cert) {
        Principal principal = cert.getIssuerDN();
        if (principal == null)
            throw new IllegalArgumentException("Cert contains no issuer DN");
        Map<String, List<String>> dnMap = dnToAttributeMap(principal.getName());
        List<String> cnValues = dnMap.get("CN");
        String login = null;
        if (cnValues != null && cnValues.size() >= 1) {
            login = cnValues.get(0);
        }
        return login;
    }

    /**
     * Extract the issuer common names from the specified certificate.
     *
     * @param cert the certificate to examine
     * @return a List of the CN attribute values for this certificate's Issuer DN.  May be null or empty.
     */
    public static List<String> extractIssuerNamesFromCertificate(X509Certificate cert) {
        Principal principal = cert.getIssuerDN();
        if (principal == null)
            throw new IllegalArgumentException("Cert contains no issuer DN");
        List<String> vals = dnToAttributeMap(principal.getName()).get("CN");
        return vals == null ? Collections.<String>emptyList() : vals;
    }

    /**
     * Extract the value of the CN attribute from the DN in the Principal, which is
     * expected to contain exactly one CN attribute.
     *
     * @param principal an X.500 Principal, whose getName() method will return a String in X.500 name format.  Required.
     * @return String  The value of CN attribute in the DN.  Might be null.
     * @throws MultipleCnValuesException if the DN contains multiple CN values.
     */
    private static String extractSingleCommonName(Principal principal) throws MultipleCnValuesException {
        String dn = principal.getName();
        Map dnParts = dnToAttributeMap(dn);
        List cns = (List)dnParts.get("CN");
        if (cns == null) return null;
        switch(cns.size()) {
            case 0:
                return null;
            case 1:
                return (String)cns.get(0);
            default:
                throw new MultipleCnValuesException();
        }
    }

    /**
     * Extract the key exchange algorithm portion of an SSL cipher suite name.
     *
     * @param cipherSuiteName The name such as "TLS_RSA_EXPORT_WITH_RC4_40_MD5"
     * @return The key exchange algorithm such as "RSA_EXPORT"
     * @throws IllegalArgumentException if the cipher suite name is invalid
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted
     */
    public static String extractAuthType(final String cipherSuiteName)  {
        Pattern pattern = Pattern.compile("(?:SSL|TLS)_(.*?)_WITH_.*");
        Matcher matcher = pattern.matcher(cipherSuiteName);
        if ( matcher.matches() ) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Invalid cipher suite name '"+cipherSuiteName+"'.");
    }

    /**
     * Copied from Apache WSS4J's org.apache.ws.security.components.crypto.AbstractCrypto
     */
     public static byte[] getSKIBytesFromCert(X509Certificate cert) {
        /*
           * Gets the DER-encoded OCTET string for the extension value (extnValue)
           * identified by the passed-in oid String. The oid string is represented
           * by a set of positive whole numbers separated by periods.
           */
        byte[] derEncodedValue = cert.getExtensionValue(X509_OID_SUBJECTKEYID);

        if (cert.getVersion() < 3 || derEncodedValue == null) {
            PublicKey key = cert.getPublicKey();
            if (!(key instanceof RSAPublicKey)) {
                logger.warning("Can't get SKI for non-RSA public key in cert '" + cert.getSubjectDN().getName() + "'");
                return null;
            }
            byte[] encoded = key.getEncoded();
            // remove 22-byte algorithm ID and header
            byte[] value = new byte[encoded.length - 22];
            System.arraycopy(encoded, 22, value, 0, value.length);
            MessageDigest sha;
            try {
                sha = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex); // Can't happen or other stuff will be badly broken too
            }
            sha.reset();
            sha.update(value);
            return sha.digest();
        }

        return stripAsnPrefix(derEncodedValue, 4);
    }

    /**
     * Get the Authority Key Identifier value for the specified X.509 certificate.
     * @param cert the cert to examine.  Must not be null.  Must be at least X.509 version 3.
     * @return the DER-encoded AKI bytes, or null if this cert does not have an AKI extension.
     */
    public static byte[] getAKIBytesFromCert(X509Certificate cert) {
        if (cert.getVersion() < 3) return null;
        byte[] ext = cert.getExtensionValue(X509_OID_AUTHORITYKEYID);
        if (ext == null) return null;
        return stripAsnPrefix(ext, 6);
    }

    private static byte[] stripAsnPrefix(byte[] derEncodedValue, int bytesToStrip) {
        if (derEncodedValue.length <= bytesToStrip) throw new IllegalArgumentException();

        /**
         * Strip away first four bytes from the DerValue (tag and length of
         * ExtensionValue OCTET STRING and KeyIdentifier OCTET STRING)
         */
        byte abyte0[] = new byte[derEncodedValue.length - bytesToStrip];

        System.arraycopy(derEncodedValue, bytesToStrip, abyte0, 0, abyte0.length);
        return abyte0;
    }

    /**
     * Decode the ASN.1 RSA data.
     *
     * <pre>
     * RSAPrivateKey ::= SEQUENCE {
     *   version Version,
     *   modulus INTEGER, -- n
     *   publicExponent INTEGER, -- e
     *   privateExponent INTEGER, -- d
     *   prime1 INTEGER, -- p
     *   prime2 INTEGER, -- q
     *   exponent1 INTEGER, -- d mod (p-1)
     *   exponent2 INTEGER, -- d mod (q-1)
     *   coefficient INTEGER -- (inverse of q) mod p }
     *
     * Version ::= INTEGER
     * </pre>
     *
     * @return A BigInteger array containing the modulus and exponent.
     */
    private static BigInteger[] decodePKCS1RSA(byte[] data) throws IOException {
        ASN1Sequence keySequence = new ASN1Sequence(new ASN1Type[]{
                new ASN1Integer(),
                new ASN1Integer(),
                new ASN1Integer(),
                new ASN1Integer(),
                new ASN1Integer(),
                new ASN1Integer(),
                new ASN1Integer(),
                new ASN1Integer(),
                new ASN1Integer()
        });
        Object decoded = keySequence.decode(data);

        if (!(decoded instanceof Object[])) {
            throw new IOException("Incorrect type when decoding RSAPrivateKey.");
        }

        Object[] sequence = (Object[]) decoded;
        if (!(sequence[1] instanceof byte[]) ||
            !(sequence[3] instanceof byte[])) {
            throw new IOException("Incorrect modulus or exponent type when decoding RSAPrivateKey.");
        }

        BigInteger modulus = new BigInteger((byte[])sequence[1]);
        BigInteger exponent = new BigInteger((byte[])sequence[3]);

        return new BigInteger[]{modulus, exponent};
    }

    /**
     * Caller passes an instance of this to importClientCertificate if they wish to present the user with a list of aliases in a file.
     */
    public static interface AliasPicker {
        /**
         * @param options the available aliases.  Never null or empty.
         * @return the preferred alias.  May not be null.
         * @throws AliasNotFoundException if none of the available aliases look good.
         */
        String selectAlias(String[] options) throws AliasNotFoundException;
    }

    /**
     * An AliasPicker that always picks a presupplied alias.
     */
    public static class SingleAliasPicker implements AliasPicker {
        private final String alias;

        /**
         * @param alias The alias to match.  Required.
         *              The match will not be case-sensitive.
         */
        public SingleAliasPicker(String alias) {
            if (alias == null) throw new NullPointerException();
            this.alias = alias;
        }

        public String selectAlias(String[] options) throws AliasNotFoundException {
            for (String option : options) {
                if (alias.equalsIgnoreCase(option))
                    return option;
            }
            throw new AliasNotFoundException("No private key entry with alias " + alias + " was found.");
        }
    }

    /**
     * An InputStream factory that creates InputStreams by opening the specified File.
     */
    public static class FileInputStreamFactory implements Callable<InputStream> {
        private final File file;

        /**
         * Create a factory that will produce new InputStreams by opening the specified file.
         *
         * @param file The file to open.  Required.  If file doesn't exist or can't be read
         *        when call() is invoked, the invocation will fail.
         */
        public FileInputStreamFactory(File file) {
            this.file = file;
        }

        /**
         * Produce a new FileInputStream by reading our associated file.  Caller takes ownership of
         * the returned stream and is responsible for closing it.
         *
         * @return a new InputStream that reads from the current file.  Never null.  Caller must close it.
         * @throws SecurityException if the security manager exists and denies access to this file
         * @throws FileNotFoundException if the file does not exist or otherwise cannot be opened for reading
         * @throws Exception declared but not thrown by this method.  May be thrown by a subclass.
         */
        public InputStream call() throws Exception {
            return new FileInputStream(file);
        }
    }

    /**
     * Loads a private key entry from the specified keystore, using the specified information.
     * <p/>
     * Caller is responsible for any validity-checking of the imported certificate, if needed.
     *
     * @param inputStreamFactory a factory which, when invoked, produces a new InputStream instance.  Required.
     *                           See {@link FileInputStreamFactory} if all you have is a File.
     * @param kstype the type of keystore to create.  Required.
     * @param ksPass the passphrase to use when opening the file.  Required.
     * @param aliasPicker Selects the alias to import.  Required.
     *                    See {@link SingleAliasPicker} if all you have is a single String.
     * @param aliasPass  the passphrase to use when getting this particular key entry.  For many keystore types,
     *                   this should be the same as the ksPass.  Required.
     * @return the PrivateKeyEntry loaded from the target keystore.  Never null.
     * @throws java.io.IOException if there is a problem reading from the InputStream
     * @throws AliasNotFoundException if the expected alias is not found in the keystore
     * @throws java.security.KeyStoreException if there is a problem reading the keystore
     * @throws java.security.NoSuchAlgorithmException if a cryptographic algorithm needed to read this keystore
     *                                                can't be found
     * @throws java.security.UnrecoverableKeyException if the specified key cannot be recovered, possibly due to
     *                                                 an incorrect passphrase
     * @throws java.security.cert.CertificateException if any of the certificates in the keystore cannot be loaded.
     */
    public static KeyStore.PrivateKeyEntry loadPrivateKey(Callable<InputStream> inputStreamFactory, String kstype, char[] ksPass, AliasPicker aliasPicker, char[] aliasPass)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, AliasNotFoundException, UnrecoverableKeyException {
        KeyStore ks = KeyStore.getInstance(kstype);
        InputStream in = null;
        try {
            in = inputStreamFactory.call();
            ks.load(in, ksPass);
        } catch (IOException e) {
            throw e;
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (CertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException(e); // can't happen
        } finally {
            ResourceUtils.closeQuietly(in);
        }

        List<String> aliases = new ArrayList<String>();
        Enumeration aliasEnum = ks.aliases();
        while (aliasEnum.hasMoreElements()) {
            String alias = (String)aliasEnum.nextElement();
            if (ks.isKeyEntry(alias))
                aliases.add(alias);
        }
        final String alias;
        if (aliases.size() > 1 && aliasPicker != null) {
            alias = aliasPicker.selectAlias(aliases.toArray(new String[aliases.size()]));
            if (alias == null)
                throw new AliasNotFoundException("The AliasPicker did not return an alias.");
        } else if (aliases.size() > 0) {
            alias = aliases.get(0);
        } else
            throw new AliasNotFoundException("The specified file does not contain any certificates.");
        Certificate[] chainToImport = ks.getCertificateChain(alias);
        if (chainToImport == null || chainToImport.length < 1)
            throw new AliasNotFoundException("The specified file does not contain a certificate chain for alias " + alias);
        if (!(chainToImport[0] instanceof X509Certificate))
            throw new AliasNotFoundException("The certificate chain for alias " + alias + " is not X.509");
        Key key = ks.getKey(alias, aliasPass);
        if (key == null || !(key instanceof PrivateKey))
            throw new AliasNotFoundException("The specified alias does not contain a private key.");

         return new KeyStore.PrivateKeyEntry((PrivateKey)key, chainToImport);
    }

    private static final String FACTORY_ALGORITHM = "X.509";
}
