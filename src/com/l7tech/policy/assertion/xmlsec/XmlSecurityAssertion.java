package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.ElementSecurity;

/**
 * The <code>XmlSecurityAssertion</code> interface is implemented by assertions
 * that offer document or document portion security. Document or document portion
 * security properties are specified with the array of <code>ElementSecurity</code>
 * elements.
 * <p/>
 *
 * @see ElementSecurity
 */
public interface XmlSecurityAssertion {
    /**
     * Return the array of XML security elements
     *
     * @return the array of XML security elements
     */
    ElementSecurity[] getElements();

    /**
     * Set the array of XML security elements
     *
     * @param elements the new security elements
     */
    void setElements(ElementSecurity[] elements);

}
