/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.token;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.UnsupportedDocumentFormatException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * A simple implementation of the UsernameToken SecurityToken.  Not threadsafe.  The credentials can't be changed
 * once the instance is created.
 */
public class UsernameTokenImpl implements UsernameToken {
    private Element element;
    private LoginCredentials creds;
    private String elementId;

    /** Create a UsernameTokenImpl from the given credentials, which must be cleartext. */
    public UsernameTokenImpl(LoginCredentials pc) {
        this.element = null;
        this.elementId = null;
        this.creds = pc;
    }

    /** Create a UsernameTokenImpl from the given username and password. */
    public UsernameTokenImpl(String username, char[] password) {
        this(new LoginCredentials(username, password, null));
    }

    /** Create a UsernameTokenImpl from the given Element.  The Element will be parsed during the construction. */
    public UsernameTokenImpl(final Element usernameTokenElement)
                                        throws InvalidDocumentFormatException, UnsupportedDocumentFormatException
    {
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
        String passwd = null;
        if (passwdEl != null) {
            passwd = XmlUtil.getTextValue(passwdEl).trim();
            if (passwd.length() < 1) {
                throw new InvalidDocumentFormatException("The usernametoken has an empty password element");
            }
            // Verify the password type to be supported
            String passwdType = passwdEl.getAttribute(SoapUtil.UNTOK_PSSWD_TYPE_ATTR_NAME).trim();
            if (passwdType.length() > 0) {
                if (!passwdType.endsWith("PasswordText")) {
                    throw new UnsupportedDocumentFormatException("This username token password type is not supported: " + passwdType);
                }
            }
        }
        // Remember this as a security token
        this.element = usernameTokenElement;
        this.creds = new LoginCredentials(username, passwd == null ? null : passwd.toCharArray(), WssBasic.class);
        this.elementId = null;
    }

    public String getUsername() {
        return creds.getLogin();
    }

    public LoginCredentials asLoginCredentials() {
        return creds;
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.USERNAME;
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
        // attach them
        untokEl.appendChild(usernameEl);
        // fill in username value
        Text txtNode = XmlUtil.createTextNode(nodeFactory, creds.getLogin());
        usernameEl.appendChild(txtNode);
        // fill in password value and type
        char[] pass = creds.getCredentials();
        if (pass != null) {
            Element passwdEl = nodeFactory.createElementNS(securityNs, "Password");
            passwdEl.setPrefix(untokEl.getPrefix());
            untokEl.appendChild(passwdEl);
            txtNode = XmlUtil.createTextNode(nodeFactory, new String(creds.getCredentials()));
            passwdEl.appendChild(txtNode);
            passwdEl.setAttribute("Type",
                              "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
        }
    }

    public Element asElement(Document factory, String securityNs, String securityPrefix) {
        // If there's already an element, import it to preserve the existing XML instead of creating new XML
        if (element != null) {
            if (element.getOwnerDocument() == factory)
                return element; // already imported
            Element imported = (Element)factory.importNode(element, true);
            return element = imported;
        }

        Element untokEl = factory.createElementNS(securityNs, "UsernameToken");
        untokEl.setPrefix(securityPrefix);
        fillInDetails(untokEl);
        return element = untokEl;
    }

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
