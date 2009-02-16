package com.l7tech.security.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.SyspropUtil;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAKey;
import java.security.interfaces.RSAKey;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the current key usage checking policy and provides a static method to check key usage
 * for a certificate.
 */
public class KeyUsageChecker {
    private static final Logger logger = Logger.getLogger(KeyUsageChecker.class.getName());

    /** Default key usage policy to use if no policyFile or policyXml specified. */
    static final String DEFAULT_POLICY_RESOURCE = "com/l7tech/security/cert/DefaultKeyUsagePolicy.xml";

    /** System property to set to "IGNORE" to disable key usage enforcement. */
    protected static final String PROPERTY_ENFORCEMENT_MODE = "com.l7tech.pkix.keyUsage";

    /** Path to local file containing key usage policy to use if no policyXml specified. */
    protected static final String PROPERTY_POLICY_FILE = "com.l7tech.pkix.keyUsagePolicyFile";

    /** Literal XML of policy to use. */
    protected static final String PROPERTY_POLICY_XML = "com.l7tech.pkix.keyUsagePolicy";

    private static final String X509_KEY_USAGE_EXTENSION_OID = "2.5.29.15";
    private static final String X509_EXTENDED_KEY_USAGE_EXTENSION_OID = "2.5.29.37";

    private static AtomicReference<KeyUsageChecker> defaultInstance = new AtomicReference<KeyUsageChecker>(makeDefaultKeyUsageChecker());

    private final KeyUsagePolicy policy;
    private final boolean enforcePolicy;

    /**
     * Create a KeyUsageChecker.
     *
     * @param policy  the policy to enforce.  If null, all activities involving any cert with a critical KeyUsage or ExtKeyUsage will be disallowed.
     * @param enforcementMode Enforcement mode.  If this is "IGNORE", all activities will always be permitted, regardless of the value of policy.
     */
    public KeyUsageChecker(KeyUsagePolicy policy, String enforcementMode) {
        this.policy = policy;
        this.enforcePolicy = enforcementMode == null || !("IGNORE".equalsIgnoreCase(enforcementMode.trim()));
    }

    public static KeyUsageChecker getDefault() {
        return defaultInstance.get();
    }

    public static void setDefault(KeyUsageChecker checker) {
        if (checker == null) throw new NullPointerException();
        defaultInstance.set(checker);
    }

    /**
     * Perform a key usage check that requires the specified activity with the specified cert, if the key is
     * a PublicKey of a type which requires such a check.
     * <p/>
     * This method will fail if the key type is neither RSA, DSA, nor SecretKey; if an RSA cert
     * is provided and it doesn't seem to match the RSA modulus of the key; if key usage enforcement is enabled
     * and an RSA or DSA public key is provided without a cert; or if key usage enforcement is enabled and an RSA or DSA
     * public key is provided with a cert whose key usage is inconsistent with the specified activity.
     * <p/>
     * X.509 only requires key usage to be enforced on use of the public key from the certificate.  Use of the private key
     * is not restricted.  (Of course, it would be pointless to sign something with a private key whose
     * corresponding public key is not certified for verification, but that isn't this method's concern.)
     *
     * @param activity an activity for key usage enforcement purposes, ie {@link KeyUsageActivity#verifyXml}.  Required.
     * @param cert the certificate corresponding to the key, if the key came from the cert and the cert is known;
     *             or null if the cert is unknown or does not apply to this key type.  (Passing a null cert
     *             will cause all RSA and DSA public keys to fail this method unless key usage enforcement is globally disabled.)
     * @param key the key with which the activity is to be performed.  Required.
     * @throws java.security.cert.CertificateException if a cert is needed but not present; or,
     *                              if key usage enforcement fails for the given cert and activity; or,
     *                              if an RSA cert is provided with a modulus that differs from the provided RSA key; or,
     *                              if the provided key type is not RSA, DSA, or SecretKey.
     */
    public static void requireActivityForKey(KeyUsageActivity activity, X509Certificate cert, Key key) throws CertificateException {
        if (key instanceof RSAKey || "RSA".equalsIgnoreCase(key.getAlgorithm())) {
            if (cert != null)
                CertUtils.checkForMismatchingKey(cert, key);
            if (key instanceof PublicKey) // Key usage does not apply to usage of the private key, only the public key
                requireActivity(activity, cert); // will succeed on null cert, if enforcement turned off
        } else if (key instanceof DSAKey) {
            if (key instanceof PublicKey) // Key usage does not apply to usage of the private key, only the public key
                requireActivity(activity, cert); // will succeed on null cert, if enforcement turned off
        } else if (key instanceof SecretKey) {
            // Ok; nothing to check
        } else {
            throw new CertificateException("Key type not supported: " + key.getClass().getName());
        }
    }

    /**
     * Check whether the specified activity should be permitted given the specified certificate's Key Usage
     * and Extended Key Usage extensions, if any.  This method assumes that you intend to perform the activity
     * with the public key from the specified certificate.
     * <p/>
     * This method returns normally if there is no key usage enforcement policy in effect; if the certificate
     * does not have a critical Key Usage or Extended Key Usage extension; or if the key usage enforcement
     * policy currently in effect permits the specified activity using the specified certificate.
     * <p/>
     * Otherwise, it throws KeyUsageException.
     *
     * @param activity a high-level description of an activity to perform using the public key from the specified certificate.  Required.
     * @param cert the certificate to consider using for the specified activity.  If you do not have access to
     *             the relevant certificate, pass null as the certificate.  If null, this method will succeed
     *             only if enforcement is globally disabled; otherwise, it will always fail for a null certificate.
     * @throws KeyUsageException if the key usage enforcement policy currently in effect does not permit the
     *         specified activity using the specified certificate.
     * @throws java.security.cert.CertificateParsingException if there is an error parsing a critical Extended Key Usage extension
     */
    public static void requireActivity(KeyUsageActivity activity, X509Certificate cert) throws KeyUsageException, CertificateParsingException {
        if (!getDefault().permitsActivity(activity, cert))
            throw new KeyUsageException(activity, cert);
    }

    // Used to translate JDK X509Certificate boolean array into bits in a single integer
    static final int[] KEY_USAGE_BIT_VALUES = {
            128,
            64,
            32,
            16,
            8,
            4,
            2,
            1,
            32768,
    };

    /**
     * Check if the currently-in-effect KeyUsagePolicy permits the specified activity using the specified certificate.
     * <p/>
     * If no KeyUsagePolicy is installed, this always fails if the certificate has any critical key usage or
     * extended key usage extension.
     *
     * @param activity a high-level description of an activity to perform using the specified certificate.  Required.
     * @param cert the certificate to consider using for the specified activity.  Required.
     * @return true if the current policy permits this activity with this cert;
     *         false if there is no policy or the current policy does not permit this activity with this cert
     * @throws java.security.cert.CertificateParsingException if there is an error parsing a critical Extended Key Usage extension
     */
    public boolean permitsActivity(KeyUsageActivity activity, X509Certificate cert) throws CertificateParsingException {
        if (!enforcePolicy)
            return true;

        if (cert == null)
            return false;

        final Set<String> criticalOids = cert.getCriticalExtensionOIDs();
        if (criticalOids == null || criticalOids.isEmpty())
            return true;

        if (criticalOids.contains(X509_KEY_USAGE_EXTENSION_OID)) {
            boolean[] kuBools = cert.getKeyUsage();
            if (kuBools != null && !policyPermitsKeyUsageForActivity(activity, kuBools))
                return false;
        }

        if (criticalOids.contains(X509_EXTENDED_KEY_USAGE_EXTENSION_OID)) {
            List<String> extUsageOids = cert.getExtendedKeyUsage();
            if (extUsageOids != null && !policyPermitsExtendedKeyUsageForActivity(activity, extUsageOids))
                return false;
        }

        return true;
    }

    private boolean policyPermitsKeyUsageForActivity(KeyUsageActivity activity, boolean[] kuBools) {
        return !enforcePolicy || (havePolicy() && policy.isKeyUsagePermittedForActivity(activity, keyUsageToInt(kuBools)));
    }

    private boolean policyPermitsExtendedKeyUsageForActivity(KeyUsageActivity activity, List<String> extendedKeyUsageOids) {
        return !enforcePolicy || (havePolicy() && policy.isExtendedKeyUsagePermittedForActivity(activity, extendedKeyUsageOids));
    }

    private boolean havePolicy() {
        if (policy == null) {
            logger.log(Level.WARNING, "No KeyUsagePolicy is set -- will decline all key usage queries");
            return false;
        }
        return true;
    }

    private int keyUsageToInt(boolean[] kuBools) {
        int kuInt = 0;

        for (int i = 0; i < kuBools.length; i++) {
            if (kuBools[i])
                kuInt |= KEY_USAGE_BIT_VALUES[i];
        }
        return kuInt;
    }

    static FileInputStream open(String path) {
        if (path == null || path.length() < 1)
            return null;
        try {
            return new FileInputStream(path);
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, "Unable to open file: " + path + ": " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    protected static KeyUsagePolicy makeDefaultPolicy() {
        try {
            return KeyUsagePolicy.fromXml(makeDefaultPolicyXml());
        } catch (SAXException e) {
            throw new RuntimeException("Invalid default key usage policy file: " + ExceptionUtils.getMessage(e), e);
        }
    }

    static String makeDefaultPolicyXml() {
        String xmlString = SyspropUtil.getString(PROPERTY_POLICY_XML, null);
        if (xmlString != null && xmlString.trim().length() > 0)
            return xmlString;

        InputStream xmlStream = open(SyspropUtil.getString(PROPERTY_POLICY_FILE, null));

        if (xmlStream == null)
            xmlStream = KeyUsageChecker.class.getClassLoader().getResourceAsStream(DEFAULT_POLICY_RESOURCE);

        if (xmlStream == null)
            throw new RuntimeException("Unable to find default key usage policy: " + DEFAULT_POLICY_RESOURCE);

        try {
            return new String(IOUtils.slurpStream(xmlStream));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read default key usage policy file: " + ExceptionUtils.getMessage(e), e);
        }
    }

    static KeyUsageChecker makeDefaultKeyUsageChecker() {
        return new KeyUsageChecker(makeDefaultPolicy(), SyspropUtil.getString(PROPERTY_ENFORCEMENT_MODE, null));
    }
}
