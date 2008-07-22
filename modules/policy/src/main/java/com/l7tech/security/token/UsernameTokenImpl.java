/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * 
 */

package com.l7tech.security.token;

import com.l7tech.util.ISO8601Date;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.UnsupportedDocumentFormatException;
import com.l7tech.common.io.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.net.PasswordAuthentication;
import java.util.Date;

/**
 * A simple implementation of the UsernameToken SecurityToken.  Not threadsafe.  The credentials can't be changed
 * once the instance is created.
 */
public class UsernameTokenImpl implements UsernameToken {
    private Element element;
    private String username; // username to include, must not be null
    private PasswordAuthentication passwordAuth; // password to include, or null to not include one
    private String elementId;

    /**
     * Create a UsernameTokenImpl from the given credentials, which must be cleartext.
     * @param pc a PasswordAuthentication, which must not be null, containing a non-null username and a non-null (but possibly empty) password.
     */
    public UsernameTokenImpl(PasswordAuthentication pc) {
        this.element = null;
        this.elementId = null;
        this.username = pc.getUserName();
        this.passwordAuth = pc;
    }

    /**
     * Create a UsernameTokenImpl from the given username and password.
     * @param username the username to include.  Must not be null.
     * @param password the password to include, or null to omit the password.
     */
    public UsernameTokenImpl(String username, char[] password) {
        if (username == null) throw new IllegalArgumentException("A username is required to create a UsernameToken");
        this.element = null;
        this.elementId = null;
        this.username = username;
        this.passwordAuth = password == null ? null : new PasswordAuthentication(username, password);
    }

    /** Create a UsernameTokenImpl from the given Element.  The Element will be parsed during the construction. */
    public UsernameTokenImpl(final Element usernameTokenElement)
                                        throws InvalidDocumentFormatException, UnsupportedDocumentFormatException
    {
        String applicableWsseNS = usernameTokenElement.getNamespaceURI();
        // Get the Username child element
        Element usernameEl = DomUtils.findOnlyOneChildElementByName(usernameTokenElement,
                                                                   applicableWsseNS,
                                                                   SoapConstants.UNTOK_USERNAME_EL_NAME);
        if (usernameEl == null) {
            throw new InvalidDocumentFormatException("The usernametoken element does not contain a username element");
        }
        String username = DomUtils.getTextValue(usernameEl).trim();
        if (username.length() < 1) {
            throw new InvalidDocumentFormatException("The usernametoken has an empty username element");
        }
        // Get the password element
        Element passwdEl = DomUtils.findOnlyOneChildElementByName(usernameTokenElement,
                                                                 applicableWsseNS,
                                                                 SoapConstants.UNTOK_PASSWORD_EL_NAME);
        String passwd = null;
        if (passwdEl != null) {
            passwd = DomUtils.getTextValue(passwdEl).trim();
            if (passwd.length() < 1) {
                throw new InvalidDocumentFormatException("The usernametoken has an empty password element");
            }
            // Verify the password type to be supported
            String passwdType = passwdEl.getAttribute( SoapConstants.UNTOK_PSSWD_TYPE_ATTR_NAME).trim();
            if (passwdType.length() > 0) {
                if (!passwdType.endsWith("PasswordText")) {
                    throw new UnsupportedDocumentFormatException("This username token password type is not supported: " + passwdType);
                }
            }
        }
        // Remember this as a security token
        this.element = usernameTokenElement;
        this.username = username;
        this.passwordAuth = passwd == null ? null : new PasswordAuthentication(username, passwd.toCharArray());
        this.elementId = null;
    }

    /** @return the username.  Never null. */
    public String getUsername() {
        return username;
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.WSS_USERNAME;
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
        Text txtNode = DomUtils.createTextNode(nodeFactory, this.username);
        usernameEl.appendChild(txtNode);
        // fill in password value and type
        if (passwordAuth != null) {
            char[] pass = passwordAuth.getPassword();
            Element passwdEl = nodeFactory.createElementNS(securityNs, "Password");
            passwdEl.setPrefix(untokEl.getPrefix());
            untokEl.appendChild(passwdEl);
            txtNode = DomUtils.createTextNode(nodeFactory, new String(pass));
            passwdEl.appendChild(txtNode);
            passwdEl.setAttribute("Type",
                              "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
        }
        Element createdEl = DomUtils.createAndAppendElementNS(untokEl, "Created", SoapConstants.WSU_NAMESPACE, "wsu");
        createdEl.appendChild(DomUtils.createTextNode(untokEl, ISO8601Date.format(new Date())));
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
            Document doc = XmlUtil.createEmptyDocument("UsernameToken", "wsse", SoapConstants.SECURITY_NAMESPACE);
            Element untokEl = doc.getDocumentElement();
            fillInDetails(untokEl);
            element = untokEl;
        }
        return element;
    }

    public String toString() {
        return "UsernameToken: " + username;
    }

    /** @return the password, or null if there wasn't one. */
    public char[] getPassword() {
        return passwordAuth == null ? null : passwordAuth.getPassword();
    }
}
