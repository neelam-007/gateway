package com.l7tech.security.cert;

import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Utility class to extract attributes from a certificate.
 *
 * @author steve
 */
public class X509CertificateAttributesExtractor {

    //- PUBLIC

    /**
     * Create an attribute extractor for the given X.509 Certificate.
     *
     * @param certificate The certificate to use
     */
    public X509CertificateAttributesExtractor( final X509Certificate certificate ) {
        this.certificate = certificate;
    }

    /**
     * Get all attribute names known by the extractor.
     *
     * @return The collection of names.
     */
    public Collection<String> getSuppotedAttributeNames() {
        Collection<String> result = new ArrayList<String>();
        for(CertificateAttribute attribute : EnumSet.allOf(CertificateAttribute.class)) {
            result.add(attribute.toString());
        }
        return result;
    }

    public boolean isSupportedAttribute(String attributeName) {
        return CertificateAttribute.fromString(attributeName) != null;
    }

    /**
     * Get the value of the specified attribute.
     *
     * @param attribute The name of the attribute, ie {@link #ATTR_SIG_ALG_NAME}.
     * @return The attribute value or null if it does not exist.
     */
    public Object getAttributeValue( final String attributeName ) {
        CertificateAttribute attribute = CertificateAttribute.fromString(attributeName);

        Map<String, Collection<Object>> entries = attribute == null ? null : new TreeMap<String, Collection<Object>>(String.CASE_INSENSITIVE_ORDER);
        if (entries != null)
            entries.putAll(attribute.extractValuesIncludingLegacyNames(certificate));

        Collection<Object> values = entries == null ? null : entries.get(attributeName);
        if (values == null && attribute != null && attribute.isPrefixed()) // valid attribute, but sub-component not found
            values = new ArrayList<Object>();

        if (values == null)
            throw new IllegalArgumentException("Unknown certificate attribute name: " + attributeName);

        return attribute.isMultiValued() ? values.toArray() : values.isEmpty() ? null : values.iterator().next();
    }

    //- PRIVATE

    private final X509Certificate certificate;

}
