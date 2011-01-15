/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.xml.decorator;

import com.l7tech.message.Message;
import com.l7tech.util.InvalidDocumentFormatException;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Set;

/**
 * Creates a Security header and decorates a message according to instructions passed in.
 *
 * @author mike
 */
public interface WssDecorator {

    /**
     * Decorate a soap message with WSS style security.
     * @param message the soap message to decorate
     * @param decorationRequirements details of what needs to be processed
     */
    DecorationResult decorateMessage(Message message, DecorationRequirements decorationRequirements)
            throws InvalidDocumentFormatException, GeneralSecurityException, DecoratorException, SAXException, IOException;

    interface DecorationResult {
        /**
         * If an EncryptedKey was generated, this returns the EncryptedKeySHA1 reference that can be used to
         * reference this particular EncryptedKey in a future signature or encryption KeyInfo.
         *
         * @return the EncryptedKeySHA1 that can be used to refer to an included EncryptedKey, or null
         *         if no EncryptedKey was generated.
         */
        String getEncryptedKeySha1();

        /**
         * If an EncryptedKey was generated, this returns the SecretKey that it contains.
         *
         * @return the SecretKey encoded into an included EncryptedKey, or null if no EncryptedKey was generated.
         */
        SecretKey getEncryptedKeySecretKey();

        /**
         * If a WS-SecureConversation session was used to decorate the message, this records the WSSC session identifier that was used.
         *
         * @return the WSSC session identifier string, or null.
         */
        String getWsscSecurityContextId();

        /**
         * Retrieves the signature values added by the decorator and their encryption status in the decorated message.
         *
         * @return A map with signature values as key; a true value for a signature means the signature value was encrypted in the decorated message.
         */
        Map<String,Boolean> getSignatures();

        /**
         * Gets the set of (unencrypted) signature values that became encrypted as a result of the decoration.
         */
        Set<String> getEncryptedSignatureValues();

        /**
         * @return the security header actor to which the decoration was applied, null for the no actor header
         */
        String getSecurityHeaderActor();

        /**
         * Sets / changes the actor associated with the decoration results. Useful for when, e.g. the security header's actor is changed. 
         */
        void setSecurityHeaderActor(String newActor);
    }
}
