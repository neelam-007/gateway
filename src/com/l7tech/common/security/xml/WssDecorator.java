/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Creates a Security header and decorates a message according to instructions passed in.
 *
 * @author mike
 */
public interface WssDecorator {
    Document decorateMessage(Document message, Element[] elementsToEncrypt, Element[] elementsToSign);
}
