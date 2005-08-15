/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.token.EncryptedElement;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.token.SignedElement;
import com.l7tech.common.security.token.SigningSecurityToken;
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
    String getLastSignatureValue(); // TODO replace this with a mechanism that can cope with multiple signature values
    String getLastSignatureConfirmation(); // TODO replace this with a mechanism that can cope with multiple SignatureConfirmation headers
    boolean isWsse11Seen(); // TODO Remove this hack.  It is for detecting the WSS interop scenario request July 2005
}
