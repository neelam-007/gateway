/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.processor;

import org.w3c.dom.Document;

/**
 * @author mike
 */
public interface ProcessorResult {
    Document getUndecoratedMessage();
    SignedElement[] getElementsThatWereSigned();
    ParsedElement[] getElementsThatWereEncrypted();
    SecurityToken[] getSecurityTokens();
    WssTimestamp getTimestamp();
    String getSecurityNS();
    String getWSUNS();
}
