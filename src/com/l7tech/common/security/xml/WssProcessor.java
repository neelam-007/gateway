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

/**
 * Consumes and removes the default Security header in a message, removes any associated decorations, and returns a
 * complete record of its activities.
 *
 * @author mike
 */
public interface WssProcessor {
    public interface ParsedElement {
        Element asElement();
        String asXmlString();
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
    }

    public interface TimestampDate extends ParsedElement {
        Date asDate();
    }

    public interface Timestamp extends ParsedElement {
        TimestampDate getCreated();
        TimestampDate getExpires();
    }

    public interface ProcessorResult {
        Document getUndecoratedMessage();
        Element[] getElementsThatWereSigned();
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
                                      PrivateKey recipientPrivateKey)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException;
}
