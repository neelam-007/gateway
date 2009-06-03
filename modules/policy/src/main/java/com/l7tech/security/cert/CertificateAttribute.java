package com.l7tech.security.cert;

import com.l7tech.util.ISO8601Date;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.common.io.CertUtils;

import javax.security.auth.x500.X500Principal;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.naming.directory.Attribute;
import javax.naming.NamingEnumeration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.CertificateEncodingException;
import java.io.IOException;

import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import sun.security.util.DerValue;


/**
 * Enum representing the supported certificate attributes. Each attribute can extract
 * its corresponding value from the certificate passed to the extractValue() method.
 */
public enum CertificateAttribute {

    /**
     * The DER encoded certificate.
     */
    DER("der", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            try {
                return makeMap(this.toString(), certificate.getEncoded());
            } catch (CertificateEncodingException e) {
                logger.log(Level.WARNING, "Error getting DER-encoded certificate" +
                                          ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
                return new HashMap<String, Collection<Object>>();
            }
        }},

    /**
     * The BASE64 encoded certificate.
     */
    BASE64("base64", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            try {
                return makeMap(this.toString(), HexUtils.encodeBase64(certificate.getEncoded()));
            } catch (CertificateEncodingException e) {
                logger.log(Level.WARNING, "Error getting DER-encoded certificate" +
                                          ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
                return new HashMap<String, Collection<Object>>();
            }
        }},

    /**
     * The PEM encoded certificate.
     */
    PEM("pem", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            try {
                return makeMap(this.toString(), CertUtils.encodeAsPEM(certificate));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error getting PEM-encoded certificate" +
                                          ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
                return new HashMap<String, Collection<Object>>();
            }
        }},

    /**
     * The Name of the Signature Algorithm for the certificate (e.g. "SHA1withRSA")
     */
    SIG_ALG_NAME("signatureAlgorithmName", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), certificate.getSigAlgName());
        }},

    /**
     * The OID of the Signature Algorithm for the certificate (e.g. "1.2.840.113549.1.1.5")
     */
    SIG_ALG_OID("signatureAlgorithmOID", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), certificate.getSigAlgOID());
        }},

    /**
     * The Certificate Serial# (e.g. "68652640310044618358965661752471103641")
     */
    SERIAL("serial", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
           return makeMap(this.toString(), certificate.getSerialNumber().toString());
        }},

    /**
     * The Certificate Not After Date (e.g. "2018-03-19T23:59:59.000Z")
     */
    NOT_AFTER("notAfter", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), ISO8601Date.format(certificate.getNotAfter()));
        }},

    /**
     * The Certificate Not Before Date (e.g. "2005-03-19T00:00:00.000Z")
     */
    NOT_BEFORE("notBefore", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
           return makeMap(this.toString(), ISO8601Date.format(certificate.getNotBefore()));
        }},

    /**
     * The Issuer DN (e.g. "CN=OASIS Interop Test CA, O=OASIS")
     */
    ISSUER("issuer", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), certificate.getIssuerX500Principal().toString());
        }},

    /**
     * The Issuer DN in canonical format: for comparisons; limited subset of OID names;
     * strict sorting, whitespace, and case rules
     */
    ISSUER_CANONICAL("issuer.canonical", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), certificate.getIssuerX500Principal().getName(X500Principal.CANONICAL));
        }},

    /**
     * The Issuer DN in RFC 2253 format: for correct but still reasonably pretty output
     * (only includes RFC 2253 OID names)
     */
    ISSUER_RFC2253("issuer.rfc2253", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), certificate.getIssuerX500Principal().getName(X500Principal.RFC2253));
        }},

    /**
     * An array of values for the Issuer DN parts corresponding to the attrNameWithKey parameter
     * given to the extractValue method.
     */
    ISSUER_DN("issuer.dn", true, true) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return getValuesFromsX500Principal(certificate.getIssuerX500Principal(), this.toString());
        }},

    /**
     * EMail address (if any) for the Issuer Alternative Name (rfc288) (e.g. "example@ca.oasis-open.org")
     */
    ISSUER_ALT_EMAIL("issuerAltNameEmail", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), getIssuerAltName(certificate, AltName.EMAIL));
        }},

    /**
     * DNS Name address (if any) for the Issuer Alternative Name (e.g. "ca.oasis-open.org")
     */
    ISSUER_ALT_DNS("issuerAltNameDNS", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), getIssuerAltName(certificate, AltName.DNS));
        }},

    /**
     * Uniform Resource Identifier (if any) for the Issuer Alternative Name (e.g. "http://ca.oasis-open.org/")
     */
    ISSUER_ALT_URI("issuerAltNameURI", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), getIssuerAltName(certificate, AltName.URI));
        }},

    /**
     * The Subject DN (e.g. "CN=Alice, OU=OASIS Interop Test Cert, O=OASIS")
     */
    SUBJECT("subject", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), certificate.getSubjectX500Principal().toString());
        }},

    /**
     * The Subject DN in canonical format: for comparisons; limited subset of OID names;
     * strict sorting, whitespace, and case rules
     */
    SUBJECT_CANONICAL("suject.canonical", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), certificate.getSubjectX500Principal().getName(X500Principal.CANONICAL));
        }},

    /**
     * The Subject DN in RFC 2253 format: for correct but still reasonably pretty output
     * (only includes RFC 2253 OID names)
     */
    SUBJECT_RFC2253("subject.rfc2253", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), certificate.getSubjectX500Principal().getName(X500Principal.RFC2253));
        }},

    /**
     * An array of values for the Subject DN parts corresponding to the attrNameWithKey parameter
     * given to the extractValue method.
     */
    SUBJECT_DN("subject.dn", true, true) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return getValuesFromsX500Principal(certificate.getSubjectX500Principal(), this.toString());
        }},

    /**
     * The Name of the Algorithm used for the Subject's Public Key (e.g. "RSA")
     */
    SUBJECT_PUB_KEY_ALG("subjectPublicKeyAlgorithm", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), certificate.getPublicKey().getAlgorithm());
        }},

    /**
     * The BASE64 encoded value of the subject key identifier (SKI) extension
     * or the derived SKI if an extension is not present.
     */
    SUBJECT_KEY_IDENTIFIER("subjectKeyIdentifier", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), CertUtils.getSki(certificate));
        }},

    /**
     * EMail address (if any) for the Subject Alternative Name (rfc288) (e.g. "example2@oasis-open.org")
     */
    SUBJECT_ALT_EMAIL("subjectAltNameEmail", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), getSubjectAltName(certificate, AltName.EMAIL));
        }},

    /**
     * DNS Name address (if any) for the Subject Alternative Name (e.g. "example2.oasis-open.org")
     */
    SUBJECT_ALT_DNS("subjectAltNameDNS", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), getSubjectAltName(certificate, AltName.DNS));
        }},

    /**
     * Uniform Resource Identifier (if any) for the Subject Alternative Name (e.g. "http://example2.oasis-open.org/")
     */
    SUBJECT_ALT_URI("subjectAltNameURI", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), getSubjectAltName(certificate, AltName.URI));
        }},

    /**
     * The BASE64 encoded value of the SHA-1 hash for the DER encoded certificate .
     */
    THUMBPRINT_SHA1("thumbprintSHA1", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            try {
                return makeMap(this.toString(), CertUtils.getThumbprintSHA1(certificate));
            } catch (CertificateEncodingException e) {
                logger.log(Level.WARNING, "Error getting DER-encoded certificate" +
                                          ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
                return new HashMap<String, Collection<Object>>();
            }
        }},

    /**
     * An array of countries that the certificate reports citizenship for
     */
    COUNTRY_OF_CITIZENSHIP("countryOfCitizenship", false, true) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return BCSubjectDirectoryAttributesExtractor.extractCitizenshipCountries(certificate, this.toString());
        }},

    /**
     * Criticality of the key usage field (none, noncrit, critical)
     */
    KEY_USAGE_CRITICALITY("keyUsageCriticality", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            boolean[] usages = certificate.getKeyUsage();
            return makeMap(this.toString(), usages == null ? KEYUSAGE_NONE : certificate.getCriticalExtensionOIDs().contains(X509_KEY_USAGE_EXTENSION_OID) ? KEYUSAGE_CRITICAL : KEYUSAGE_NONCRIT);
        }},

    /**
     * Digital Signature (true/false)
     */
    KEY_USAGE_DIGITAL_SIGNATURE("keyUsage.digitalSignature", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            boolean[] usages = certificate.getKeyUsage();
            return makeMap(this.toString(), usages != null && usages[0]);
        }},

    /**
     * Non Repudiation (true/false)
     */
    KEY_USAGE_NON_REPUDIATION("keyUsage.nonRepudiation", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            boolean[] usages = certificate.getKeyUsage();
            return makeMap(this.toString(), usages != null && usages[1]);
        }},

    /**
     * Key Encipherment (true/false)
     */
    KEY_USAGE_KEY_ENCIPHERMENT("keyUsage.keyEncipherment", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            boolean[] usages = certificate.getKeyUsage();
            return makeMap(this.toString(), usages != null && usages[2]);
        }},

    /**
     * Data Encipherment (true/false)
     */
    KEY_USAGE_DATA_ENCIPHERMENT("keyUsage.dataEncipherment", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            boolean[] usages = certificate.getKeyUsage();
            return makeMap(this.toString(), usages != null && usages[3]);
        }},

    /**
     * Key Agreement (true/false)
     */
    KEY_USAGE_KEY_AGREEMENT("keyUsage.keyAgreement", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            boolean[] usages = certificate.getKeyUsage();
            return makeMap(this.toString(), usages != null && usages[4]);
        }},

    /**
     * Key Certificate Sign (true/false)
     */
    KEY_USAGE_KEY_CERT_SIGN("keyUsage.keyCertSign", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            boolean[] usages = certificate.getKeyUsage();
            return makeMap(this.toString(), usages != null && usages[5]);
        }},

    /**
     * CRL Sign (true/false)
     */
    KEY_USAGE_CRL_SIGN("keyUsage.crlSign", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            boolean[] usages = certificate.getKeyUsage();
            return makeMap(this.toString(), usages != null && usages[6]);
        }},

    /**
     * Encipher Only (true/false)
     */
    KEY_USAGE_ENCIPHER_ONLY("keyUsage.encipherOnly", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            boolean[] usages = certificate.getKeyUsage();
            return makeMap(this.toString(), usages != null && usages[7]);
        }},

    /**
     * Decipher Only (true/false)
     */
    KEY_USAGE_DECIPHER_ONLY("keyUsage.decipherOnly", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            boolean[] usages = certificate.getKeyUsage();
            return makeMap(this.toString(), usages != null && usages[8]);
        }},

    /**
     * Extended key usage extension presence and criticality.  The value is either null, "noncrit", or "critical".
     */
    EXTENDED_KEY_USAGE_CRITICALITY("extendedKeyUsageCriticality", false, false) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            List<String> usages = null;
            try {
                usages = certificate.getExtendedKeyUsage();
            } catch (CertificateParsingException e) {
                logger.log(Level.WARNING, "Error extracting extended key usage criticality" +
                    ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            }
            return makeMap(this.toString(), usages == null ? KEYUSAGE_NONE : certificate.getCriticalExtensionOIDs().contains(X509_EXT_KEY_USAGE_EXTENSION_OID) ? KEYUSAGE_CRITICAL : KEYUSAGE_NONCRIT);
        }},

    /**
     * The key usage information. Each value is an OID.
     */
    EXTENDED_KEY_USAGE_VALUES("extendedKeyUsageValues", false, true) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            List<String> usages = new ArrayList<String>();
            try {
                usages = certificate.getExtendedKeyUsage();
            } catch (CertificateParsingException e) {
                logger.log(Level.WARNING, "Error extracting extended key usage criticality" +
                    ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            }
            return makeMap(this.toString(), usages != null ? usages : new ArrayList<Object>());
        }},

    /**
     * The certificate policies information. Each value is an OID.
     */
    CERTIFICATE_POLICIES("certificatePolicies", false, true) {
        @Override
        public Map<String, Collection<Object>> extractValues(X509Certificate certificate) {
            return makeMap(this.toString(), extractCertificatePolicies(certificate));
        }},
    ;


    // - PRIVATE

    private static final String X509_KEY_USAGE_EXTENSION_OID="2.5.29.15";
    private static final String X509_EXT_KEY_USAGE_EXTENSION_OID="2.5.29.37";
    private static final Logger logger = Logger.getLogger( CertificateAttribute.class.getName() );

    // Values for determining presence and criticality of keyusage and extkeyusage extensions
    private static final String KEYUSAGE_NONE = "none";
    private static final String KEYUSAGE_CRITICAL = "critical";
    private static final String KEYUSAGE_NONCRIT = "noncrit";

    private static final Pattern OID_PATTERN = Pattern.compile("^(?:\\d+)(?:\\.\\d+)$");

    private String attributeName;
    private boolean prefixed;
    private boolean multiValued;

    private CertificateAttribute(String name, boolean prefixed, boolean multiValued) {
        this.attributeName = name;
        this.prefixed = prefixed;
        this.multiValued = multiValued;
    }

    private static final Map<String,CertificateAttribute> stringToEnum = new HashMap<String, CertificateAttribute>();
    static {
        for (CertificateAttribute a : values()) {
            stringToEnum.put(a.toString(), a);
        }
    }

    // from X509Certificate#getSubjectAlternativeNames(), since there doesn't seem to be set of constants elsewhere
    private static enum AltName {
        OTHER        ("altNameOther",        0),   // otherName                  [0]     OtherName,
        EMAIL        ("altNameEmail",        1),   // rfc822Name                 [1]     IA5String,
        DNS          ("altNameDNS",          2),   // dNSName                    [2]     IA5String,
        X400         ("altNameX400",         3),   // x400Address                [3]     ORAddress,
        DIRECTORY    ("altNameDirectory",    4),   // directoryName              [4]     Name,
        EDI_PARTY    ("altNameEdiParty",     5),   // ediPartyName               [5]     EDIPartyName,
        URI          ("aleNameURI",          6),   // uniformResourceIdentifier  [6]     IA5String,
        IP           ("altNameIPAddress",    7),   // iPAddress                  [7]     OCTET STRING,
        REGISTERED_ID("altNameRegisteredID", 8),   // registeredID               [8]     OBJECT IDENTIFIER}
        ;

        private String friendlyName;
        private int intType;

        private AltName(String friendlyName, int intType) {
            this.friendlyName = friendlyName;
            this.intType = intType;
        }

        @Override
        public String toString() {
            return friendlyName;
        }

        public Integer getType() {
            return intType;
        }
    }


    // - PRIVATE UTILS

    private static Map<String,Collection<Object>> makeMap(final String key, final Object value) {
        Map<String,Collection<Object>> result = new HashMap<String, Collection<Object>>();

        if (key == null || value == null)
            return result;

        if (value.getClass().isArray() && Object.class.isAssignableFrom(value.getClass().getComponentType())) {
            result.put(key, new ArrayList<Object>() {{ addAll(Arrays.asList((Object[])value)); }});
        } else if (value instanceof Collection) {
            result.put(key, new ArrayList<Object>() {{ addAll(((Collection<?>)value)); }});
        } else {
            result.put(key, new ArrayList<Object>() {{ add(value); }});
        }

        return result;
    }

    private static void addToMap(Map<String, Collection<Object>> result, String key, final Object value) {
        if (result == null) return;
        Collection<Object> existing = result.get(key);
        if (existing == null) {
            result.put(key, new ArrayList<Object>() {{ add(value); }} );
        } else {
            existing.add(value);
        }
    }

    private static Map<String,Collection<Object>> getValuesFromsX500Principal(X500Principal x500Principal, String attrName) {
        Map<String,Collection<Object>> result = new HashMap<String, Collection<Object>>();
        try {
            if (x500Principal != null) {
                int rdnPos = 0;
                List<Rdn> rdns = new ArrayList<Rdn>(new LdapName(x500Principal.getName(X500Principal.RFC2253)).getRdns());
                Collections.reverse(rdns);
                for (Rdn rdn : rdns) {
                    rdnPos++;
                    addToMap(result, attrName + "." + Integer.toString(rdnPos), rdn.toString());

                    NamingEnumeration<? extends Attribute> attrs = rdn.toAttributes().getAll();
                    while (attrs.hasMore()) {
                        Attribute a = attrs.next();
                        String id = a.getID().toLowerCase();
                        if (OID_PATTERN.matcher(id).matches())
                            id = "oid." + id;
                        for(int i=0; i<a.size(); i++) {
                            String value = attributeValueToString(a.get(i));
                            addToMap(result, attrName + "." + Integer.toString(rdnPos) + "." + id, value);
                            addToMap(result, attrName + "." + id, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // should not happen
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Error Could not extract issuer alternative names from certificate '" +
                    ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            }
        }
        return result;
    }

    private static String getIssuerAltName(X509Certificate certificate, AltName altNameType) {
        try {
            return getAltName(certificate.getIssuerAlternativeNames(), altNameType);
        } catch (CertificateParsingException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Could not extract issuer alternative names from certificate '" +
                                        ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            }
        }
        return null;
    }

    private static String getSubjectAltName(X509Certificate certificate, AltName altNameType) {
        try {
            return getAltName(certificate.getSubjectAlternativeNames(), altNameType);
        } catch (CertificateParsingException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Could not extract issuer alternative names from certificate '" +
                                        ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            }
        }
        return null;
    }

    private static String getAltName(Collection<List<?>> altNames, AltName altNameType) {
        if (altNames != null) {
            Integer intType = altNameType.getType();
            for (List<?> altName : altNames) {
                if (altName != null && altName.size() > 0 && (intType.equals(altName.get(0)))) {
                    Object value = altName.get(1);
                    if (value instanceof String) {
                        return (String) value;
                    } else if (value instanceof byte[]) {
                        try {
                            return (new DerValue((byte[]) value)).toString();
                        } catch (IOException e) {
                            logger.log(Level.WARNING, "Error extracting value for {0}", altNameType);
                            return null;
                        }
                    } else { // should not happen
                        logger.log(Level.WARNING, "Invalid alternative name value type: {0}", value == null ? null : value.getClass());
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private static List<String> extractCertificatePolicies(X509Certificate certificate) {
        List<String> policies = new ArrayList<String>();
        byte[] extensionBytes = certificate.getExtensionValue(X509Extensions.CertificatePolicies.getId());
        if(extensionBytes != null && extensionBytes.length > 0) {
            try {
                Object extensionValue = X509ExtensionUtil.fromExtensionValue(extensionBytes);
                    if(extensionValue != null && extensionValue instanceof DERSequence) {
                        DERSequence policiesSequence = (DERSequence)extensionValue;
                        for(int i = 0;i < policiesSequence.size();i++) {
                            DERSequence oidSequence = (DERSequence)policiesSequence.getObjectAt(i);
                            if(oidSequence.size() > 0) {
                                policies.add(((DERObjectIdentifier)oidSequence.getObjectAt(0)).getId());
                            }
                        }
                    }
            } catch(IOException ioe) {
                logger.log(Level.WARNING, "Failed to parse certificate policies (check certificate)");
            }
        }
        return policies;
    }

    /**
     * Separate class since this uses server only lib.
     */
    private static final class BCSubjectDirectoryAttributesExtractor {
        /**
         * Country of citizenship values from the Subject Directory Attributes field.
         *
         * joint-iso-ccitt(2) ds(5) 29 id-ce-subjectDirectoryAttributes(9)
         * 2.5.29.9
         *
         * @param cert  the certificate to examine.  Required.
         * @param s
         * @return a List containing zero or more ISO country codes (ie, "US", "CA")
         */
        private static Map<String, Collection<Object>> extractCitizenshipCountries(final X509Certificate cert, String attrName) {
            Map<String, Collection<Object>> citizenshipCountries = new HashMap<String, Collection<Object>>();
            try {
                byte[] extensionBytes = cert.getExtensionValue(X509Extensions.SubjectDirectoryAttributes.getId());
                if(extensionBytes == null || extensionBytes.length == 0) {
                    return citizenshipCountries;
                }

                Object extensionValue = X509ExtensionUtil.fromExtensionValue(extensionBytes);
                if(extensionValue == null || !(extensionValue instanceof DERSequence)) {
                    return citizenshipCountries;
                }
                DERSequence subjectDirAttrs = (DERSequence)extensionValue;
                for(int i = 0;i < subjectDirAttrs.size();i++) {
                    if(!(subjectDirAttrs.getObjectAt(i) instanceof DERSequence)) {
                        continue;
                    }

                    DERSequence seq = (DERSequence)subjectDirAttrs.getObjectAt(i);
                    if(seq.size() < 2 || !(seq.getObjectAt(0) instanceof DERObjectIdentifier)) {
                        continue;
                    }

                    DERObjectIdentifier id = (DERObjectIdentifier)seq.getObjectAt(0);
			        if(id.equals(X509Name.COUNTRY_OF_CITIZENSHIP)) {
                        if(!(seq.getObjectAt(1) instanceof DERSet)) {
                            continue;
                        }

                        DERSet cocSet = (DERSet)seq.getObjectAt(1);
				        for(int j = 0;j < cocSet.size();j++) {
                            if(!(cocSet.getObjectAt(j) instanceof DERPrintableString)) {
                                continue;
                            }

                            DERPrintableString countryCode = (DERPrintableString)cocSet.getObjectAt(j);
                            addToMap(citizenshipCountries, attrName, countryCode.getString());
                        }
		        	}
		        }

                return citizenshipCountries;
            } catch(IOException ioe) {
                if ( logger.isLoggable(Level.FINE) ) {
                    logger.log( Level.FINE, "Could not extract citizenship countries from certificate '"+ExceptionUtils.getMessage(ioe)+"'.", ExceptionUtils.getDebugException(ioe));
                }

                return citizenshipCountries;
            }
        }
    }

    // - PUBLIC

    public abstract Map<String,Collection<Object>> extractValues(X509Certificate certificate);

    @Override
    public String toString() {
        return attributeName;
    }

    public static CertificateAttribute fromString(String attrName) {
        if (attrName == null)
            return null;
        else if (attrName.startsWith( ISSUER_DN.toString() + ".") )
            return ISSUER_DN;
        else if (attrName.startsWith( SUBJECT_DN.toString() + ".") )
            return SUBJECT_DN;
        else
            return stringToEnum.get(attrName);
    }

    public boolean isPrefixed() {
        return prefixed;
    }

    public boolean isMultiValued() {
        return multiValued;
    }

    public static String attributeValueToString(Object value) {
        return value == null ? null : value instanceof byte[] ? "#" + HexUtils.hexDump((byte[])value) : value.toString();
    }
}
