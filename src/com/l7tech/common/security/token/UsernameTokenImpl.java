/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.token;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * A simple implementation of the UsernameToken SecurityToken.  Not threadsafe.
 */
public class UsernameTokenImpl implements UsernameToken {
    private Element element;
    private LoginCredentials creds;
    private String elementId;

    /** Create a UsernameTokenImpl from the given username and password. */
    public UsernameTokenImpl(String username, char[] password) {
        this.element = null;
        this.elementId = null;
        this.creds = new LoginCredentials(username, password, null);
    }

    /** Create a UsernameTokenImpl from the given Element.  The Element will be parsed during the construction. */
    public UsernameTokenImpl(final Element usernameTokenElement) throws InvalidDocumentFormatException {
        String applicableWsseNS = usernameTokenElement.getNamespaceURI();
        // Get the Username child element
        Element usernameEl = XmlUtil.findOnlyOneChildElementByName(usernameTokenElement,
                                                                   applicableWsseNS,
                                                                   SoapUtil.UNTOK_USERNAME_EL_NAME);
        if (usernameEl == null) {
            throw new InvalidDocumentFormatException("The usernametoken element does not contain a username element");
        }
        String username = XmlUtil.getTextValue(usernameEl).trim();
        if (username.length() < 1) {
            throw new InvalidDocumentFormatException("The usernametoken has an empty username element");
        }
        // Get the password element
        Element passwdEl = XmlUtil.findOnlyOneChildElementByName(usernameTokenElement,
                                                                 applicableWsseNS,
                                                                 SoapUtil.UNTOK_PASSWORD_EL_NAME);
        if (passwdEl == null) {
            throw new InvalidDocumentFormatException("The usernametoken element does not contain a password element");
        }
        String passwd = XmlUtil.getTextValue(passwdEl).trim();
        if (passwd.length() < 1) {
            throw new InvalidDocumentFormatException("The usernametoken has an empty password element");
        }
        // Verify the password type to be supported
        String passwdType = passwdEl.getAttribute(SoapUtil.UNTOK_PSSWD_TYPE_ATTR_NAME).trim();
        if (passwdType.length() > 0) {
            if (!passwdType.endsWith("PasswordText")) {
                throw new InvalidDocumentFormatException("This username token password type is not supported: " + passwdType);
            }
        }
        // Remember this as a security token
        this.element = usernameTokenElement;
        this.creds = new LoginCredentials(username, passwd.toCharArray(), null);
        this.elementId = null;
    }

    public String getUsername() {
        return creds.getLogin();
    }

    public LoginCredentials asLoginCredentials() {
        return creds;
    }

    public String getElementId() {
        if (elementId != null)
            return elementId;
        return elementId = SoapUtil.getElementWsuId(asElement());
    }

    /** Fill in the given empty UsernameToken, which must already be configured with the desired security namespace and prefix. */
    public void fillInDetails(Element untokEl) {
        Document nodeFactory = untokEl.getOwnerDocument();
        String securityNs = untokEl.getNamespaceURI();
        Element usernameEl = nodeFactory.createElementNS(securityNs, "Username");
        usernameEl.setPrefix(untokEl.getPrefix());
        Element passwdEl = nodeFactory.createElementNS(securityNs, "Password");
        passwdEl.setPrefix(untokEl.getPrefix());
        // attach them
        untokEl.appendChild(usernameEl);
        untokEl.appendChild(passwdEl);
        // fill in username value
        Text txtNode = XmlUtil.createTextNode(nodeFactory, creds.getLogin());
        usernameEl.appendChild(txtNode);
        // fill in password value and type
        txtNode = XmlUtil.createTextNode(nodeFactory, new String(creds.getCredentials()));
        passwdEl.appendChild(txtNode);
        passwdEl.setAttribute("Type",
                              "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
    }

    /** @return XML serialized version of this SecurityToken using the specified Security namespace and owner document. */
    public Element asElement(Document factory, String securityNs, String securityPrefix) {
        Element untokEl = factory.createElementNS(securityNs, "UsernameToken");
        untokEl.setPrefix(securityPrefix);
        fillInDetails(untokEl);
        return element = XmlUtil.findFirstChildElement(factory.getDocumentElement());
    }

    /**
     * @return XML serialized version of this SecurityToken.  This will return an existing element, if there is one.
     *         Otherwise, a new element will be created as the root of a new document and returned.
     *         This will use the default security namespace and prefix.
     */
    public Element asElement() {
        if (element == null) {
            Document doc = XmlUtil.createEmptyDocument("UsernameToken", "wsse", SoapUtil.SECURITY_NAMESPACE);
            Element untokEl = doc.getDocumentElement();
            fillInDetails(untokEl);
            element = untokEl;
        }
        return element;
    }

    public String toString() {
        return "UsernameToken: " + creds.getLogin();
    }
}
