/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.token.*;
import com.l7tech.common.security.xml.SecurityActor;
import org.w3c.dom.Element;


/**
 * Holds the result of calling WssProcessor.
 */
public interface ProcessorResult {
    SignedElement[] getElementsThatWereSigned();
    EncryptedElement[] getElementsThatWereEncrypted();

    /**
     * @param element the element to find the signing tokens for
     * @return the array if tokens that signed the element or empty array if none
     */
    SigningSecurityToken[] getSigningTokens(Element element);

    SecurityToken[] getSecurityTokens();
    WssTimestamp getTimestamp();
    String getSecurityNS();
    String getWSUNS();
    SecurityActor getProcessedActor();
}
