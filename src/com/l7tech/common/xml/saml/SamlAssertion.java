/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml.saml;

import com.l7tech.common.security.token.SamlSecurityToken;
import com.l7tech.common.security.xml.processor.X509SigningSecurityTokenImpl;
import com.l7tech.common.security.xml.SecurityTokenResolver;
import com.l7tech.common.security.saml.SamlConstants;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import org.apache.xmlbeans.XmlObject;

import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.*;

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

    public abstract X509Certificate getIssuerCertificate();

    public abstract void setIssuerCertificate(X509Certificate issuerCertificate);

    public abstract X509Certificate getAttestingEntity();

    /**
     * Set the attesting entity certificate. This models
     * @param attestingEntity
     */
    public abstract void setAttestingEntity(X509Certificate attestingEntity);

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
