/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Consumes and removes the default Security header in a message, removes any associated decorations, and returns a
 * complete record of its activities.
 *
 * @author mike
 */
public interface WssProcessor {
    public interface ParsedElement {
        Element asElement();
    }

    public interface SecurityToken extends ParsedElement {
        /**
         * Get the object format for this Security Token if applicable. Possible types
         * are LoginCredentials for UsernameToken, X509Certificate for BinarySecurityToken
         * or AssertionType for SamlSecurityToken.
         */
        Object asObject();
    }

    public interface UsernameToken extends SecurityToken {
        String getUsername();
    }

    public interface X509SecurityToken extends SecurityToken {
        X509Certificate asX509Certificate();
        boolean isPossessionProved();
    }

    public interface SamlSecurityToken extends SecurityToken {
    }

    public interface SecurityContextToken extends SecurityToken {
        SecurityContext getSecurityContext();
        String getContextIdentifier();
        boolean isPossessionProved();
    }

    /**
     * Provided by SecurityContextFinder TO the WssProcessor.  Result of looking up a session. WssProcessor will
     * never create an instance of this.
     */
    public interface SecurityContext {
        SecretKey getSharedSecret();
    }

    /**
     * Provided by the caller TO the WssProcessor.  WssProcessor will use this to look up sessions.
     * WssProcessor will never create an instance of this.
     */
    public interface SecurityContextFinder {
        SecurityContext getSecurityContext(String securityContextIdentifier);
    }

    public interface TimestampDate extends ParsedElement {
        Date asDate();
    }

    public interface Timestamp extends ParsedElement {
        TimestampDate getCreated();
        TimestampDate getExpires();
        boolean isSigned();
        SecurityToken getSigningSecurityToken();
    }

    public interface SignedElement extends ParsedElement {
        /**
         * @return either a X509SecurityToken or a DerivedKeyToken
         */
        SecurityToken getSigningSecurityToken();
    }

    public interface ProcessorResult {
        Document getUndecoratedMessage();
        SignedElement[] getElementsThatWereSigned();
        Element[] getElementsThatWereEncrypted();
        SecurityToken[] getSecurityTokens();
        Timestamp getTimestamp();
    }

    public static class ProcessorException extends Exception {}

    /**
     * This processes a soap message. That is, the contents of the Header/Security are processed as per the WSS rules.
     *
     * @param message the xml document containing the soap message. this document may be modified on exit
     * @param recipientCertificate the recipient's cert to which encrypted keys may be encoded for
     * @param recipientPrivateKey the private key corresponding to the recipientCertificate used to decypher the encrypted keys
     * @return a ProcessorResult object reffering to all the WSS related processing that happened.
     * @throws InvalidDocumentFormatException if the message is not SOAP or has some other problem that can't be ignored
     * @throws ProcessorException in case of some other problem
     * @throws GeneralSecurityException in case of problems with a key or certificate
     */
    ProcessorResult undecorateMessage(Document message,
                                      X509Certificate recipientCertificate,
                                      PrivateKey recipientPrivateKey,
                                      SecurityContextFinder securityContextFinder)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException;
}
