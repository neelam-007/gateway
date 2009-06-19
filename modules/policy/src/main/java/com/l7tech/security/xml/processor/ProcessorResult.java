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

    /**
     * Get the actor/role "type" of the security header that was processed.
     *
     * @return The actor or null if no security header was found.
     */
    SecurityActor getProcessedActor();

    /**
     * Get the URI for the actor/role of security header that was processed.
     *
     * @return The actor URI or null if no actor was specified.
     */
    String getProcessedActorUri();

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
     * Get the SignatureConfirmation WSS Processor result. Validation is enforced in either strict or
     * non-strict mode, and the outcome and errors are recorded in this result.
     *
     * <b>IMPORTANT: The WSS Processor ensures that signature confirmations are signed by <i>something</i>,
     * however the caller is responsible for validating the that the signing identities are trusted and
     * authoritative for signing the signature confirmations.
     *
     * @return a List of SignatureConfirmation instances.  May be empty but never null.
     * @see com.l7tech.security.token.SignatureConfirmation
     * @see strict // todo
     */
    SignatureConfirmation getSignatureConfirmation();

    String getLastKeyEncryptionAlgorithm();

    /**
     * Provides hinting about whether any WSS 1.1 features were seen while processing this message,
     * such as EncryptedHeader or SignatureConfirmation.
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
