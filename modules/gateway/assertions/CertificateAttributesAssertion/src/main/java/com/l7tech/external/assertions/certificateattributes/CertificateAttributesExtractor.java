package com.l7tech.external.assertions.certificateattributes;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ISO8601Date;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.extension.X509ExtensionUtil;

import javax.naming.NamingException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Utility class to extract attributes from a certificate.
 *
 * @author steve
 */
public class CertificateAttributesExtractor {

    //- PUBLIC

    /**
     * Create an attribute extractor for the given X.509 Certificate.
     *
     * @param certificate The certificate to use
     */
    public CertificateAttributesExtractor( final X509Certificate certificate ) {
        this.certificateAttributes = extractAttributes(certificate);
    }

    /**
     * Get the names of all attributes available for this certificate.
     *
     * @return The collection of names.
     */
    public Collection<String> getAttributeNames() {
        return certificateAttributes.keySet();
    }

    /**
     * Get the value of the specified attribute.
     *
     * @param attribute The name of the attribute, ie {@link #ATTR_SIG_ALG_NAME}.
     * @return The attribute value or null if it does not exist.
     */
    public Object getAttributeValue( final String attribute ) {
        return certificateAttributes.get( attribute );
    }

    /**
     * Get the names of all simple (unprefixed) attributes.  The returned list does not include the subcomponents
     * of ISSUER and SUBJECT since these are defined at runtime based on the contents of the certificate.
     *
     * <p>Note that any given certificate may only have a subset of these
     * attributes.</p>
     *
     * @return The colllection of attribute names
     */
    public static Collection<String> getSimpleCertificateAttributes() {
        return Arrays.asList(ATTRS);
    }

    /**
     * Check if the specified attribute name has subcomponents that are defined at runtime.
     *
     * @param attrName the attribute name to check to see if it is a prefix.  Required.
     * @return true if the specified attribute name will have subcomponents not listed as simple attributes.
     */
    public static boolean hasSubcomponents(String attrName) {
        return ATTR_ISSUER_PREFIX.equalsIgnoreCase(attrName) || ATTR_SUBJECT_PREFIX.equalsIgnoreCase(attrName);
    }

    //- PRIVATE

    private static final String X509_KEY_USAGE_EXTENSION_OID="2.5.29.15";
    private static final String X509_EXT_KEY_USAGE_EXTENSION_OID="2.5.29.37";

    // Values for determining presence and criticality of keyusage and extkeyusage extensions
    private static final String VALUE_NONE = "none";
    private static final String VALUE_CRITICAL = "critical";
    private static final String VALUE_NONCRIT = "noncrit";

    /**
     * The Name of the Signature Algorithm for the certificate (e.g. "SHA1withRSA")
     */
    private static final String ATTR_SIG_ALG_NAME = "signatureAlgorithmName";

    /**
     * The OID of the Signature Algorithm for the certificate (e.g. "1.2.840.113549.1.1.5")
     */
    private static final String ATTR_SIG_ALG_OID = "signatureAlgorithmOID";

    /**
     * The Certificate Serial# (e.g. "68652640310044618358965661752471103641")
     */
    private static final String ATTR_SERIAL_NUM = "serial";

    /**
     * The Certificate Not After Date (e.g. "2018-03-19T23:59:59.000Z")
     */
    private static final String ATTR_NOT_AFTER = "notAfter";

    /**
     * The Certificate Not Before Date (e.g. "2005-03-19T00:00:00.000Z")
     */
    private static final String ATTR_NOT_BEFORE = "notBefore";

    /** Prefix for issuer subvariables. */
    private static final String ATTR_ISSUER_PREFIX = "issuer";

    /**
     * The Issuer DN (e.g. "CN=OASIS Interop Test CA, O=OASIS") in default human-friendly format
     */
    private static final String ATTR_ISSUER = "issuer.dn";

    /**
     * The Issuer DN in canonical format (more useful for comparisons).
     */
    private static final String ATTR_ISSUER_CANONICAL = "issuer.dn.canonical";

    /**
     * The Issuer DN in RFC 2253 format.
     */
    private static final String ATTR_ISSUER_RFC2253 = "issuer.dn.rfc2253";

    /**
     * Email address (if any) from the Issuer DN (e.g. "example@ca.oasis-open.org")
     */
    private static final String ATTR_ISSUER_EMAIL = "issuerEmail";

    /**
     * EMail address (if any) for the Issuer Alternative Name (rfc288) (e.g. "example@ca.oasis-open.org")
     */
    private static final String ATTR_ISSUER_ALT_EMAIL = "issuerAltNameEmail";

    /**
     * DNS Name address (if any) for the Issuer Alternative Name (e.g. "ca.oasis-open.org")
     */
    private static final String ATTR_ISSUER_ALT_DNS = "issuerAltNameDNS";

    /**
     * Uniform Resource Identifier (if any) for the Issuer Alternative Name (e.g. "http://ca.oasis-open.org/")
     */
    private static final String ATTR_ISSUER_ALT_URI = "issuerAltNameURI";

    /** Prefix for subject-related subvariables. */
    private static final String ATTR_SUBJECT_PREFIX = "subject";

    /**
     * The Subject DN (e.g. "CN=Alice, OU=OASIS Interop Test Cert, O=OASIS") in default human-friendly format
     */
    private static final String ATTR_SUBJECT = "subject.dn";

    /**
     * The Subject DN in canonical format, more useful for comparisons.
     */
    private static final String ATTR_SUBJECT_CANONICAL = "subject.dn.canonical";

    /**
     * The Subject DN in RFC 2253 format.
     */
    private static final String ATTR_SUBJECT_RFC2253 = "subject.dn.rfc2253";

    /**
     * The Name of the Algorithm used for the Subject's Public Key (e.g. "RSA")
     */
    private static final String ATTR_SUBJECT_PUB_KEY_ALG = "subjectPublicKeyAlgorithm";

    /**
     * Email address (if any) from the Subject DN (e.g. "example2@oasis-open.org")
     */
    private static final String ATTR_SUBJECT_EMAIL = "subjectEmail";

    /**
     * EMail address (if any) for the Subject Alternative Name (rfc288) (e.g. "example2@oasis-open.org")
     */
    private static final String ATTR_SUBJECT_ALT_EMAIL = "subjectAltNameEmail";

    /**
     * DNS Name address (if any) for the Subject Alternative Name (e.g. "example2.oasis-open.org")
     */
    private static final String ATTR_SUBJECT_ALT_DNS = "subjectAltNameDNS";

    /**
     * Uniform Resource Identifier (if any) for the Subject Alternative Name (e.g. "http://example2.oasis-open.org/")
     */
    private static final String ATTR_SUBJECT_ALT_URI = "subjectAltNameURI";

    /**
     * List of countries that the certificate claims citizenship in.
     */
    private static final String ATTR_SUBJECT_DOMAIN_ATTR_COUNTRY_OF_CITIZENSHIP = "countryOfCitizenship";

    /**
     * Key usage extension presence and criticality.  The value is either null, "noncrit", or "critical".
     */
    private static final String ATTR_KEY_USAGE_CRITICALITY  = "keyUsage.criticality";

    /**
     * Key usage context variable names. The values are Boolean objects.
     */
    private static final String ATTR_KEY_USAGE_DIGITAL_SIGNATURE = "keyUsage.digitalSignature";
    private static final String ATTR_KEY_USAGE_NON_REPUDIATION = "keyUsage.nonRepudiation";
    private static final String ATTR_KEY_USAGE_KEY_ENCIPHERMENT = "keyUsage.keyEncipherment";
    private static final String ATTR_KEY_USAGE_DATA_ENCIPHERMENT = "keyUsage.dataEncipherment";
    private static final String ATTR_KEY_USAGE_KEY_AGREEMENT = "keyUsage.keyAgreement";
    private static final String ATTR_KEY_USAGE_KEY_CERT_SIGN = "keyUsage.keyCertSign";
    private static final String ATTR_KEY_USAGE_CRL_SIGN = "keyUsage.cRLSign";
    private static final String ATTR_KEY_USAGE_DECIPHER_ONLY = "keyUsage.decipherOnly";

    /**
     * Extended key usage extension presence and criticality.  The value is either null, "noncrit", or "critical".
     */
    private static final String ATTR_EXTENDED_KEY_USAGE_CRITICALITY = "extendedKeyUsage.criticality";

    /**
     * List of OIDs from the extended key usage.
     */
    private static final String ATTR_EXTENDED_KEY_USAGE = "extendedKeyUsage";

    /**
     * List of OIDs for the certificate policies
     */
    private static final String ATTR_CERTIFICATE_POLICIES = "certificatePolicies";

    /**
     * All known attributes except SUBJECT and ISSUER subcomponents
     */
    private static final String[] ATTRS = {
            ATTR_SIG_ALG_NAME,
            ATTR_SIG_ALG_OID,
            ATTR_SERIAL_NUM,
            ATTR_NOT_AFTER,
            ATTR_NOT_BEFORE,
            ATTR_ISSUER_PREFIX,
            ATTR_ISSUER_CANONICAL,
            ATTR_ISSUER_RFC2253,
            ATTR_ISSUER_EMAIL,
            ATTR_ISSUER_ALT_EMAIL,
            ATTR_ISSUER_ALT_DNS,
            ATTR_ISSUER_ALT_URI,
            ATTR_SUBJECT_PREFIX,
            ATTR_SUBJECT_CANONICAL,
            ATTR_SUBJECT_RFC2253,
            ATTR_SUBJECT_PUB_KEY_ALG,
            ATTR_SUBJECT_EMAIL,
            ATTR_SUBJECT_ALT_EMAIL,
            ATTR_SUBJECT_ALT_DNS,
            ATTR_SUBJECT_ALT_URI,
            ATTR_SUBJECT_DOMAIN_ATTR_COUNTRY_OF_CITIZENSHIP,
            ATTR_KEY_USAGE_CRITICALITY,
            ATTR_KEY_USAGE_DIGITAL_SIGNATURE,
            ATTR_KEY_USAGE_NON_REPUDIATION,
            ATTR_KEY_USAGE_KEY_ENCIPHERMENT,
            ATTR_KEY_USAGE_DATA_ENCIPHERMENT,
            ATTR_KEY_USAGE_KEY_AGREEMENT,
            ATTR_KEY_USAGE_KEY_CERT_SIGN,
            ATTR_KEY_USAGE_CRL_SIGN,
            ATTR_KEY_USAGE_DECIPHER_ONLY,
            ATTR_EXTENDED_KEY_USAGE,
            ATTR_EXTENDED_KEY_USAGE_CRITICALITY,
            ATTR_CERTIFICATE_POLICIES,
};

    private static final Logger logger = Logger.getLogger( CertificateAttributesExtractor.class.getName() );

    private final Map<String,Object> certificateAttributes;

    /**
     * Extract attributes from the given cert, return immutable map.
     * @param certificate the certificate to examine.  Required.
     * @return a Map of attribute name (not including any runtime variable prefix) to attribute value (usually
     *         a String or String[]).
     */
    private static Map<String,Object> extractAttributes( final X509Certificate certificate ) {
        Map<String,Object> attributes = new HashMap<String,Object>();

        attributes.put( ATTR_SIG_ALG_NAME, certificate.getSigAlgName() );
        attributes.put( ATTR_SIG_ALG_OID, certificate.getSigAlgOID() );
        attributes.put( ATTR_SERIAL_NUM, certificate.getSerialNumber().toString() );
        attributes.put( ATTR_NOT_AFTER, ISO8601Date.format(certificate.getNotAfter()));
        attributes.put( ATTR_NOT_BEFORE, ISO8601Date.format(certificate.getNotBefore()));

        final X500Principal issuer = certificate.getIssuerX500Principal();
        final X500Principal subject = certificate.getSubjectX500Principal();

        // Human readable (nicely formatted, has names for a wide variety of attribute OIDs
        attributes.put( ATTR_ISSUER, issuer.toString() );
        attributes.put( ATTR_SUBJECT, subject.toString() );

        // Canonical, for comparisons (limited subset of OID names; strict sorting, whitespace, and case rules)
        attributes.put( ATTR_ISSUER_CANONICAL, issuer.getName(X500Principal.CANONICAL) );
        attributes.put( ATTR_SUBJECT_CANONICAL, subject.getName(X500Principal.CANONICAL) );

        // RFC 2253, for correct but still reasonably pretty output (only includes RFC 2253 OID names).
        attributes.put( ATTR_ISSUER_RFC2253, issuer.getName(X500Principal.RFC2253) );
        attributes.put( ATTR_SUBJECT_RFC2253, subject.getName(X500Principal.RFC2253) );

        attributes.put( ATTR_SUBJECT_PUB_KEY_ALG, certificate.getPublicKey().getAlgorithm() );

        putIssuerDNAttributes( attributes, issuer);
        putIssuerAltNameAttributes( attributes, certificate );

        putSubjectDNAttributes( attributes, subject);
        putSubjectAltNameAttributes( attributes, certificate );

        putCitizenshipCountries(attributes, certificate);

        final Set<String> criticalExtensionOids = certificate.getCriticalExtensionOIDs();
        putKeyUsage(attributes, criticalExtensionOids, certificate.getKeyUsage());
        putExtendedKeyUsage(attributes, criticalExtensionOids, certificate);

        putCertificatePolicies(attributes, certificate);

        return Collections.unmodifiableMap(attributes);
    }

    public List<String> getDynamicKeys() {
        List<String> keys = new ArrayList<String>();

        for(String key : certificateAttributes.keySet()) {
            if(key.startsWith("issuer.") && !key.startsWith("issuer.dn.") || key.startsWith("subject.") && !key.startsWith("issuer.dn.")) {
                keys.add(key);
            }
        }

        return keys;
    }

    private static void putIssuerDNAttributes( final Map<String,Object> attributes, X500Principal issuer ) {
        putDNEmail( attributes, issuer, ATTR_ISSUER_EMAIL );
        putDNFields( attributes, issuer, ATTR_ISSUER_PREFIX );
    }

    private static void putSubjectDNAttributes( final Map<String,Object> attributes, X500Principal subject ) {
        putDNEmail( attributes, subject, ATTR_SUBJECT_EMAIL );
        putDNFields( attributes, subject, ATTR_SUBJECT_PREFIX );
    }

    private static final Pattern OID_PATTERN = Pattern.compile("^(?:\\d+)(?:\\.\\d+)$");

    private static void putDNFields( final Map<String,Object> attributes, X500Principal x500Principal, String prefix ) {
        try {
            LdapName ldapName = new LdapName(x500Principal.getName(X500Principal.RFC2253));
            prefix = prefix + '.';

            List<Rdn> rdns = new ArrayList<Rdn>(ldapName.getRdns());
            Collections.reverse(rdns);
            Map<String, List<String>> attrs = new LinkedHashMap<String, List<String>>();
            for (Rdn rdn : rdns) {
                String id = rdn.getType().toLowerCase(); // TODO decide what to do about multivalued RDNs
                if (OID_PATTERN.matcher(id).matches())
                    id = "oid." + id;
                Object valObj = rdn.getValue();

                final String valString;
                if (valObj instanceof byte[]) {
                    valString = "#" + HexUtils.hexDump((byte[])valObj);
                } else {
                    valString = valObj == null ? null : valObj.toString();
                }

                putAdd(attrs, prefix + id, valString);
            }

            for (Map.Entry<String, List<String>> entry : attrs.entrySet()) {
                final List<String> val = entry.getValue();
                attributes.put(entry.getKey(), val.toArray(new String[val.size()]));
            }

        } catch (NamingException e) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log( Level.FINE, "Could not extract DN components from certificate '"+ ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e));
            }
        }
    }

    /**
     * Add a value to a map that allows multiple values per key.
     * <p/>
     * This method is not threadsafe, regardless of whether the map and the lists within it are individually threadsafe.
     *
     * @param map the map to which a value is to be added.  Required.
     * @param key the key to put.  Required.
     * @param val the value to add for this key.  Required.  This method will add the value to the list of values
     *            for this key even if another identical value is already present.
     */
    private static <K, V> void putAdd(Map<K, List<V>> map, K key, V val) {
        List<V> list = map.get(key);
        if (list == null) {
            list = new ArrayList<V>();
            map.put(key, list);
        }
        list.add(val);
    }

    private static void putDNEmail( final Map<String,Object> attributes, X500Principal x500Principal, String attrName ) {
        String email = BCEmailExtractor.extractEmail(x500Principal);
        if ( email != null ) {
            attributes.put( attrName, email );
        }
    }

    private static void putIssuerAltNameAttributes( final Map<String,Object> attributes, final X509Certificate certificate ) {
        try {
            Collection<List<?>> alternativeNames = certificate.getIssuerAlternativeNames();
            if ( alternativeNames != null ) {
                putAltNameAttributes( attributes, alternativeNames, ATTR_ISSUER_ALT_EMAIL, ATTR_ISSUER_ALT_DNS, ATTR_ISSUER_ALT_URI );
            }
        } catch ( CertificateParsingException cpe ) {
            if ( logger.isLoggable(Level.FINE) ) {
                logger.log( Level.FINE, "Could not extract issuer alternative names from certificate '"+ExceptionUtils.getMessage(cpe)+"'.", ExceptionUtils.getDebugException(cpe));
            }
        }
    }

    private static void putSubjectAltNameAttributes( final Map<String,Object> attributes, final X509Certificate certificate ) {
        try {
            Collection<List<?>> alternativeNames = certificate.getSubjectAlternativeNames();
            if ( alternativeNames != null ) {
                putAltNameAttributes( attributes, alternativeNames, ATTR_SUBJECT_ALT_EMAIL, ATTR_SUBJECT_ALT_DNS, ATTR_SUBJECT_ALT_URI );
            }
        } catch ( CertificateParsingException cpe ) {
            if ( logger.isLoggable(Level.FINE) ) {
                logger.log( Level.FINE, "Could not extract subject alternative names from certificate '"+ExceptionUtils.getMessage(cpe)+"'.", ExceptionUtils.getDebugException(cpe));
            }
        }
    }

    private static void putAltNameAttributes( final Map<String,Object> attributes,
                                              final Collection<List<?>> alternativeNames,
                                              final String attrNameEmail,
                                              final String attrNameDNS,
                                              final String attrNameURI ) {
        for ( List<?> alternativeName : alternativeNames ) {
            Integer type = (Integer) alternativeName.get(0);
            // see X509Certificate#getSubjectAlternativeNames() for values.
            switch ( type ) {
                case 1:
                    attributes.put( attrNameEmail, alternativeName.get(1));
                    break;
                case 2:
                    attributes.put( attrNameDNS, alternativeName.get(1));
                    break;
                case 6:
                    attributes.put( attrNameURI, alternativeName.get(1));
                    break;
            }
        }
    }

    private static void putCertificatePolicies(final Map<String,Object> attributes,
                                               final X509Certificate cert) {
        List<String> policies = new ArrayList<String>();

        byte[] extensionBytes = cert.getExtensionValue(X509Extensions.CertificatePolicies.getId());
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

        attributes.put(ATTR_CERTIFICATE_POLICIES, policies.toArray());
    }

    private static void putCitizenshipCountries(final Map<String,Object> attributes,
                                                final X509Certificate cert) {
        List<String> countries = BCSubjectDirectoryAttributesExtractor.extractCitizenshipCountries(cert);
        attributes.put(ATTR_SUBJECT_DOMAIN_ATTR_COUNTRY_OF_CITIZENSHIP, countries.toArray());
    }

    private static void putKeyUsage(final Map<String, Object> attributes,
                                    Set<String> criticalExtensionOids,
                                    boolean[] usages)
    {
        if (usages == null) {
            attributes.put(ATTR_KEY_USAGE_CRITICALITY, VALUE_NONE);
        } else {
            boolean crit = criticalExtensionOids.contains(X509_KEY_USAGE_EXTENSION_OID);
            attributes.put(ATTR_KEY_USAGE_CRITICALITY, crit ? VALUE_CRITICAL : VALUE_NONCRIT);
        }

        attributes.put(ATTR_KEY_USAGE_DIGITAL_SIGNATURE, usages != null && usages[0]);
        attributes.put(ATTR_KEY_USAGE_NON_REPUDIATION, usages != null && usages[1]);
        attributes.put(ATTR_KEY_USAGE_KEY_ENCIPHERMENT, usages != null && usages[2]);
        attributes.put(ATTR_KEY_USAGE_DATA_ENCIPHERMENT, usages != null && usages[3]);
        attributes.put(ATTR_KEY_USAGE_KEY_AGREEMENT, usages != null && usages[4]);
        attributes.put(ATTR_KEY_USAGE_KEY_CERT_SIGN, usages != null && usages[5]);
        attributes.put(ATTR_KEY_USAGE_CRL_SIGN, usages != null && usages[6]);
        attributes.put(ATTR_KEY_USAGE_DECIPHER_ONLY, usages != null && usages[7]);
    }

    private static void putExtendedKeyUsage(final Map<String, Object> attributes,
                                            Set<String> criticalExtensionOids,
                                            X509Certificate cert)
    {
        try {
            List<String> extendedKeyUsages = cert.getExtendedKeyUsage();
            if (extendedKeyUsages == null) {
                attributes.put(ATTR_EXTENDED_KEY_USAGE_CRITICALITY, VALUE_NONE);
            } else {
                boolean crit = criticalExtensionOids.contains(X509_EXT_KEY_USAGE_EXTENSION_OID);
                attributes.put(ATTR_EXTENDED_KEY_USAGE_CRITICALITY, crit ? VALUE_CRITICAL : VALUE_NONCRIT);
            }
            attributes.put(ATTR_EXTENDED_KEY_USAGE, extendedKeyUsages == null ? new String[0] : extendedKeyUsages.toArray(new String[extendedKeyUsages.size()]));
        } catch(CertificateParsingException cpe) {
            attributes.put(ATTR_EXTENDED_KEY_USAGE_CRITICALITY, VALUE_NONE);
            attributes.put(ATTR_EXTENDED_KEY_USAGE, new String[0]);
        }
    }

    /**
     * Separate class since this uses server only lib.
     */
    private static final class BCEmailExtractor {
        /**
         * EmailAddress is defined in PKCS-9 as:
         *
         *   iso(1) member-body(2) US(840) rsadsi(113549) pkcs(1) pkcs-9(9) EmailAddress(1)
         *   1.2.840.113549.1.9.1
         * @param x500Principal the X500Principal to examine for an email address value.  Required.
         * @return the email address from this X500Principal, if any, or null.
         */
        private static String extractEmail( final X500Principal x500Principal ) {
            String email = null;

            try {
                X509Principal principal = new X509Principal( x500Principal.getEncoded() );
                Vector values = principal.getValues(X509Principal.EmailAddress);
                if ( values.size() == 1 ) {
                    email = (String) values.get(0);
                }
            } catch (IOException ioe) {
                if ( logger.isLoggable(Level.FINE) ) {
                    logger.log( Level.FINE, "Could not extract email from certificate '"+ExceptionUtils.getMessage(ioe)+"'.", ExceptionUtils.getDebugException(ioe));
                }
            }

            return email;
        }
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
         * @return a List containing zero or more ISO country codes (ie, "US", "CA")
         */
        private static List<String> extractCitizenshipCountries(final X509Certificate cert) {
            List<String> citizenshipCountries = new ArrayList<String>();
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
                            citizenshipCountries.add(countryCode.getString());
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
}
