/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import org.w3c.dom.Document;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Consumes and removes the default Security header in a message, removes any associated decorations, and returns a
 * complete record of its activities.
 *
 * @author mike
 */
public interface WssProcessor {

    /**
     * This processes a soap message. That is, the contents of the Header/Security are processed as per the WSS rules.
     *
     * @param message the xml document containing the soap message. this document may be modified on exit
     * @param recipientCertificate the recipient's cert to which encrypted keys may be encoded for
     * @param recipientPrivateKey the private key corresponding to the recipientCertificate used to decypher the encrypted keys
     * @return a ProcessorResult object reffering to all the WSS related processing that happened.
     * @throws InvalidDocumentFormatException if the message is not SOAP or has some other problem that can't be ignored
     * @throws com.l7tech.common.security.xml.processor.ProcessorException in case of some other problem
     * @throws GeneralSecurityException in case of problems with a key or certificate
     * @throws BadSecurityContextException if the message contains a WS-SecureConversation SecurityContextToken, but the securityContextFinder has no record of that session.
     */
    ProcessorResult undecorateMessage(Document message,
                                      X509Certificate recipientCertificate,
                                      PrivateKey recipientPrivateKey,
                                      SecurityContextFinder securityContextFinder)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException, BadSecurityContextException;
}
