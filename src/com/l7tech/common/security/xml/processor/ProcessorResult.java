/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;



/**
 * Holds the result of calling WssProcessor.
 */
public interface ProcessorResult {
    SignedElement[] getElementsThatWereSigned();
    ParsedElement[] getElementsThatWereEncrypted();
    SecurityToken[] getSecurityTokens();
    WssTimestamp getTimestamp();
    String getSecurityNS();
    String getWSUNS();
}
