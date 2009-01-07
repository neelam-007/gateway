/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.xml.processor;

import com.l7tech.message.Message;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.UnexpectedKeyInfoException;
import com.l7tech.util.InvalidDocumentFormatException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

/**
 * Consumes and removes the default Security header in a message, removes any associated decorations, and returns a
 * complete record of its activities.
 *
 * @author mike
 */
public interface WssProcessor {

    /**
     * This processes a soap message in-place.
     * That is, the contents of the Header/Security are processed as per the WSS rules.
     *
     * @param message the xml document containing the soap message. this document may be modified on exit.
     * @param senderCertificate    the sender's cert, if known, so that Signatures containing SKI KeyInfos can be matched up, or null to disable this feature.
     * @param securityContextFinder a security context finder for looking up ws-sc sessions, or null to disable WS-SC support.
     * @param securityTokenResolver   a resolver for looking up certificates in various ways, or null disable certificate reference support.
     * @return a ProcessorResult object reffering to all the WSS related processing that happened.
     * @throws InvalidDocumentFormatException if there is a problem with the document format that can't be ignored
     * @throws GeneralSecurityException if there is a problem with a key or certificate
     * @throws com.l7tech.security.xml.processor.ProcessorException in case of some other problem
     * @throws BadSecurityContextException if the message contains a WS-SecureConversation SecurityContextToken, but the securityContextFinder has no record of that session.
     * @throws SAXException if the first part's content type is not text/xml; or,
     *                      if the XML in the first part's InputStream is not well formed
     * @throws IOException if there is a problem reading XML from the first part's InputStream; or,
     *                     if there is a problem reading from or writing to a stash
     * @throws IllegalStateException if the Message has not yet been initialized
     * @throws UnexpectedKeyInfoException if the message has a KeyInfo that doesn't match the expected value.  Note that
     *                                    this must be caught before ProcessorException.
     */
    ProcessorResult undecorateMessage(Message message,
                                      X509Certificate senderCertificate,
                                      SecurityContextFinder securityContextFinder,
                                      SecurityTokenResolver securityTokenResolver)
            throws ProcessorException, InvalidDocumentFormatException, GeneralSecurityException, BadSecurityContextException, SAXException, IOException;
}
