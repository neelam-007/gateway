package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.common.security.xml.ElementSecurity;

/**
 * Enforces XML security on the message elements or the entire message.
 * <p/>
 * <code>ElementSecurity</code> list.
 * <p/>
 *
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
public class XmlResponseSecurity extends Assertion implements XmlSecurityAssertion {
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
     * Test if this assertion has an encryption element specified.
     * <p/>
     *
     * @return true if this instance has encryption required for an
     *         element, false otherwise
     */
    public boolean hasEncryptionElement() {
        for (int i = 0; i < elements.length; i++) {
            ElementSecurity elementSecurity = elements[i];
            // authenticated if Xpath points to envelope
            if (elementSecurity.isEncryption()) {
                return true;
            }
        }
        return false;
    }


    private ElementSecurity[] elements = new ElementSecurity[]{};
}
