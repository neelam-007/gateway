/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.security.GeneralSecurityException;
import java.util.Date;

import com.l7tech.common.xml.InvalidDocumentFormatException;

import javax.crypto.SecretKey;

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
         * are LoginCredentials for UsernameToken or X509Certificate for BinarySecurityToken
         */
        Object asObject();
    }

    public interface UsernameToken extends SecurityToken {
        String getUsername();
    }

    public interface X509SecurityToken extends SecurityToken {
        X509Certificate asX509Certificate();
        boolean isPossessionProved();
        Element[] getElementsSignedWithThisCert(); // TODO remove this expensive method if it remains unneeded
    }

    public interface SecurityContextToken extends SecurityToken {
        SecurityContext getSecurityContext();
    }

    public interface DerivedKeyToken extends SecurityToken {
        /**
         * The wsu:Id attribute of this derived key so that it can be referenced from.
         */
        String getId();

        /**
         * The actual symmetric key data to be used to verify signatures or decrypt.
         */
        byte[] getComputedDerivedKey();
    }

    public interface SecurityContext {
        SecretKey getSharedSecret();
    }

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
        X509SecurityToken getSigningSecurityToken();
    }

    public interface SignedElement extends ParsedElement {
        X509SecurityToken getSigningSecurityToken();
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
