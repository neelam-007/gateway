/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.decorator;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.message.Message;

import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.io.IOException;

import org.xml.sax.SAXException;

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
    }
}
