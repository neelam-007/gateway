/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.xml.processor;

import com.l7tech.security.token.*;
import com.l7tech.security.xml.SecurityActor;
import org.w3c.dom.Element;

import java.util.List;

/**
 * Holds the result of calling WssProcessor.
 */
public interface ProcessorResult {
    SignedElement[] getElementsThatWereSigned();
    SignedPart[] getPartsThatWereSigned();
    EncryptedElement[] getElementsThatWereEncrypted();

    /**
     * @param element the element to find the signing tokens for
     * @return the array if tokens that signed the element or empty array if none
     */
    SigningSecurityToken[] getSigningTokens(Element element);

    XmlSecurityToken[] getXmlSecurityTokens();
    WssTimestamp getTimestamp();
    String getSecurityNS();
    String getWSUNS();
    SecurityActor getProcessedActor();

    /**
     * Get a list of all SignatureValue text nodes whose owning ds:Signature signatures were validated successfully.
     * This is needed to obey the WSS 1.1 requirement to include SignatureConfirmation values in a response
     * to a signed request.
     *
     * @return a List of all SignatureValue strings whose owning signatures were successfully validated while
     *         processing this message.  Each of this is the still-base64-encoded content of a dsig:SignatureValue
     *         element.
     *         <p/>
     *         May be empty, but never null.
     */
    List<String> getValidatedSignatureValues();

    /**
     * Get all SignatureConfirmation elements that were seen while processing this message.
     * <b>Important: Caller is responsible for checking that these elements were properly signed.</b>
     * (The WssProcessor could conceivably check that the elements were signed by <i>something</i>, but not whether
     * the signing tokens were trusted to provided the confirmations in question.)
     *
     * @return a List of SignatureConfirmation instances.  May be empty but never null.
     */
    List<SignatureConfirmation> getSignatureConfirmationValues();

    String getLastKeyEncryptionAlgorithm();

    /**
     * Provides hinting about whether any WSS 1.1 features were seen while processing this message.
     * Currently the only WSS 1.1 feature that will cause this to be true is an EncryptedHeader.
     * <p/>
     * Note that a request can fail to use any obvious WSS 1.1 features, but the requester could still
     * be expecting a WSS 1.1 reply (with SignatureConfirmation etc).  Thus this hint should not be
     * considered absolute.
     *
     * @return true if this message used any WSS 1.1 features.
     */
    boolean isWsse11Seen();

    /**
     * Check if any DerivedKey elements were seen while processing this message.
     * <p/>
     * This can be used as a heuristic to decide whether to prefer to use derived keys in a response.
     *
     * @return true if any derived keys were seen while processing this request.
     */
    boolean isDerivedKeySeen(); // Use this to tell if it is safe to use derived keys in a response.
}
