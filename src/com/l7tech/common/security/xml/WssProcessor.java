/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Consumes and removes the default Security header in a message, removes any associated decorations, and returns a
 * complete record of its activities.
 *
 * @author mike
 */
public interface WssProcessor {
    public interface ParsedElement {
        Element asElement();
        String asXmlString();
    }

    public interface SecurityToken extends ParsedElement {
        Object asObject();
    }

    public interface UsernameToken extends SecurityToken {
        String getUsername();
    }

    public interface X509SecurityToken extends SecurityToken {
        X509Certificate asX509Certificate();
    }

    public interface TimestampDate extends ParsedElement {
        Date asDate();
    }

    public interface Timestamp extends ParsedElement {
        TimestampDate getCreated();
        TimestampDate getExpires();
    }

    public interface ProcessorResult {
        Document getUndecoratedMessage();
        Element[] getElementsThatWereSigned();
        Element[] getElementsThatWereEncrypted();
        SecurityToken[] getSecurityTokens();
        Timestamp getTimestamp();
    }

    public static class ProcessorException extends Exception {}

    ProcessorResult undecorateMessage(Document message) throws ProcessorException;
}
