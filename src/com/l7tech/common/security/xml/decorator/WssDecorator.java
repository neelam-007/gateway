/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.decorator;

import com.l7tech.common.xml.InvalidDocumentFormatException;
import org.w3c.dom.Document;

import java.security.GeneralSecurityException;

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
    void decorateMessage(Document message, DecorationRequirements decorationRequirements)
            throws InvalidDocumentFormatException, GeneralSecurityException, DecoratorException;
}
