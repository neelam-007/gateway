package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;

import java.util.ArrayList;
import java.util.List;

/**
 * Enforces the XML security on the message elements or entire message
 * 
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
public class XmlRequestSecurity extends CredentialSourceAssertion implements XmlSecurityAssertion {
    /**
     * default constructor
     */
    public XmlRequestSecurity() {
        elements.add(new ElementSecurity());
    }

    /**
     * Return the array of security elements that are specified
     *
     * @return the array of XML security elements
     */
    public ElementSecurity[] elements() {
        return (ElementSecurity[])elements.toArray(new ElementSecurity[]{});
    }

    /**
     * Add the security element to the list of elements
     *
     * @param xse the security element
     */
    public void addSecurityElement(XmlSecurityAssertion xse) {
        elements.add(xse);
    }

    /**
     * Remove the security element from the list of elements
     *
     * @param xse the security element
     */
    public void removeSecurityElement(XmlSecurityAssertion xse) {
        elements.remove(xse);
    }


    private List elements = new ArrayList();
}
