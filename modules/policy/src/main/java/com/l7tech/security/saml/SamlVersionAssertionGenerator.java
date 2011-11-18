package com.l7tech.security.saml;

import com.l7tech.common.io.CertUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;

/**
 * Convert SAML configuration into an XML document. Abstract instead of interface for convenient common
 * utility methods.
 */
abstract class SamlVersionAssertionGenerator {

    /**
     * Create an SAML Assertion containing Subject Statements for those supplied. This method converts our
     * internal SAML version independent configuration model into an XML Document containing a SAML assertion for the
     * correct version.
     *
     * @param options Options The configuration options
     * @param caDn Private Key's corresponding public key's distinguished name. May be null when a custom Issuer is configured on options.
     * If a custom issuer value is required, then caDn MUST be null.
     * @param subjectStatements The subject statements to add to the created assertion (authentication, authorization, attribute).
     * @return SAML Assertion as a Document.
     * @throws SignatureException If cannot sign.
     * @throws CertificateException If cert exception.
     * @throws IllegalStateException if both options.getCustomIssuer() and caDn are null.
     */
    abstract Document createStatementDocument(@NotNull SamlAssertionGenerator.Options options,
                                     @Nullable String caDn,
                                     @NotNull SubjectStatement ... subjectStatements)
            throws SignatureException, CertificateException, IllegalStateException;

    /**
     *  Get the CN from a DN.
     *
     * @param caDn String DN.
     * @return String CN.
     */
    String getSubjectCn(final String caDn) {
        final Map caMap = CertUtils.dnToAttributeMap(caDn);
        return (String)((List)caMap.get("CN")).get(0);
    }
}

