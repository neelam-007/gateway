package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;

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
        } else {
            elements = new ElementSecurity[]{};
        }
    }

    private ElementSecurity[] elements = new ElementSecurity[]{};
}
