/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.xml.saml;

import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.SamlSecurityToken;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.UnexpectedKeyInfoException;
import com.l7tech.security.xml.processor.X509SigningSecurityTokenImpl;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.Resolver;
import org.apache.xmlbeans.XmlObject;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.security.GeneralSecurityException;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.TimeZone;

/**
 * Encapsulates an abstract saml:Assertion SecurityToken.
 */
public abstract class SamlAssertion extends X509SigningSecurityTokenImpl implements SamlSecurityToken {
    protected static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    /**
     * Constructs a new SamlAssertion from an XML Element.
     * <p/>
     * The resulting object could be either sender-vouches or holder-of-key depending on the
     * SubjectConfirmation/ConfirmationMethod found in the XML; separate subclasses are no longer used.
     *
     * @param assertionElement xmlassertion the xml element containing the assertion.  Must be saml:Assertion.
     * @throws SAXException    if the format of this assertion is invalid or not supported
     * @throws NullPointerException if the given element is null
     */
    public static SamlAssertion newInstance(Element assertionElement) throws SAXException {
        return newInstance(assertionElement, null);
    }

    /**
     * Construct a new SamlAssertion from an XML element, using the specified thumbprint resolver to locate
     * certificates by thumbprint reference (as opposed to inline keyinfo).
     *
     * @param assertionElement the XML element containing the assertion.  Must be a saml:Assertion element.
     * @param securityTokenResolver  the resolver for thumbprint KeyInfos, or null for no thumbprint support.
     * @throws org.xml.sax.SAXException    if the format of this assertion is invalid or not supported
     * @throws org.xml.sax.SAXException    if the KeyInfo used a thumbprint, but no thumbprint resolver was supplied.
     */
    public static SamlAssertion newInstance(Element assertionElement,
                                            SecurityTokenResolver securityTokenResolver) throws SAXException {
        if (SamlConstants.NS_SAML2.equals(assertionElement.getNamespaceURI())) {
            return new SamlAssertionV2(assertionElement, securityTokenResolver);
        }
        else {
            return new SamlAssertionV1(assertionElement, securityTokenResolver);
        }
    }

    /**
     * Constructs a new SamlAssertion from an XML Element.
     * <p/>
     * The resulting object could be either sender-vouches or holder-of-key depending on the
     * SubjectConfirmation/ConfirmationMethod found in the XML; separate subclasses are no longer used.
     *
     * @param ass xmlassertion the xml element containing the assertion.  Must be saml:Assertion.
     */
    protected SamlAssertion(Element ass) {
        super(ass);
    }

    public abstract void setIssuerCertificate(X509Certificate issuerCertificate);

    public abstract X509Certificate getAttestingEntity();

    /**
     * Set the attesting entity certificate. This models
     * @param attestingEntity
     */
    public abstract void setAttestingEntity(X509Certificate attestingEntity);

    public abstract Element getEmbeddedIssuerSignature();

    /**
     * Get the DOM Element representing an EncryptedKey used for subject confirmation, if one is present.
     * <p/>
     * Even if an encrypted key is returned, it still may not have been unwrapped; call call {@link com.l7tech.security.token.EncryptedKey#isUnwrapped()} to
     * check whether the unwrapped secret key is already available.
     *
     * @param tokenResolver resolver for finding information that may be needed for unwrapping an embedded encrypted key. Pass a ContextualSecurityTokenResolver if BST resolution is desired. Required unless {@link #isSubjectConfirmationEncryptedKeyAvailable()} returned true.
     * @return an EncryptedKey, or null if one was not present or could not be processed
     * @throws InvalidDocumentFormatException if there is a problem with the format of the SAML assertion or the encrypted key.
     * @throws GeneralSecurityException if there was a problem with the recipient certificate or a certificate embedded within the EncryptedKey.
     * @throws UnexpectedKeyInfoException  if the EncryptedKey's KeyInfo did not match private key known to the tokenResolver.
     */
    public abstract EncryptedKey getSubjectConfirmationEncryptedKey(SecurityTokenResolver tokenResolver) throws InvalidDocumentFormatException, UnexpectedKeyInfoException, GeneralSecurityException;

    /**
     * Check if a subject confirmation EncryptedKey has already been located and examined (though not necessarily unwrapped).
     * <p/>
     * If this returns true, you may call {@link #getSubjectConfirmationEncryptedKey} without passing in any resolvers.
     *
     * @return true if {@link #getSubjectConfirmationEncryptedKey} would return non-null if called with null arguments.
     */
    public abstract boolean isSubjectConfirmationEncryptedKeyAvailable();

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SamlAssertion that = (SamlAssertion) o;

        if (!getAssertionId().equals(that.getAssertionId())) return false;
        if (!getUniqueId().equals(that.getUniqueId())) return false;

        return true;
    }

    public int hashCode() {
        return getUniqueId().hashCode();
    }

    static class CausedSignatureException extends SignatureException {
        public CausedSignatureException() {
        }

        public CausedSignatureException(String msg) {
            super(msg);
        }

        public CausedSignatureException(String msg, Throwable t) {
            super(msg);
            initCause(t);
        }

        public CausedSignatureException(Throwable t) {
            super();
            initCause(t);
        }
    }

    static class ResolveIdException extends RuntimeException {
        public ResolveIdException(String s) {
            super(s);
        }
    }

    /**
     * @return the Xml Beans assertion type.  Never null.
     */
    public abstract XmlObject getXmlBeansAssertionType();
}
