/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * 
 */

package com.l7tech.security.token;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.*;
import com.l7tech.xml.UnsupportedDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * A simple implementation of the UsernameToken SecurityToken.  Not threadsafe.  The credentials can't be changed
 * once the instance is created.
 */
public class UsernameTokenImpl implements UsernameToken {
    private Element element;
    private String username; // username to include, must not be null
    private PasswordAuthentication passwordAuth; // plaintext password to include, or null to not include one (or if using digest)
    private String passwordDigest; // digested password if already known, or null to generate it when needed
    private String elementId;
    private String nonce;   // base64-encoded random byte string
    private String created; // timestamp string in ISO 8601 format
    private boolean digest;

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
        this(username, password, null, null, false);
    }

    /**
     * Create a UsernameToken that uses the specified username, password, created date, and nonce, that may
     * optionally encode the password as a digest.
     *
     * @param username the username to include.  Must not be null.
     * @param password the password to include, or null to omit the password.
     * @param created created date, or null to allow one to be generated.
     * @param nonce nonce, or null to omit it.
     * @param digest true if the password element should be digested; false to use plaintext.
     */
    public UsernameTokenImpl(String username, char[] password, String created, byte[] nonce, boolean digest) {
        if (username == null) throw new IllegalArgumentException("A username is required to create a UsernameToken");
        this.element = null;
        this.elementId = null;
        this.username = username;
        this.passwordAuth = password == null ? null : new PasswordAuthentication(username, password);
        this.created = created;
        this.nonce = nonce == null ? null : HexUtils.encodeBase64(nonce);
        this.digest = digest;
    }

    /**
     * Create a UsernameTokenImpl from the given Element.  The Element will be parsed during the construction.
     * @param utok the UsernameToken element to examine. Required.
     * @throws com.l7tech.util.InvalidDocumentFormatException if a required element is missing, or if an optional element contains an invalid value.
     * @throws com.l7tech.xml.UnsupportedDocumentFormatException if the Username is empty or the Password is present with Type other than ...#PasswordText or ...#PasswordDigest
     */
    public UsernameTokenImpl(final Element utok)
                                        throws InvalidDocumentFormatException, UnsupportedDocumentFormatException
    {
        String wsseUri = utok.getNamespaceURI();
        String username = extractUsername(utok, wsseUri);
        this.created = extractCreated(utok);
        this.nonce = extractNonce(utok, wsseUri);

        // Remember this as a security token
        this.element = utok;
        this.username = username;
        this.elementId = null;
        extractPassword(utok, wsseUri);
    }

    private static String extractUsername(Element utok, String wsseUri) throws InvalidDocumentFormatException, UnsupportedDocumentFormatException {
        // Get the Username child element
        Element usernameEl = DomUtils.findOnlyOneChildElementByName(utok, wsseUri, SoapConstants.UNTOK_USERNAME_EL_NAME);
        if (usernameEl == null) throw new InvalidDocumentFormatException("The usernametoken element does not contain a username element");
        String username = DomUtils.getTextValue(usernameEl).trim();
        if (username.length() < 1)throw new UnsupportedDocumentFormatException("The usernametoken has an empty username element");
        return username;
    }

    // Sets password, passwordDigest
    private void extractPassword(Element utok, String wsseUri) throws InvalidDocumentFormatException, UnsupportedDocumentFormatException {
        this.passwordAuth = null;
        this.passwordDigest = null;
        this.digest = false;

        // Get the password element
        Element passwdEl = DomUtils.findOnlyOneChildElementByName(utok, wsseUri, SoapConstants.UNTOK_PASSWORD_EL_NAME);
        if (passwdEl == null)
            return; // No password

        String value = DomUtils.getTextValue(passwdEl).trim();
        if (value.length() < 1) throw new InvalidDocumentFormatException("The usernametoken has an empty password element");

        // Verify the password type to be supported
        String passwdType = passwdEl.getAttribute( SoapConstants.UNTOK_PSSWD_TYPE_ATTR_NAME).trim();
        if (passwdType == null || passwdType.length() < 1 || passwdType.endsWith("PasswordText")) {
            // Assume plaintext password
            this.passwordAuth = new PasswordAuthentication(username, value.toCharArray());
            return;
        }

        if (passwdType.endsWith("PasswordDigest")) {
            // Assume digest password
            this.passwordDigest = value;
            this.digest = true;
            return;
        }

        throw new UnsupportedDocumentFormatException("This username token password type is not supported: " + passwdType);
    }

    private static String extractCreated(Element utok) throws InvalidDocumentFormatException {
        return extractOptionalElement(utok, SoapConstants.WSU_URIS_ARRAY, SoapConstants.UNTOK_CREATED_EL_NAME);
    }

    private static String extractNonce(Element utok, String wsseUri) throws InvalidDocumentFormatException {
        return extractOptionalElement(utok, new String[] { wsseUri }, SoapConstants.UNTOK_NONCE_EL_NAME);
    }

    private static String extractOptionalElement(Element utok, String[] nsUris, String elemName) throws InvalidDocumentFormatException {
        Element elem = DomUtils.findOnlyOneChildElementByName(utok, nsUris, elemName);
        if (elem == null) // Timestamp is optional
            return null;
        String value = DomUtils.getTextValue(elem).trim();
        if (value.length() < 1)
            throw new InvalidDocumentFormatException("Bad UsernameToken: empty " + elemName + " element");
        return value;
    }


    @Override
    public String getNonce() {
        return nonce;
    }

    @Override
    public String getCreated() {
        return created;
    }

    @Override
    public boolean isDigest() {
        return digest;
    }

    /** @return the username.  Never null. */
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public SecurityTokenType getType() {
        return SecurityTokenType.WSS_USERNAME;
    }

    @Override
    public String getElementId() {
        if (elementId != null)
            return elementId;
        return elementId = SoapUtil.getElementWsuId(asElement());
    }

    /**
     * Fill in the given empty UsernameToken, which must already be configured with the desired security namespace and prefix.
     * @param untokEl an empty UsernameToken element to fill in.  Required.
     */
    public void fillInDetails(Element untokEl, String wsuPrefix) {
        Document nodeFactory = untokEl.getOwnerDocument();
        String securityNs = untokEl.getNamespaceURI();
        Element usernameEl = nodeFactory.createElementNS(securityNs, "Username");
        usernameEl.setPrefix(untokEl.getPrefix());
        // attach them
        untokEl.appendChild(usernameEl);
        // fill in username value
        Text txtNode = DomUtils.createTextNode(nodeFactory, this.username);
        usernameEl.appendChild(txtNode);

        if (created == null) created = ISO8601Date.format(new Date());

        String type = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText";
        String pass = null;
        if (digest) {
            pass = getPasswordDigest();
            type = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest";
        } else if (passwordAuth != null) {
            pass = new String(passwordAuth.getPassword());
        }

        if (pass != null) {
            Element passwdEl = nodeFactory.createElementNS(securityNs, "Password");
            passwdEl.setPrefix(untokEl.getPrefix());
            untokEl.appendChild(passwdEl);
            txtNode = DomUtils.createTextNode(nodeFactory, pass);
            passwdEl.appendChild(txtNode);
            passwdEl.setAttribute("Type", type);
        }

        if (nonce != null) {
            Element nonceEl = nodeFactory.createElementNS(securityNs, "Nonce");
            nonceEl.setPrefix(untokEl.getPrefix());
            nonceEl.appendChild(DomUtils.createTextNode(untokEl, nonce));
            nonceEl.setAttribute("EncodingType", SoapUtil.ENCODINGTYPE_BASE64BINARY);
            untokEl.appendChild(nonceEl);
        }

        if ( !created.isEmpty() ) {
            Element createdEl;
            if (wsuPrefix != null) {
                createdEl = nodeFactory.createElementNS(SoapConstants.WSU_NAMESPACE, "Created");
                createdEl.setPrefix(wsuPrefix);
                untokEl.appendChild(createdEl);
            } else {
                createdEl = DomUtils.createAndAppendElementNS(untokEl, "Created", SoapConstants.WSU_NAMESPACE, "wsu");
            }
            createdEl.appendChild(DomUtils.createTextNode(untokEl, created));
        }
    }

    @Override
    public String getPasswordDigest() {
        if (passwordDigest != null)
            return passwordDigest;

        if (passwordAuth == null)
            return null;

        passwordDigest = createPasswordDigest(passwordAuth.getPassword(), created, nonce);
        return passwordDigest;
    }

    /**
     * Compute the base64-encoded digest password value for the given password, created date,
     * and nonce.
     *
     * @param password  password to digest.  Required.
     * @param created   creation date in ISO 8601 format, or null.
     * @param nonce     base64-encoded nonce bytes, or null.
     * @return the base64-encoded password digest for this password with this nonce and created date.
     */
    public static String createPasswordDigest(char[] password, String created, String nonce) {
        if (password == null)
            return null;
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            if (nonce != null) sha1.update(HexUtils.decodeBase64(nonce, true));
            if (created != null) sha1.update(created.getBytes("UTF-8"));
            sha1.update(new String(password).getBytes("UTF-8"));
            return HexUtils.encodeBase64(sha1.digest());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            // XXX This might make more sense as a checked exception
            throw new RuntimeException(e);
        }
    }

    @Override
    public Element asElement(Element context, String securityNs, String securityPrefix) {
        Document factory = context.getOwnerDocument();
        // If there's already an element, import it to preserve the existing XML instead of creating new XML
        if (element != null && (securityNs == null || securityNs.equals(element.getNamespaceURI()))) {
            if (element.getOwnerDocument() == factory)
                return element; // already imported
            Element imported = (Element)factory.importNode(element, true);
            return element = imported;
        }

        Element untokEl = factory.createElementNS(securityNs, "UsernameToken");
        untokEl.setPrefix(securityPrefix);
        fillInDetails(untokEl, XmlUtil.findActivePrefixForNamespace( context, SoapConstants.WSU_NAMESPACE ));
        return element = untokEl;
    }

    @Override
    public Element asElement() {
        if (element == null) {
            Document doc = XmlUtil.createEmptyDocument("UsernameToken", "wsse", SoapConstants.SECURITY_NAMESPACE);
            Element untokEl = doc.getDocumentElement();
            fillInDetails(untokEl, null);
            element = untokEl;
        }
        return element;
    }

    @Override
    public String toString() {
        return "UsernameToken: " + username;
    }

    /** @return the password, or null if there wasn't one. */
    @Override
    public char[] getPassword() {
        return passwordAuth == null ? null : passwordAuth.getPassword();
    }
}
