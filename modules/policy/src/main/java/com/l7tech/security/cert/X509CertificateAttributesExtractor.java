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
        if (attribute == null) return null;

        Map<String, Collection<Object>> entries = attribute.extractValues(certificate);
        if (entries == null) return null;

        Collection<Object> values = entries.get(attributeName);
        if (values == null) return null;

        if (attribute.isMultiValued())
            return values.toArray();
        else if (values.isEmpty())
            return null;
        else
            return values.iterator().next();
    }

    //- PRIVATE

    private final X509Certificate certificate;

}
