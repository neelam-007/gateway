package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.common.security.xml.ElementSecurity;

/**
 * Enforces the XML security on the message elements or entire message
 * 
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
public class XmlRequestSecurity extends CredentialSourceAssertion implements XmlSecurityAssertion {
    /**
     * Return the array of security elements that are specified
     *
     * @return the array of XML security elements
     */
    public ElementSecurity[] getElements() {
        return elements;
    }

    /**
     * Set the array of XML security elements
     *
     * @param elements the new security elements
     */
    public void setElements(ElementSecurity[] elements) {
        if (elements != null) {
            this.elements = elements;
        }
    }

    /**
     * Test if this assertion can be considered as a credential source.
     * The assertion is a credential source if it signs the nevelope.
     * <p/>
     * The <code>CredentialSourceAssertion<code> is left as superclass
     * for a moment. It sould be removed, at the moment it will impact
     * many things.
     *
     * @return true if this instance is credential source, false otherwise
     */
    public boolean hasAuthenticationElement() {
        for (int i = 0; i < elements.length; i++) {
            ElementSecurity elementSecurity = elements[i];
            // authenticated if Xpath points to envelope
            if (ElementSecurity.isEnvelope(elementSecurity)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Test if this assertion has an encryption element specified.
     * <p/>
     *
     * @return true if this instance has encryption required for an
     *         element, false otherwise
     */
    public boolean hasEncryptionElement() {
        for (int i = 0; i < elements.length; i++) {
            ElementSecurity elementSecurity = elements[i];
            if (elementSecurity.isEncryption()) {
                return true;
            }
        }
        return false;
    }


    private ElementSecurity[] elements = new ElementSecurity[]{};
}
